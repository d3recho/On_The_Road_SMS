package net.tidalsource.on.the.road.sms;

import android.app.Application;
import android.speech.tts.TextToSpeech;

public class App extends Application {
	
	protected TextToSpeech myTTS;
	protected MyMessage myMessage;
	protected MyPrefs myPrefs;
	protected MyAudio myAudio;

    
	public void onCreate() {
		super.onCreate();
		myMessage = new MyMessage(this);
		myPrefs = new MyPrefs(this);
		myAudio = new MyAudio(this);

	}
  
    
}
