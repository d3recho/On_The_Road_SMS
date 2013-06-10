package net.tidalsource.on.the.road.sms;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class About extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
				
		// Determine if newer version has been installed
		PackageInfo packageInfo = null;
		String app_version = "";
		String app_name = "On The Road SMS";
		
		try {
			packageInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
		app_version = packageInfo.versionName;
		this.setTitle(app_name + " " + app_version);
		
	    // text6 has links specified by putting <a> tags in the string
	    // resource.  By default these links will appear but not
	    // respond to user input.  To make them active, you need to
	    // call setMovementMethod() on the TextView object.

	    TextView t = (TextView) findViewById(R.id.aboutText6);
	    t.setMovementMethod(LinkMovementMethod.getInstance());
	}


}
