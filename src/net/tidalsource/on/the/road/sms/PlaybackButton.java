package net.tidalsource.on.the.road.sms;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlaybackButton extends RelativeLayout {
	private TextView textTitle, textBody;
		
	public PlaybackButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		((Activity) getContext()).getLayoutInflater().inflate(R.layout.playback_button, this);
		setupViewItems();		
	}
	
	private void setupViewItems() {
		textTitle = (TextView) findViewById(R.id.playingButtonTitle);
		textBody = (TextView) findViewById(R.id.playingButtonBody);
	}

	public void textTitle(String text) {
		textTitle.setText(text);
	}
 
	public void textBody(String text) {
		textBody.setText(text);
	}	
}