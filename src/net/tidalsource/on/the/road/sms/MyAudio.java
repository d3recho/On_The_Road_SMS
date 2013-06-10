package net.tidalsource.on.the.road.sms;

import java.util.HashMap;
import java.util.Locale;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

public class MyAudio {
	private final String TAG_CLASS = "MyAudio";
	public final int MY_STREAM = AudioManager.STREAM_MUSIC;
	public AudioManager audioMgr;
	public boolean hasStreamVolBeenSaved;
	private Context context;
	private HashMap<String, String> paramsSpeak = new HashMap<String, String>();
	protected AsyncTask<Integer, Void, Integer> myAsyncTask;
	public int mediaVol, myAppVol;
	private Resources res;
	private App app;
	
	public MyAudio(Context c) {
		context = c;
		res = context.getResources();
		app = (App)c.getApplicationContext();
		audioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mediaVol = audioMgr.getStreamVolume(MY_STREAM);
		myAppVol = app.myPrefs.getVal(CONST.APP_VOLUME);
	}

	public void initPlayback(int delay) {
		if (app.myPrefs.getVal(CONST.MISSED_MESSAGE)) {
			app.myPrefs.putVal(CONST.MISSED_MESSAGE, false);
			myAsyncTask = new asyncDbLookupDoSpeak().execute(10);
		}
		else if (!app.myMessage.playbackMsg.isEmpty()) {
			speakMessage(paramsSpeak, delay, "");
		}
		else 
			Log.e(CONST.TAG, TAG_CLASS + ": initPlayback(): Playback message not initialized");
	}
	
	public void saveStreamVol(int volume) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": saveStreamVol()");
		
		hasStreamVolBeenSaved = true;
		app.myPrefs.putVal(CONST.RESTORE_TO_VOL, volume);
	}

	public void restoreStreamVol() {
		if (hasStreamVolBeenSaved) {
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": restoreStreamVol()");
			hasStreamVolBeenSaved = false;
			mediaVol = app.myPrefs.getVal(CONST.RESTORE_TO_VOL);
			audioMgr.setStreamVolume(MY_STREAM, mediaVol, 0);
		}
	}
	
	public void utteranceComplete() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": utteranceComplete()");
		
		if (app.myTTS != null)
			app.myTTS.stop();
		if (myAsyncTask != null)
			myAsyncTask.cancel(true);
		audioMgr.abandonAudioFocus(audioFocus);
	}
	
	public void successTTSInit(String id) {
		paramsSpeak.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,	id);  // Needed for the completed listener to work
		paramsSpeak.put(TextToSpeech.Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS, "true");
	}

	public void setTTSLanguage() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": setTTSLanguage(): myTTS = " + (app.myTTS != null ? "active" : "null"));

		String langPref = app.myPrefs.getVal(CONST.PLAYBACK_LANG);
		for (Locale loc : Locale.getAvailableLocales())
			if (loc.getDisplayName().equals(langPref))
				app.myTTS.setLanguage(loc);
	}	

	private AudioManager.OnAudioFocusChangeListener audioFocus = new AudioManager.OnAudioFocusChangeListener() {
		
		@Override
		public void onAudioFocusChange(int focusChange) {
			// Not really caring about this for now
		}
	};
	

	public void speakMessage(int delay) {
		this.speakMessage(paramsSpeak, delay, "");
	}
	
    public void speakQuickMessage(String speakThis) {
		speakMessage(paramsSpeak, 0, speakThis);
    }

    public void speakMessage(HashMap<String, String> params, int delay, String quickMessage) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": speakMessage(): delay = " + delay);

		// Last check to make sure TTS has loaded, if not then abort
		if (app.myTTS == null) {
			Toast.makeText(context, res.getString(R.string.TTS_NOT_FOUND), Toast.LENGTH_SHORT).show();
			return;
		}
		
		app.myTTS.stop();
		String msgComplete = ""; 
					
		if (quickMessage.equals("")) {
			if (app.myPrefs.getVal(CONST.MISSED_MESSAGE))
				app.myPrefs.putVal(CONST.MISSED_MESSAGE, true);
			// Save id of message so it does not auto play next time app is opened
			app.myPrefs.putVal(CONST.LAST_PLAYED_MSG_ID, app.myMessage.playbackMsg.get("id"));
			
			String msgIntroduce = res.getString(R.string.TTS_INTRO_A_SPOKEN) + app.myMessage.playbackMsg.get("name") + ", . " + res.getString(R.string.TTS_INTRO_B_SPOKEN);
			String msgBody = res.getString(R.string.TTS_BODY_A_SPOKEN) + app.myMessage.playbackMsg.get("body") + ". " + res.getString(R.string.TTS_BODY_B_SPOKEN);
			msgComplete = msgIntroduce + msgBody;			
		}
		else {
			msgComplete = quickMessage;
		}		

		// Get App volume and call function to store real media volume to tmp variable
		mediaVol = audioMgr.getStreamVolume(MY_STREAM);
		myAppVol = app.myPrefs.getVal(CONST.APP_VOLUME);
		saveStreamVol(mediaVol);
		audioMgr.setStreamVolume(MY_STREAM, myAppVol, 0);
		
		// Take audio focus, allow ducking volume for any other app
		int resFocus = audioMgr.requestAudioFocus(audioFocus, MY_STREAM, 
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
		if (resFocus == AudioManager.AUDIOFOCUS_REQUEST_FAILED)
			if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": Audio focus request failed...");
		
		app.myTTS.playSilence(delay, TextToSpeech.QUEUE_FLUSH, null);
		app.myTTS.speak(msgComplete + ". ", TextToSpeech.QUEUE_ADD, params);		
	}	
	
	private class asyncDbLookupDoSpeak extends AsyncTask<Integer, Void, Integer> {
		protected void onPreExecute() {
			if (BuildConfig.DEBUG) Log.w(CONST.TAG, TAG_CLASS + ": asyncDbLookupDoSpeak()");
		}
		@Override
		protected Integer doInBackground(Integer... params) {
			app.myMessage.getMessageFromDb(1);
			return params[0];
		}
		protected void onPostExecute(Integer params) {
			speakMessage(paramsSpeak, params, "");
		}
	}

}


