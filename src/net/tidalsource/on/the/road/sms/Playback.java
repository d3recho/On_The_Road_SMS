package net.tidalsource.on.the.road.sms;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;


public class Playback extends Activity implements OnInitListener {
	private final String TAG_CLASS = "Playback"; 
	private boolean startedByReceiver, userIntervention, waitForAudioOnInit;
	private int appMode;
	protected boolean isTTSActive;
	private Button btnPlay, btnMode, btnReplyMenu;
	private PlaybackButton btnStop;
	private SeekBar volumeBar;
	private NotificationManager notifMgr;
	private Bundle bundle;
	private Resources res;
	private App app;
	
	Handler handler = new Handler();	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate()");
		
		app = (App)getApplicationContext();
		
		setContentView(R.layout.playback);
			
		notifMgr = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
		appMode = app.myPrefs.getVal(CONST.APP_MODE);

	    btnMode = (Button) findViewById(R.id.btnMode);
		btnPlay = (Button) findViewById(R.id.btnPlay);
		btnStop = (PlaybackButton) findViewById(R.id.btnStop);
		btnReplyMenu = (Button) findViewById(R.id.btnReplyMenu);
		volumeBar = (SeekBar) findViewById(R.id.volumeBar);
		
		btnMode.setOnTouchListener(new MyHapticFeedback());
		btnPlay.setOnTouchListener(new MyHapticFeedback());
		btnStop.setOnTouchListener(new MyHapticFeedback());
		btnReplyMenu.setOnTouchListener(new MyHapticFeedback());
		volumeBar.setOnTouchListener(new MyHapticFeedback());
		
		startedByReceiver = false;
		isTTSActive = false;
		setModeButtonText(appMode);
		updateNotification(appMode);
	
		// Fire off an intent to check if a TTS engine is installed
		Intent audioIntent = new Intent();
		audioIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		audioIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		startActivityForResult(audioIntent, CONST.AUDIO_CHECK_CODE);
		
		setVolumeControlStream(app.myAudio.MY_STREAM);
		volumeBar.setMax(app.myAudio.audioMgr.getStreamMaxVolume(app.myAudio.MY_STREAM));
		volumeBar.setProgress((Integer) app.myAudio.myAppVol);
		
		volumeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (app.myAudio.hasStreamVolBeenSaved)
					app.myAudio.audioMgr.setStreamVolume(app.myAudio.MY_STREAM, progress, 0);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				userIntervention = true;
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				app.myPrefs.putVal(CONST.APP_VOLUME, seekBar.getProgress());
			}
		});
		
		app.myPrefs.putVal(CONST.MESSAGE_MAYBE_SENT, false);	// Just to clear in case this has remained set	
		bundle = getIntent().getExtras();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate(): bundle = " + (bundle != null ? true : false));

		if (bundle != null) 
			startedByIntent(bundle, true);		// If started by receiver then latest message should be received by intent extras
		else {
			updateButtonDrawable(R.drawable.btnblank, btnPlay, 3);
			new asyncDbLookupPreSpeak().execute(2000);	// If app started manually then do a db lookup to get latest message
		}
	}

	public void onResume() {
		super.onResume();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onResume");		

		app.myPrefs.putVal(CONST.IN_PAUSED_MODE, false);

		if (app.myTTS != null) {
			app.myAudio.setTTSLanguage();
		}

		//  If app is opened manually (resumed though) still make sure new message is advertised to user 
		if (app.myPrefs.getVal(CONST.MISSED_MESSAGE)) {
			appStatus(true, true, false, res.getString(R.string.PLAYBACK_NEW_MESSAGE));
		}
	}
	
	public boolean poppedUpAboutPage() {
		// Should About page be showed section
		boolean show_about_page = false;
		
		// Determine if app is just intalled then show About page
		if (app.myPrefs.getVal(CONST.IS_FIRST_RUN)) {
			app.myPrefs.putVal(CONST.IS_FIRST_RUN, false);
			show_about_page = true;
		}
		
		// Determine if newer version has been installed
		int app_version = 0;
		try {
			app_version = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		if (app_version > (Integer) app.myPrefs.getVal(CONST.APP_VERSION)) {
			app.myPrefs.putVal(CONST.APP_VERSION, app_version);
			show_about_page = true;			
		}
		
		// If either of the above is true then show About page		
		if (show_about_page) {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": poppedUpAboutPage(): True");
			Intent j = new Intent(this, About.class);
			startActivity(j);			
			return true;			
		}
		
		return false;
	}
	
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);		
		bundle = intent.getExtras();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onNewIntent(): bundle = " + (bundle != null ? true : false));		
		if (bundle != null)
			startedByIntent(bundle, false);
	}
		
	public void onPause() {
		super.onPause();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onPause");		
		app.myAudio.utteranceComplete();
		app.myAudio.restoreStreamVol();
	}

	@Override
	public void onDestroy() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onDestroy");		
		if (app.myTTS != null)
		app.myTTS.shutdown();
		super.onDestroy();
	}
		
	// If Bundle is not empty (SMS receiver triggers this Playback.class by Intent)
	public void startedByIntent(Bundle bundle, boolean _waitForAudioOnInit) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": startedByIntent()");
		
		waitForAudioOnInit = _waitForAudioOnInit;
		startedByReceiver = true;
		userIntervention = false;

		// Get through keylock and when screen is off
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
			    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		// Get message values from IntentExtra instead of queryMsgDb()
		app.myMessage.setMessageFromBundle(bundle);
		bundle.clear();

		// If Auto play enabled
		if ((Integer) app.myPrefs.getVal(CONST.APP_MODE) == CONST.MODE_AUTO_READ_MSG) {
			if (waitForAudioOnInit == false)					
				preSpeakMessage(1500);
		}
		// If Auto play disabled, but Bring to Front enabled
		else if ((Integer) app.myPrefs.getVal(CONST.APP_MODE) == CONST.MODE_BRING_TO_FRONT) {
			// If keylock is active, only keep app in front only for some time (ineffective if keylock not active)
			handler.postDelayed(new delayedClosure(), CONST.DELAYED_CLOUSURE_2);
			// Obviously a message was received, set action button text etc.
			appStatus(true, true, false, res.getString(R.string.PLAYBACK_NEW_MESSAGE));
			
		}
		
		if (app.myPrefs.getVal(CONST.MISSED_MESSAGE)) {
			app.myPrefs.putVal(CONST.MISSED_MESSAGE, false);
		}
	}

	
	
	@SuppressWarnings("deprecation")
	public void updateNotification(int mode) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": updateNotification(): mode = " + mode);
		
		String contentTitle = "";
		String contentText = "";
		
		if (mode == 0) {
				notifMgr.cancel(CONST.NOTIFICATION_ID);
		}
		else {
			Notification notif = new Notification();
			notif.tickerText = CONST.TAG;
			notif.when = System.currentTimeMillis();
			notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
			switch (mode) {
			case 1 :
				notif.icon = R.drawable.statusplay;
				contentTitle = "" + CONST.TAG + res.getString(R.string.MODE_IS_ACTIVE);
				contentText = res.getString(R.string.MODE_DESCRIPTION_1);
				break;
			case 2 : 
				notif.icon = R.drawable.statusfront;
				contentTitle = "" + CONST.TAG + res.getString(R.string.MODE_IS_ACTIVE);
				contentText = res.getString(R.string.MODE_DESCRIPTION_2);
				break;
			}
			Intent playbackIntent = new Intent(this, Playback.class);
			playbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent contentIntent = PendingIntent.getActivity(this,  0, playbackIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
			notif.setLatestEventInfo(app, contentTitle, contentText, contentIntent);			
			notifMgr.notify(CONST.NOTIFICATION_ID, notif);			
		}		
	}	
	
	public void modeButtonClicked(View v) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": modeButtonClicked(): appMode = " + appMode);
		// If user has made a click, used to stop countdown for auto closure of windows 
		userIntervention = true;
		
		switch (appMode) {
		case 0 : 
			app.myPrefs.putVal(CONST.APP_MODE, CONST.MODE_AUTO_READ_MSG); 
			break;
		case 1 : 
			app.myPrefs.putVal(CONST.APP_MODE, CONST.MODE_BRING_TO_FRONT); 
			break;
		case 2 : 
			app.myPrefs.putVal(CONST.APP_MODE, CONST.MODE_DISABLED); 
			break;
		}
		
		appMode = app.myPrefs.getVal(CONST.APP_MODE);
		setModeButtonText(appMode);
		updateNotification(appMode);
	}	

	public void setModeButtonText(int mode) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": setModeButtonText(): appMode = " + mode);
		
		switch (mode) {
		case 0 : 
			btnMode.setText(res.getString(R.string.APP_MODE_TEXT_0));
			btnMode.setShadowLayer(2.0f, 1.5f, 1.5f, Color.RED);
			updateButtonDrawable(R.drawable.modedisabled, btnMode, 0);
			break;
		case 1 : 
			btnMode.setText(res.getString(R.string.APP_MODE_TEXT_1));	
			btnMode.setShadowLayer(2.0f, 1.5f, 1.5f, Color.GREEN);
			updateButtonDrawable(R.drawable.modeplay, btnMode, 0);
			break;
		case 2 : 
			btnMode.setText(res.getString(R.string.APP_MODE_TEXT_2));
			btnMode.setShadowLayer(2.0f, 1.5f, 1.5f, Color.YELLOW);
			updateButtonDrawable(R.drawable.modemute, btnMode, 0);
			break;
		}
	}	
	
	public void updateButtonDrawable(int myDrawable, Button button, int location) {
		for(Drawable oldDrawable : button.getCompoundDrawables())
			if (oldDrawable != null)
				oldDrawable.setCallback(null);
		Drawable newDrawable = this.getResources().getDrawable(myDrawable);
		
		switch (location) {
		case 0 :	// Left
			button.setCompoundDrawablesWithIntrinsicBounds(newDrawable, null, null, null); break;
		case 1 :	// Top
			button.setCompoundDrawablesWithIntrinsicBounds(null, newDrawable, null, null); break;
		case 2 :	// Right
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, newDrawable, null); break;
		case 3 :	// Bottom
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, newDrawable); break;
		case -1 :	// None
			button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null); break;
		}
	}
	
	public void mainButtonClicked(View v) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": mainButtonClicked()");
		
		userIntervention = true;	// If user has made a click, used to stop countdown for auto closure of windows
		
		if (v.getId() == R.id.btnPlay) {
			appStatus(false, true, false, "");
			app.myAudio.initPlayback(100);	
		}
		else if (v.getId() == R.id.btnStop) {
			app.myAudio.utteranceComplete();
			// Want to prevent fast clicking on the button
			appStatus(true, false, true, res.getString(R.string.PLAYBACK_STOPPED));			
			handler.postDelayed(new postPlayRunnable(), CONST.THREAD_SLEEP_MILLIS);
		}		
	}
	
	public void appStatus(boolean isStopped, boolean isClickable, boolean isReplyVisible, String loadMessage) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": appStatus()" + isStopped + ", " + isClickable + ", " + isReplyVisible + ", " + loadMessage );
		
		if (isStopped) {
//			app.myAudio.utteranceComplete();
			btnPlay.setVisibility(View.VISIBLE);
			btnStop.setVisibility(View.GONE);
			btnMode.setVisibility(View.VISIBLE);
			volumeBar.setVisibility(View.GONE);
			
			btnPlay.setClickable(isClickable);
			btnPlay.setText(loadMessage);
			
		}
		else {
			btnPlay.setVisibility(View.GONE);
			btnStop.setVisibility(View.VISIBLE);
			btnMode.setVisibility(View.GONE);
			volumeBar.setVisibility(View.VISIBLE);
			
			btnStop.setClickable(isClickable);
			btnStop.textTitle(app.myMessage.playbackMsg.get("name"));
			btnStop.textBody(app.myMessage.playbackMsg.get("body"));
		}
		
		if (isReplyVisible)
			btnReplyMenu.setVisibility(View.VISIBLE);
		else
			btnReplyMenu.setVisibility(View.GONE);
	}
	
	public void replyMenuButtonClicked(View v) {
		Intent j = new Intent(Playback.this, Replying.class);
		j.putExtra("address", app.myMessage.playbackMsg.get("address"));
		startActivity(j);		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreateOptionsMenu()");
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onOptionsItemSelected()");
		
		switch (item.getItemId()) {
		case R.id.menu_preferences:
			Intent i = new Intent(this, Preferences.class);
			startActivity(i);
			return true;
		case R.id.menu_about:
			Intent j = new Intent(this, About.class);
			startActivity(j);
			return true;			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
		
	public class delayedClosure implements Runnable {
		public void run() {
			// Only if screen was locked will the following command matter, which closes the window 
			if (userIntervention == false) {
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": delayedClosure(): userIntervention = " + userIntervention + ", appMode = " + appMode);				
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
			}
		}
	}
	
	public class postPlayRunnable implements Runnable {
		public void run() {			
			app.myAudio.restoreStreamVol();
			appStatus(true, true, true, res.getString(R.string.PLAYBACK_STOPPED));
		}
	}
	
	// Make sure TTS is installed, create TextToSpeech here
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onActivityResult(): requestCode = " + requestCode);
		
		 if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				switch (requestCode) {
				case CONST.AUDIO_CHECK_CODE :
					if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) 
						app.myTTS = new TextToSpeech(this, this);	// success, create the TTS instance
					 else {
							 askUserToInstallTTS();						// missing data, ask user to go and install it
					 }
					break;
				}			 
		 }
		 else {
				app.myTTS = new TextToSpeech(this, this);	// success, create the TTS instance			 
		 }
		
	}
	
	// missing data, ask user to go and install it
	public void askUserToInstallTTS() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(res.getString(R.string.INSTALL_TTS_DIALOG_TITLE))
		.setMessage(res.getString(R.string.INSTALL_TTS_DIALOG_MESSAGE))
		.setPositiveButton(res.getString(R.string.BUTTON_YES), new DialogInterface.OnClickListener() {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
				finish();
			}
		})
		.setNegativeButton(res.getString(R.string.BUTTON_CANCEL), new DialogInterface.OnClickListener() {					
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		})
		.show();		
	}
	
	
	// Runs this once initialised TTS service
	@SuppressWarnings("deprecation")
	@Override
	public void onInit(int status) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onInit(): status = " + status);
		
		app.myTTS.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {			
			@Override
			public void onUtteranceCompleted(String utteranceId) {
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onInit(): onUtteranceComplete: " + utteranceId);

				app.myAudio.utteranceComplete();
				handler.postDelayed(new postPlayRunnable(), CONST.THREAD_SLEEP_MILLIS);	// Reset action button text, show reply 
				handler.postDelayed(new delayedClosure(), CONST.DELAYED_CLOUSURE_1);	// Close app if lock screen was enabled
			}
		});
		
		if (status == TextToSpeech.SUCCESS) {
			app.myAudio.successTTSInit("playback");
			app.myAudio.setTTSLanguage();
			
			// waitForAudioOnInit can only be set when started by receiver
			// So basically if a receiver started this activity, details about a new message is available through intent extra
			if (waitForAudioOnInit) {
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onInit(): waitForAudioOnInit");
				preSpeakMessage(1500);
				waitForAudioOnInit = false;
			}
			
			// If app started manually then do this 
			if (startedByReceiver == false) {
				isTTSActive = true;
			}
		}
	}
	
	public void preSpeakMessage(final int delay) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": preSpeakMessage()");

		// Only playback last incoming message if it is flagged as unread and it has not been played back before 
		if (!app.myMessage.playbackMsg.isEmpty() && app.myMessage.playbackMsg.get("read").equals("0") &&  
				!app.myPrefs.getVal(CONST.LAST_PLAYED_MSG_ID).equals(app.myMessage.playbackMsg.get("id"))) {

			appStatus(false, true, false, "");
			
			// Put following in a Runnable so UI refreshes properly
			handler.post(new Runnable() {
				@Override
				public void run() {
					app.myAudio.speakMessage(delay);					
				}				
			});
			
		}
		else
			appStatus(true, true, false, res.getString(R.string.PLAYBACK_STOPPED));
	}
	
	private class asyncDbLookupPreSpeak extends AsyncTask<Integer, Void, Integer> {
		@Override
		protected void onPreExecute() {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": asyncDbLookupPrePlayback()");			
			appStatus(true, false, false, res.getString(R.string.PLAYBACK_LOADING));
		}
		
		@Override
		protected Integer doInBackground(Integer... params) {
			app.myMessage.getMessageFromDb(1);
			int count = 0;
			// Loop up to 10 times with 150ms sleep to wait for TTS to be initialised
			do {
				if (isTTSActive)
					break;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				count++;
			} while (count < 10);
			
			if (count >= 10)
				if (BuildConfig.DEBUG) Log.e(CONST.TAG, TAG_CLASS + ":asyncDbLookupPreSpeak(): doInBackground(): FAILED to get TTS active flag");			
			return params[0];
		}
		protected void onPostExecute(Integer delay) {
			updateButtonDrawable(R.drawable.btnplay, btnPlay, 3);
			appStatus(true, true, false, res.getString(R.string.PLAYBACK_STOPPED));
			if (poppedUpAboutPage())
				return;
			preSpeakMessage(delay);
		}
	}	
}
 