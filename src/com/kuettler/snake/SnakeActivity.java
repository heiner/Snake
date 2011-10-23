package com.kuettler.snake;

import java.util.ArrayList;

import android.app.Activity;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.os.Bundle;
import android.widget.Toast;

public class SnakeActivity extends Activity implements OnGesturePerformedListener
{
    private GestureLibrary mLibrary;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

	/* Load the gestures */
	mLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
	if (!mLibrary.load()) {
	    finish();
	}
	GestureOverlayView gestureOverlayView = (GestureOverlayView) findViewById(R.id.gestureOverlayView);
	//gestures.addOnGesturePerformedListener(this);
	gestureOverlayView.addOnGesturePerformedListener(this);
	//setContentView(gestureOverlayView);
    }

    public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	ArrayList<Prediction> predictions = mLibrary.recognize(gesture);

	if (predictions.size() > 0) {
	    Prediction prediction = predictions.get(0);
	    // We want at least some confidence in the result
	    if (prediction.score > 1.0) {
		Toast.makeText(this, prediction.name, Toast.LENGTH_SHORT).show();
	    }
	}
    }
}
