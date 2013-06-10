package net.tidalsource.on.the.road.sms;

import java.util.Calendar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
	private static final String TAG_CLASS = "SmsReceiver"; 
	private static MyPrefs myPrefs;
	private static MyMessage myMessage;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onReceive()");
		
		if (testAppMode(context) && testPhoneState(context) && testRingerMode(context) && testAfterHoursEnabled()) {
			Bundle bundle = intent.getExtras();
			if (bundle != null) {

				myMessage = new MyMessage(context);
				int lastMsgId = myMessage.getMessageFromDb(1);
				
				// Get message from smsreceiver
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] msg = new SmsMessage[1];
				msg[0] = SmsMessage.createFromPdu((byte[]) pdus[0]);
				
//				String xx1 = myMessage.playbackMsg.get("body");
//				String xx2 = msg[0].getMessageBody();
					
				if (myMessage.playbackMsg.get("body").equals(msg[0].getMessageBody()) || myMessage.delayWhileMsgArrives(lastMsgId, 1)) {
					// create intent to bring playback app to front
					Intent playbackIntent = new Intent(context, Playback.class);
					playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					playbackIntent.putExtra("id", myMessage.playbackMsg.get("id"));
					playbackIntent.putExtra("body", myMessage.playbackMsg.get("body"));
					playbackIntent.putExtra("address", myMessage.playbackMsg.get("address"));
					playbackIntent.putExtra("name", myMessage.playbackMsg.get("name"));

					if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": Launching activity Playback");
					context.startActivity(playbackIntent);
				}
			}			
		}
		else {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onReceive: Halted due to phone state, ringer mode, app behaviour or after hours");
			myPrefs.putVal(CONST.MISSED_MESSAGE, true);
			return;
		}
	}

	// App behaviour mode must be either auto read or bring to front
	private boolean testAppMode(Context context) {
		myPrefs = new MyPrefs(context);
		return (((Integer) myPrefs.getVal(CONST.APP_MODE) == CONST.MODE_AUTO_READ_MSG) || 
				((Integer) myPrefs.getVal(CONST.APP_MODE) == CONST.MODE_BRING_TO_FRONT)) && 
				((Boolean) myPrefs.getVal(CONST.IN_PAUSED_MODE) == false);
	}

	// Do not proceed unless phone is idle
	private boolean testPhoneState(Context context) {
		TelephonyManager teleMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return teleMgr.getCallState() == TelephonyManager.CALL_STATE_IDLE;
	}

	// Do not continue if in silent or vibrate mode
	private boolean testRingerMode(Context context) {
		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		return am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL; 
	}
	
	// After hours check
	private boolean testAfterHoursEnabled() {
		if (myPrefs.getVal(CONST.AFTER_HOURS_ENABLED)) {
			
			Calendar c = Calendar.getInstance();
			String strTimeA = myPrefs.getVal(CONST.AFTER_HOURS_A);
			String strTimeB = myPrefs.getVal(CONST.AFTER_HOURS_B);

			int hourNow = c.get(Calendar.HOUR_OF_DAY);
			int minuteNow = c.get(Calendar.MINUTE);
			
			int hourA = Integer.parseInt(strTimeA.substring(0, strTimeA.indexOf(":")));
			int minuteA = Integer.parseInt(strTimeA.substring(strTimeA.indexOf(":") + 1, strTimeA.length()));
			int hourB = Integer.parseInt(strTimeB.substring(0, strTimeB.indexOf(":")));
			int minuteB = Integer.parseInt(strTimeB.substring(strTimeB.indexOf(":") + 1, strTimeB.length()));
			
			int x = hourNow * 60 + minuteNow;
			int y = hourA * 60 + minuteA;
			int z = hourB * 60 + minuteB;
			
			if (y > z)
				return (x < y && x > z);
			else if (y < z)
				return (x < y && x > z);
			else
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": Arrgh, did my After Hour computation just blow up ;{ ");
			
		}
		return true;
	}
	
}
