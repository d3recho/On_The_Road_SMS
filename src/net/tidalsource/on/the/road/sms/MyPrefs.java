package net.tidalsource.on.the.road.sms;

import java.util.Locale;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class MyPrefs {
	protected static SharedPreferences prefs;
	protected static Context context;
	protected Resources res;
	
	public MyPrefs(Context c) {
		context = c;
		res = c.getResources();
		getPrefs(CONST.PREF_NAME);
	}

	private boolean getPrefs(String prefsName) {
		prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
		if (prefs == null)
			return false;
		return true;
	}	
	
	@SuppressWarnings("unchecked")
	public <T> T getVal(int prefId) {
		if (prefs == null)
			getPrefs(CONST.PREF_NAME);
		
		Object result = null;
		
		switch (prefId) {
		case CONST.PLAYBACK_LANG :			result = prefs.getString("pref" + prefId, Locale.US.getDisplayName()); break;
		case CONST.APP_VOLUME : 			result = prefs.getInt("pref" + prefId, 10); break;
		case CONST.MISSED_MESSAGE : 		result = prefs.getBoolean("pref" + prefId, false); break;
		case CONST.IN_PAUSED_MODE: 			result = prefs.getBoolean("pref" + prefId, false); break;
		case CONST.MESSAGE_MAYBE_SENT:		result = prefs.getBoolean("pref" + prefId, false); break;
		case CONST.CLEAR_REPEAT:			result = prefs.getBoolean("pref" + prefId, false); break;
		case CONST.AFTER_HOURS_A : 			result = prefs.getString("pref" + prefId, "22:00"); break;
		case CONST.AFTER_HOURS_B : 			result = prefs.getString("pref" + prefId, "07:00"); break;		
		case CONST.AFTER_HOURS_ENABLED:		result = prefs.getBoolean("pref" + prefId, false); break;
		case CONST.APP_MODE : 				result = prefs.getInt("pref" + prefId, 1); break;
		case CONST.RESTORE_TO_VOL : 		result = prefs.getInt("pref" + prefId, -1); break; 
		case CONST.LAST_PLAYED_MSG_ID : 	result = prefs.getString("pref" + prefId, "-1"); break;
		case CONST.LAST_MSG_ID_BEFORE_REPLY:result = prefs.getInt("pref" + prefId, -1); break;
		case CONST.RECORDING_LANGUAGE :		result = prefs.getString("pref" + prefId, res.getString(R.string.STT_DEFAULT_LANGUAGE)); break;
		case CONST.REPLY_MSG_1 :			result = prefs.getString("pref" + prefId, res.getString(R.string.DEFAULT_REPLY_MSG_1)); break;
		case CONST.REPLY_MSG_2 :			result = prefs.getString("pref" + prefId, res.getString(R.string.DEFAULT_REPLY_MSG_2)); break;
		case CONST.AVAILABLE_REC_LANGUAGES :result = prefs.getString("pref" + prefId, ""); break;
		case CONST.AVAILABLE_REC_LANG_IDS :	result = prefs.getString("pref" + prefId, ""); break;
		case CONST.IS_FIRST_RUN:			result = prefs.getBoolean("pref" + prefId, true); break;
		case CONST.APP_VERSION : 			result = prefs.getInt("pref" + prefId, 0); break;
		}
		
		return (T) result;	
	} 
	
	public <T> boolean putVal(int prefId, T value) {
		SharedPreferences.Editor edit = prefs.edit();
		
		if (value.getClass() == String.class)
			edit.putString("pref" + prefId, (String) value);
		else if (value.getClass() == Integer.class)
			edit.putInt("pref" + prefId, (Integer) value);
		else if (value.getClass() == Boolean.class)
			edit.putBoolean("pref" + prefId, (Boolean) value);

		return edit.commit();
		
/*		switch (prefId) {
		case CONST.PLAYBACK_LANG :		edit.putString("pref" + prefId, (String) value); break;
		case CONST.USE_MAX_VOL : 		edit.putBoolean("pref" + prefId, (Boolean) value); break;
		case CONST.REPLY_MSG_1 : 		edit.putString("pref" + prefId, (String) value); break;
		case CONST.REPLY_MSG_2 : 		edit.putString("pref" + prefId, (String) value); break;
		case CONST.MISSED_MESSAGE : 	edit.putBoolean("pref" + prefId, (Boolean) value); break;

		case CONST.APP_MODE : 		edit.putInt("pref" + prefId, (Integer) value); break;
		case CONST.BRING_TO_FRONT : 	edit.putBoolean("pref" + prefId, (Boolean) value); break;
		case CONST.AUTO_READ_MSG : 		edit.putBoolean("pref" + prefId, (Boolean) value); break;

		case CONST.RESTORE_TO_VOL : 	edit.putInt("pref" + prefId, (Integer) value); break;
		case CONST.LAST_PLAYED_MSG_ID : edit.putString("pref" + prefId, (String) value); break;		
		}	*/		
	}	
}
