
package com.kuettler.snake;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kuettler.snake.SnakeView;
import com.kuettler.snake.SnakeView.SnakeThread;


public class SnakeActivity extends Activity
{
    private static final String TAG = "SnakeActivity";

    private SnakeThread mSnakeThread;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        SnakeView snakeview = (SnakeView) findViewById(R.id.snakeview);
        mSnakeThread = snakeview.getThread();

        snakeview.setTextView((TextView) findViewById(R.id.text));
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mSnakeThread.pause();
    }

    /*
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    // @Override
    // public void onWindowFocusChanged (boolean hasFocus) {
    //     super.onWindowFocusChanged(hasFocus);
    //     Log.d(TAG, "onWindowFocusChanged: " + hasFocus);
    // }
    */

    private static final int MENU_START = 0;
    private static final int MENU_STOP = 1;
    private static final int MENU_PAUSE = 2;
    private static final int MENU_RESUME = 3;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        menu.add(0, MENU_PAUSE, 0, R.string.menu_pause);
        menu.add(0, MENU_RESUME, 0, R.string.menu_resume);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_START:
            mSnakeThread.doStart();
            return true;
        case MENU_STOP:
            //mSnakeThread.setState(LunarThread.STATE_LOSE,
            //                      getText(R.string.message_stopped));
            return true;
        case MENU_PAUSE:
            mSnakeThread.pause();
            return true;
        case MENU_RESUME:
            mSnakeThread.unpause();
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // just have the View's thread save its state into our Bundle
        super.onSaveInstanceState(outState);
        mSnakeThread.saveState(outState);
        Log.d(TAG, "SIS called");
    }
}
