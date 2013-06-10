package net.tidalsource.on.the.road.sms;

import java.util.ArrayList;
import java.util.List;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class Voicereply extends Activity implements OnClickListener, OnItemClickListener {
	private final String TAG_CLASS = "Voicereply";
    private Context context;
	private Bundle bundle;
	private ListView mList;
	private Button btnRecord, btnCancel;
	private String address;
	private Boolean smsMaybeSent;
	private SpeechRecognizer sr;
	private ProgressDialog progressDialog;
	private ToneGenerator tone;
	private boolean didCancelDialog;
	private Resources res;
	private App app;
	
	Handler handler = new Handler();	
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (App)getApplicationContext();
		res = getResources();
		context = getApplicationContext();

		bundle = getIntent().getExtras();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate(): bundle = " + (bundle != null ? true : false));
		if (bundle == null) {
			finish();
		}
		
		setContentView(R.layout.voice_reply);
		btnRecord = (Button) findViewById(R.id.btnRecord);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		mList = (ListView) findViewById(R.id.listView1);
		
		btnRecord.setOnTouchListener(new MyHapticFeedback());
		btnCancel.setOnTouchListener(new MyHapticFeedback());
		mList.setOnTouchListener(new MyHapticFeedback());
		
		address = bundle.getString("address");
		app.myPrefs.putVal(CONST.IN_PAUSED_MODE, true);
		smsMaybeSent = false;
		
		sr = SpeechRecognizer.createSpeechRecognizer(this);       
		sr.setRecognitionListener(new listener()); 		
		tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);		
		
        // Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0) { 
			btnRecord.setOnClickListener(this);
			btnCancel.setOnClickListener(this);
			mList.setOnItemClickListener(this);
		}
		else {
			btnRecord.setEnabled(false);
			btnRecord.setText(res.getString(R.string.STT_NOT_FOUND));
		}

		startVoiceRecognitionActivity();
	}

	public void onResume() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onResume(): smsMaybeSent = " + smsMaybeSent);		
		super.onResume();
		btnRecord.setClickable(true);
	}

    private void startVoiceRecognitionActivity() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": startVoiceRecognitionActivity()");
		
		didCancelDialog = false;
		btnRecord.setClickable(false);
		
		progressDialog = new ProgressDialog(Voicereply.this);
		progressDialog.setCancelable(true);
		progressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				didCancelDialog = true;
		    	sr.stopListening();
	        	tone.startTone(ToneGenerator.TONE_PROP_NACK);
	    		btnRecord.setClickable(true);
	        	finish();
			}
		});	
		progressDialog.setTitle(res.getString(R.string.STT_SPEAK_TITLE) + " " + (String) app.myPrefs.getVal(CONST.RECORDING_LANGUAGE));
		progressDialog.setMessage(res.getString(R.string.STT_SPEAK_BUTTON));
		
		btnRecord.setVisibility(View.GONE);
    	mList.setVisibility(View.GONE);
		btnCancel.setVisibility(View.GONE);

        tone.startTone(ToneGenerator.TONE_DTMF_1, 300);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());        
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, (String) app.myPrefs.getVal(CONST.RECORDING_LANGUAGE));
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE , true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        sr.startListening(intent);
		
}		
        
	@Override
	public void onClick(View v) {
        if (v.getId() == R.id.btnRecord) {
            startVoiceRecognitionActivity();
        }
        else if (v.getId() == R.id.btnCancel) {
        	finish();
        }
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		String speechText= arg0.getItemAtPosition(arg2).toString();		
		voiceReplyMsgIntent(address, speechText);
	}
	
	public void voiceReplyMsgIntent(String address, String body) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": voiceReplyMsgIntent()");
        smsMaybeSent = true;
		Intent replyIntent = new Intent(Intent.ACTION_VIEW);
		replyIntent.setData(Uri.parse("sms:"));
		replyIntent.putExtra("address", address);
		replyIntent.putExtra("sms_body", body);
		replyIntent.putExtra("exit_on_sent", true);
		replyIntent.setData(Uri.parse("smsto:" + address));
		startActivity(replyIntent);
		app.myPrefs.putVal(CONST.LAST_MSG_ID_BEFORE_REPLY, app.myMessage.getMessageFromDb(2));

	}	
	
	class listener implements RecognitionListener {
		
        public void onReadyForSpeech(Bundle params)
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onReadyForSpeech");
    		progressDialog.show();
        	
        }
        public void onBeginningOfSpeech()
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
//        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
//        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onBufferReceived");
        }
        public void onEndOfSpeech()
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onEndofSpeech");
        	sr.stopListening();
        	progressDialog.dismiss();
    		btnRecord.setClickable(true);        	
        }
        public void onError(int error)
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG,  TAG_CLASS + ": error " +  error);
        	progressDialog.dismiss();
        	tone.startTone(ToneGenerator.TONE_PROP_NACK);
        	String errorMsg = "";
        	switch(error) {
	        case 1 : errorMsg = res.getString(R.string.STT_ERROR_1); break;
        	case 2 : errorMsg = res.getString(R.string.STT_ERROR_2); break;
        	case 3 : errorMsg = res.getString(R.string.STT_ERROR_3); break;
        	case 4 : errorMsg = res.getString(R.string.STT_ERROR_4); break;
        	case 5 : errorMsg = res.getString(R.string.STT_ERROR_5); break;
        	case 6 : errorMsg = res.getString(R.string.STT_ERROR_6); break;
        	case 7 : errorMsg = res.getString(R.string.STT_ERROR_7); break;
        	case 8 : errorMsg = res.getString(R.string.STT_ERROR_8); break;
        	case 9 : errorMsg = res.getString(R.string.STT_ERROR_9); break;
        	}
        	if (!didCancelDialog) {	// We get error 6 if we cancel the process manually, why we check this
        		app.myAudio.speakQuickMessage(errorMsg);
        		Toast.makeText(context, res.getString(R.string.STT_ERROR_TOAST) + error, Toast.LENGTH_SHORT).show();
        	}
        	finish();
        }
        public void onResults(Bundle results)                   
        {
        	progressDialog.dismiss();
        	btnRecord.setVisibility(View.VISIBLE);
    		btnCancel.setVisibility(View.VISIBLE);
    		
        	tone.startTone(ToneGenerator.TONE_PROP_ACK);
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onResults " + results);
        	ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onResults : size = " + data.size() + " " + data.get(0).toString());
        	
        	if (!data.isEmpty()) {
        		// Limit size to 3 as Jelly Bean does not honour the MAX_RESULTS 
        		for (int i = data.size(); i > 3 ; i--) {
        			data.remove(i - 1);
        		}
        		
        		mList.setAdapter(new ArrayAdapter<String>(Voicereply.this, R.layout.results, data));
        		mList.setVisibility(View.VISIBLE);            	
        		handler.postDelayed(new Runnable() {
        			// Quick workaround because I cant access mList directly after adapter has been set....
        			@Override
        			public void run() {
        				mList.getChildAt(0).setMinimumHeight(100);
        				mList.getChildAt(0).setPadding(20, 40, 10, 45);
        				mList.getChildAt(0).setBackgroundColor(0x55FF0000);
        			}            		
        		}, 100);
        		app.myAudio.speakQuickMessage(data.get(0));
        	}
        	else
        		app.myAudio.speakQuickMessage(res.getString(R.string.STT_NO_RESULTS_SPOKEN));

        }
        public void onPartialResults(Bundle partialResults)
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
        	if (BuildConfig.DEBUG) Log.d(CONST.TAG, TAG_CLASS + ": onEvent " + eventType);
        }
		
	}
	
	public void onPause() {
		super.onPause();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onPause");
    	progressDialog.dismiss();
    	sr.stopListening();
    	sr.destroy();
		if (smsMaybeSent) {
			app.myPrefs.putVal(CONST.MESSAGE_MAYBE_SENT, true);
			finish();
		}		
	}
	
	@Override
	public void onDestroy() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onDestroy");		
		super.onDestroy();
	}	
}
