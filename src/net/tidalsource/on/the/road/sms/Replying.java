package net.tidalsource.on.the.road.sms;

import android.app.Activity;
import android.content.Context;
import android.view.View.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class Replying extends Activity implements OnClickListener {
	private final String TAG_CLASS = "Replying";
	private static Context context;
	private Bundle bundle;
	private Button btnSpeak, btnReply1, btnReply2, btnCancel;
	private Boolean smsSent;
	private Resources res;
	private App app;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (App)getApplicationContext();
		res = getResources();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate()");
		context = getApplicationContext();

		// Get through keylock and when screen is off
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
			    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		
		setContentView(R.layout.replymenu);
		btnSpeak = (Button) findViewById(R.id.btnSpeak);
		btnReply1 = (Button) findViewById(R.id.btnReply1);
		btnReply2 = (Button) findViewById(R.id.btnReply2);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		
		btnSpeak.setOnTouchListener(new MyHapticFeedback());
		btnReply1.setOnTouchListener(new MyHapticFeedback());
		btnReply2.setOnTouchListener(new MyHapticFeedback());
		btnCancel.setOnTouchListener(new MyHapticFeedback());
		
		btnSpeak.setOnClickListener(this);
		btnReply1.setOnClickListener(this);
		btnReply2.setOnClickListener(this);
		btnCancel.setOnClickListener(this);
		
		app.myPrefs = new MyPrefs(context);
		smsSent = false;
		
		bundle = getIntent().getExtras();
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate(): bundle = " + (bundle != null ? true : false));
		if (bundle == null) {
			finish();
		}
	}
	
	public void onResume() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onResume()");
		super.onResume();
		app.myPrefs.putVal(CONST.IN_PAUSED_MODE, false);
		if (smsSent || (Boolean) app.myPrefs.getVal(CONST.MESSAGE_MAYBE_SENT)) {
			app.myPrefs.putVal(CONST.MESSAGE_MAYBE_SENT, false);
			new asyncReplyNotification().execute();
			//finish();
		}		
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

	
	public void defaultReplyMsgIntent(String address, String body) {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": defaultReplyMsgIntent()");
		smsSent = true;
		Intent replyIntent = new Intent(Intent.ACTION_VIEW);
		replyIntent.setData(Uri.parse("sms:"));
		replyIntent.putExtra("address", address);
		replyIntent.putExtra("sms_body", body);
		replyIntent.putExtra("exit_on_sent", true);		
		replyIntent.setData(Uri.parse("smsto:" + address));
		startActivity(replyIntent);
		app.myPrefs.putVal(CONST.LAST_MSG_ID_BEFORE_REPLY, app.myMessage.getMessageFromDb(2));
	}
	
    public void onClick(View v) {
    	if (v.getId() == R.id.btnSpeak) {
    		Intent voiceIntent = new Intent(Replying.this, Voicereply.class);
        	voiceIntent.putExtra("address", bundle.getString("address"));
        	startActivity(voiceIntent);
        }
        else if (v.getId() == R.id.btnReply1) {
    		String address = bundle.getString("address");
    		String body = app.myPrefs.getVal(CONST.REPLY_MSG_1).toString();
    		defaultReplyMsgIntent(address, body);        	
        }
        else if (v.getId() == R.id.btnReply2) {
    		String address = bundle.getString("address");
    		String body = app.myPrefs.getVal(CONST.REPLY_MSG_2).toString();
    		defaultReplyMsgIntent(address, body);        	
        }
        else if (v.getId() == R.id.btnCancel) {
    		finish();        	
        }
    }
	
	public class asyncReplyNotification extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			if (app.myMessage.delayWhileMsgArrives((Integer) app.myPrefs.getVal(CONST.LAST_MSG_ID_BEFORE_REPLY), 2)) {
				app.myAudio.speakQuickMessage(res.getString(R.string.MESSAGE_SENT_SPOKEN));
				return true;
			}
			return false;
		}
		@Override
		protected void onPostExecute(Boolean wasReallySent) {
			if (wasReallySent) {
				Toast.makeText(app, res.getString(R.string.MESSAGE_SENT_TOAST), Toast.LENGTH_SHORT).show();
				finish();
			}
		}		
	}
}
