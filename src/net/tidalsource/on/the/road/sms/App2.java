package net.tidalsource.on.the.road.sms;

//ACRA 
import org.acra.*;
import org.acra.annotation.*;

import android.app.Application;
@ReportsCrashes(formKey = "dDBUNl9kTml5dklpN01LVXJycTZTTmc6MQ") 

public class App2 extends Application {
	  @Override
	  public void onCreate() {
	    // The following line triggers the initialization of ACRA
	    ACRA.init(this);
	    super.onCreate();
	  }
}