package net.tidalsource.on.the.road.sms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {
	private static final String TAG_CLASS = "Preferences";
	private ListPreference prefLanguage, prefRecLanguage;
	private Preference prefReply1, prefReply2;
	private Preference prefAfterHoursA, prefAfterHoursB;
	
//	boolean clearRepeat;
	private String playbackLang;
	private EditText textReplyMessage;
	private static final int TIME_DIALOG_ID_A = 1002;
	private static final int TIME_DIALOG_ID_B = 1003;	
	private int selectedReplyMsg;
	private int selectedTimePicker;
	private List<String> languages;
	private Resources res;
	private App app;
	
	Handler handler = new Handler();
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onCreate()");
		
		app = (App)getApplicationContext();
		res = getResources();		
		getPreferenceManager().setSharedPreferencesName(CONST.PREF_NAME);
		addPreferencesFromResource(R.xml.preferences);
        
		prefLanguage = (ListPreference) findPreference("pref" + CONST.PLAYBACK_LANG);
		prefRecLanguage = (ListPreference) findPreference("pref" + CONST.RECORDING_LANGUAGE);
		
		prefReply1 = (Preference) findPreference("pref" + CONST.REPLY_MSG_1);     
        prefReply2 = (Preference) findPreference("pref" + CONST.REPLY_MSG_2);
        
        prefAfterHoursA = (Preference) findPreference("pref" + CONST.AFTER_HOURS_A);
        prefAfterHoursB = (Preference) findPreference("pref" + CONST.AFTER_HOURS_B);        
        prefAfterHoursA.setSummary(this.getString(R.string.AFTER_HOURS_SUMMARY_A) + " (" + app.myPrefs.getVal(CONST.AFTER_HOURS_A) + ")");
        prefAfterHoursB.setSummary(this.getString(R.string.AFTER_HOURS_SUMMARY_B) + " (" + app.myPrefs.getVal(CONST.AFTER_HOURS_B) + ")");
        
        
        // For language selector, to set summary once a language is selected
		OnPreferenceChangeListener myLanguageChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object arg1) {
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": OnPreferenceChangeListener(): preference = " + preference.getKey());
				
				if (preference.getKey().equals("pref" + CONST.PLAYBACK_LANG))
					prefLanguage.setSummary((CharSequence) arg1.toString());
				if (preference.getKey().equals("pref" + CONST.RECORDING_LANGUAGE)) {
					prefRecLanguage.setSummary(getLanguageDisplayName(arg1.toString()));

				}
				return true;
			}
        };
        
        // Custom default reply message dialog
        OnPreferenceClickListener myReplyMessageClickListener = new OnPreferenceClickListener() {
			
        	@Override
			public boolean onPreferenceClick(Preference preference) {
				if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": OnPreferenceClickListener(): preference = " + preference.getKey());

				View inflated = getLayoutInflater().inflate(R.layout.dialog_replymessage, null);
		        AlertDialog dialog = new AlertDialog.Builder(Preferences.this).create();
				dialog.setView(inflated);
				dialog.setTitle("Reply message");
				textReplyMessage = (EditText) inflated.findViewById(R.id.textReplyMsg);				
				
				if (preference.getKey().equals("pref" + CONST.REPLY_MSG_1)) {
					textReplyMessage.setText(app.myPrefs.getVal(CONST.REPLY_MSG_1).toString());
					selectedReplyMsg = CONST.REPLY_MSG_1;
				}
				else if (preference.getKey().equals("pref" + CONST.REPLY_MSG_2)) {
					textReplyMessage.setText(app.myPrefs.getVal(CONST.REPLY_MSG_2).toString());
					selectedReplyMsg = CONST.REPLY_MSG_2;
				}
				else
					return false;				
				
				dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": ClickListener(): Save button clicked");
						app.myPrefs.putVal(selectedReplyMsg, textReplyMessage.getText().toString());
					}
				});
				
				dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": ClickListener(): Cancel button clicked");
						dialog.cancel();
					}
				});
				
				dialog.show();
				dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
				textReplyMessage.requestFocus();				
				return true;
			}
        };
        
		prefLanguage.setOnPreferenceChangeListener(myLanguageChangeListener);
		prefRecLanguage.setOnPreferenceChangeListener(myLanguageChangeListener);
		
        prefReply1.setOnPreferenceClickListener(myReplyMessageClickListener);		
        prefReply2.setOnPreferenceClickListener(myReplyMessageClickListener);
        
        prefAfterHoursA.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				selectedTimePicker = CONST.AFTER_HOURS_A;
				showDialog(TIME_DIALOG_ID_A);
				return true;
			}        	
        });
        
        prefAfterHoursB.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				selectedTimePicker = CONST.AFTER_HOURS_B;
				showDialog(TIME_DIALOG_ID_B);
				return true;
			}        	
        });
        
        
	}

	public void onResume() {
		super.onResume();
		app.myPrefs.putVal(CONST.IN_PAUSED_MODE, true);		
		doLanguagePreps();
	}
	
	public void doLanguagePreps() {
		if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": doLanguagePreps()");
		
		// TTS Language
  		playbackLang = app.myPrefs.getVal(CONST.PLAYBACK_LANG);
		ArrayList<CharSequence> languageList;
		if (app.myTTS != null) {
			languageList = new ArrayList<CharSequence>();
			
			for(final Locale loc : Locale.getAvailableLocales()) {
				if (app.myTTS.isLanguageAvailable(loc) == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
					languageList.add((CharSequence) loc.getDisplayName());
					if (loc.getDisplayName().equals(playbackLang)) {
						app.myTTS.setLanguage(loc);
					}
				}
			}
			
			prefLanguage.setSummary(app.myTTS.getLanguage().getDisplayName());
			prefLanguage.setEntries(languageList.toArray(new CharSequence[languageList.size()]));
			prefLanguage.setEntryValues(languageList.toArray(new CharSequence[languageList.size()]));
		}
		
		// STT
        // Check to see if a recognition activity is present
		prefRecLanguage.setSummary(getLanguageDisplayName((String) app.myPrefs.getVal(CONST.RECORDING_LANGUAGE)));
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0) {
	        sendOrderedBroadcast(RecognizerIntent.getVoiceDetailsIntent(Preferences.this), null,
	                new SupportedLanguageBroadcastReceiver(), null, Activity.RESULT_OK, null, null);
        } else {
        	prefRecLanguage.setEnabled(false);
        	Toast.makeText(Preferences.this, "Voice recognition not found", Toast.LENGTH_SHORT).show();
        }
		
		
	}
		
    @Override
    public void onDestroy() {
    	if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + ": onDestroy()");    	
    	super.onDestroy();
    }

    
    
	TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
		
		public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {			
			app.myPrefs.putVal(selectedTimePicker, selectedHour + ":" + pad(selectedMinute));

	        prefAfterHoursA.setSummary(res.getString(R.string.AFTER_HOURS_SUMMARY_A) + " (" + app.myPrefs.getVal(CONST.AFTER_HOURS_A) + ")");
	        prefAfterHoursB.setSummary(res.getString(R.string.AFTER_HOURS_SUMMARY_B) + " (" + app.myPrefs.getVal(CONST.AFTER_HOURS_B) + ")");
		}

		private String pad(int c) {
			if (c >= 10)
				return String.valueOf(c);
			else
				return "0" + String.valueOf(c);
		}		
	};	
    
	@Override
    protected Dialog onCreateDialog(int id) {
		String strTime;
		int hour, minute;
		switch (id) {
		case TIME_DIALOG_ID_A:
			strTime = app.myPrefs.getVal(selectedTimePicker);
			hour = Integer.parseInt(strTime.substring(0, strTime.indexOf(":")));
			minute = Integer.parseInt(strTime.substring(strTime.indexOf(":") + 1, strTime.length()));
			return new TimePickerDialog(this, timePickerListener, hour, minute, true);			
		case TIME_DIALOG_ID_B:
			strTime = app.myPrefs.getVal(selectedTimePicker);
			hour = Integer.parseInt(strTime.substring(0, strTime.indexOf(":")));
			minute = Integer.parseInt(strTime.substring(strTime.indexOf(":") + 1, strTime.length()));
			return new TimePickerDialog(this, timePickerListener, hour, minute, true);			
		}
		return null;
	}
	

    private class SupportedLanguageBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, final Intent intent) {
        	if (BuildConfig.DEBUG) Log.v(CONST.TAG, TAG_CLASS + "Receiving broadcast " + intent);

            final Bundle extra = getResultExtras(true);

            if (getResultCode() != Activity.RESULT_OK) {
            	handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(Preferences.this, res.getString(R.string.STT_ERROR) + getResultCode(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else if (extra.isEmpty()) {
            	handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(Preferences.this, res.getString(R.string.STT_NO_EXTRAS), Toast.LENGTH_SHORT).show();
                		CharSequence[] languages = ((String) app.myPrefs.getVal(CONST.AVAILABLE_REC_LANGUAGES)).split(";");
                		CharSequence[] lang_ids = ((String) app.myPrefs.getVal(CONST.AVAILABLE_REC_LANG_IDS)).split(";");
                		prefRecLanguage.setEntries(res.getStringArray(R.array.LANGUAGE_NAMES));
                		prefRecLanguage.setEntryValues(res.getStringArray(R.array.LANGUAGE_IDS));
                    }
                });
            }

            else if (extra.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
            	handler.post(new Runnable() {

                    @Override
                    public void run() {
                    	// Have we stored the language data in preference already, if so then just get it from there
                    	if (app.myPrefs.getVal(CONST.AVAILABLE_REC_LANGUAGES).equals(""))
                    		updateSupportedLanguages(extra.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES));
                    	else {
                    		CharSequence[] languages = ((String) app.myPrefs.getVal(CONST.AVAILABLE_REC_LANGUAGES)).split(";");
                    		CharSequence[] lang_ids = ((String) app.myPrefs.getVal(CONST.AVAILABLE_REC_LANG_IDS)).split(";");
                    		prefRecLanguage.setEntries(languages);
                    		prefRecLanguage.setEntryValues(lang_ids);
                    	}
                    }
                });
            }
        }
    }
	
    private void updateSupportedLanguages(List<String> langs) {
    	this.languages = langs;
    	
        List<String> languagesNamed = new ArrayList<String>();
        List<String> languagesId = new ArrayList<String>();
        
        // Add default language row
//        languagesNamed.add(res.getString(R.string.STT_DEFAULT_TEXT));			
//        languagesId.add(res.getString(R.string.STT_DEFAULT_LANGUAGE));

        // Get all languages we think exist and get their names
        for(Locale loc : Locale.getAvailableLocales())	        	
        	if (loc.toString().length() == 5)
        		for (int i = 0; i < languages.size(); i++) 
        			if (languages.get(i).length() == 5 && loc.toString().equals(languages.get(i).replaceFirst("-", "_"))) {
        				languagesNamed.add(loc.getDisplayName());
        				languagesId.add(languages.get(i));
        				break;
        			}
        
        // Put to preference list
        prefRecLanguage.setEntries(languagesNamed.toArray(new CharSequence[languagesNamed.size()]));
        prefRecLanguage.setEntryValues(languagesId.toArray(new CharSequence[languagesId.size()]));	
        
        // Store to preferences so we dont have to run this slow function again
        String languages = "";
        String lang_ids = "";
        for (int i = 0; i < languagesNamed.size(); i++) {
        	languages += languagesNamed.get(i) + ";";
        	lang_ids += languagesId.get(i) + ";";
        }
        app.myPrefs.putVal(CONST.AVAILABLE_REC_LANGUAGES, languages);
        app.myPrefs.putVal(CONST.AVAILABLE_REC_LANG_IDS, lang_ids);
    }
    
    public CharSequence getLanguageDisplayName(String localeShort) {
        for(Locale loc : Locale.getAvailableLocales())	        	
        	if (loc.toString().length() == 5)
        		if (loc.toString().equals(localeShort.toString().replaceFirst("-", "_")))
        			return loc.getDisplayName();
        return "";
    }  

}

