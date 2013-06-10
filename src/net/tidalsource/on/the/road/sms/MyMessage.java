package net.tidalsource.on.the.road.sms;

import java.util.HashMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

public class MyMessage {	
	private final String TAG_CLASS = "MyMessage";
	private Context context;
	private ContentResolver cr;
	public HashMap<String, String> playbackMsg = new HashMap<String, String>();
	
	public MyMessage(Context c) {
		context = c;
		cr = context.getContentResolver();
	}
	
	public int getMessageFromDb(int type) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": getMessageFromDb()");
		
		Uri uriSMS = Uri.parse("content://sms");
		String[] projection = new String[] { "_id", "read", "type", "body", "address" };
		String selection = "type = " + type;
		String sortOrder = "_id DESC";
		
		Cursor cursor = cr.query(uriSMS, projection, selection, null, sortOrder);
		if (cursor == null || !cursor.moveToFirst())
			return -1;
		
/*		for (int i = 1; i < cursor.getCount(); i++) {
			Log.v(CONST.TAG, "_id:" + cursor.getString(0) + " read:" + cursor.getString(1) + 
					" type:" + cursor.getString(2) + " body:" + cursor.getString(3) + " address:" + cursor.getString(4));
			cursor.moveToNext();		}	*/
			
		int msgId = cursor.getInt(cursor.getColumnIndex("_id"));
		// only update playbackMsg if type is 1, that will say is an incoming message
		if (type == 1) {
			playbackMsg.clear();
			playbackMsg.put("id", String.valueOf(msgId));
			playbackMsg.put("read", cursor.getString(cursor.getColumnIndex("read")));
			playbackMsg.put("type", cursor.getString(cursor.getColumnIndex("type")));
			playbackMsg.put("body", cursor.getString(cursor.getColumnIndex("body")));
			playbackMsg.put("address", cursor.getString(cursor.getColumnIndex("address")));
			playbackMsg.put("name",	getContactName(playbackMsg.get("address")));
		}
		cursor.close();
		return msgId;
	}
	
	public String getContactName(String number) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		String name = "Unknown";

		Cursor contactLookup = cr.query(uri, new String[] {
				BaseColumns._ID, PhoneLookup.DISPLAY_NAME }, null, null, null);

		try {
			if (contactLookup != null && contactLookup.getCount() > 0) {
				contactLookup.moveToNext();
				name = contactLookup.getString(contactLookup
						.getColumnIndex(Data.DISPLAY_NAME));
			}
		} finally {
			if (contactLookup != null) {
				contactLookup.close();
			}
		}
		return name;
	}
	
	// Get sms message id and wait until it appears in the database for retrieval
	public boolean delayWhileMsgArrives(int previousMsgId, int type) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": delayWhileMsgArrives()");

		int msgId;
		int count = 0;
		do {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": delayWhileMsgArrives(): Wait for msg after #" + previousMsgId + 
					" to arrive, loop " + count + " of " + CONST.LOOKUP_MAX_LOOP);
			try {
				Thread.sleep(CONST.THREAD_SLEEP_MILLIS * 2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			msgId = getMessageFromDb(type);
			count++;				
		} while (previousMsgId == msgId && count < CONST.LOOKUP_MAX_LOOP);
		if (count == CONST.LOOKUP_MAX_LOOP && previousMsgId == msgId) {
			if (BuildConfig.DEBUG) Log.e(CONST.TAG, TAG_CLASS + ": delayWhileMsgArrives(): Reached end of looping, no message found...");
			return false;
		}
		else {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": delayWhileMsgArrives(): Message has successfully arrived");
			return true;
		}
	}	

	public void setMessageFromBundle(Bundle bundle) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": setMessageFromBundle()");
		playbackMsg.clear();
		playbackMsg.put("id", bundle.getString("id"));
		playbackMsg.put("read", "0");
		playbackMsg.put("body", bundle.getString("body"));
		playbackMsg.put("address", bundle.getString("address"));
		playbackMsg.put("name", bundle.getString("name"));		
	}	
}
