package net.tidalsource.on.the.road.sms;

public class CONST {
	// Application
	protected static final String SETTINGS = "settings";
	protected static final String TAG = "On The Road SMS";
	protected static final int IS_FIRST_RUN = 114;
	protected static final int APP_VERSION = 115;
	
	// Preference variables
	protected static final String PREF_NAME = "thispreference";
	protected static final int PLAYBACK_LANG = 100;
	protected static final int APP_VOLUME = 101;			// Unaffected by Voice Volume, this is how loud speech will be read
	protected static final int REPLY_MSG_1 = 102;
	protected static final int REPLY_MSG_2 = 103;
	protected static final int CLEAR_REPEAT = 107;			// When a message is repeated, it will play back slower and louder
	protected static final int AFTER_HOURS_A = 110;
	protected static final int AFTER_HOURS_B = 111;
	protected static final int AFTER_HOURS_ENABLED = 112;
	protected static final int RECORDING_LANGUAGE = 113;
	
	// Behaviour when message is received
	protected static final int APP_MODE = 200;
	protected static final int MODE_DISABLED = 0;
	protected static final int MODE_AUTO_READ_MSG = 1;
	protected static final int MODE_BRING_TO_FRONT = 2;
	
	// Playback variables 
	protected static final int MISSED_MESSAGE = 104;		// If receiver gets a message but is not allowed to start activity, set this to true
	protected static final int IN_PAUSED_MODE = 105;		// Stops receiver to bring activity forth. We don't want to interrupt a reply recording.
	protected static final int MESSAGE_MAYBE_SENT = 106;	// If a reply is sent then set this flag so parent activities know.. could use intent result.... 
	protected static final int RESTORE_TO_VOL = 300;
	protected static final int LAST_PLAYED_MSG_ID = 301;
	protected static final int LAST_MSG_ID_BEFORE_REPLY = 302;	// Store last sent sms msg id so we can check that a reply was actually sent (in lack of better way to finding out)
	protected static final int AVAILABLE_REC_LANGUAGES = 303;
	protected static final int AVAILABLE_REC_LANG_IDS = 304;
	
	// Variables
	protected static final int LOOKUP_MAX_LOOP = 10;
	protected static final int THREAD_SLEEP_MILLIS = 300;
	protected static final int DELAYED_CLOUSURE_1 = 5000;
	protected static final int DELAYED_CLOUSURE_2 = 8000;
	
	// Id's and check codes
	protected static final int AUDIO_CHECK_CODE = 1001;
	protected static final int SETTINGS_CHECK_CODE = 1002;
	protected static final int NOTIFICATION_ID = 72344;

	
}
