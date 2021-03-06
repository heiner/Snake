
package com.kuettler.snake;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Bitmap;

public class SnakeView extends SurfaceView implements SurfaceHolder.Callback
{
    private static final String TAG = "SnakeActivity";

    class SnakeThread extends Thread
    {
        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        private SnakeView mView;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Used to figure out elapsed time between frames */
        private long mLastTime;

        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;

        private Snake snake;

        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        public SnakeThread(SurfaceHolder holder, SnakeView view, Context context,
                          Handler handler) {
            mSurfaceHolder = holder;
            mView = view;
            mHandler = handler;

            snake = new Snake(Color.RED);
        }

        public void setRunning(boolean b) {
            mRun = b;
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas canvas = null;
                try {
                    canvas = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        canvas.drawColor(Color.BLACK);
                        snake.draw(canvas);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }

        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                mMode = mode;
                Log.d(TAG, "Mode is now " + mode);
            }

            Message msg = mHandler.obtainMessage();
            Bundle b = new Bundle();

            if (mMode == STATE_RUNNING) {
                b.putString("text", "");
                b.putInt("viz", View.INVISIBLE);
            } else if (mMode == STATE_PAUSE) {
                b.putString("text", "Pause");
                b.putInt("viz", View.VISIBLE);
            }
            msg.setData(b);
            mHandler.sendMessage(msg);
        }

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
            }
            setState(STATE_RUNNING);
        }

        public void togglePause() {
            if ( mMode == STATE_RUNNING )
                pause();
            else
                unpause();
        }

        private void updatePhysics() {
            long now = System.currentTimeMillis();

            if (mLastTime > now)
                return;

            long elapsed = now - mLastTime;

            snake.integrate(now/1000f, elapsed/1000f);
            mLastTime = now;
        }

        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                mLastTime = System.currentTimeMillis() + 100;
                setState(STATE_PAUSE); // start in pause
            }
        }

        public void setSurfaceSize(int w, int h) {
            synchronized (mSurfaceHolder) {
                Rect boundary = new Rect(0, 0, w, h);
                //boundary.inset(30, 30);
                snake.setBoundary(boundary);
                snake.setPosition(w/2f, 5*h/8f);
            }
        }

	public boolean doDown(MotionEvent e) {
            if (mMode == STATE_RUNNING) {
                snake.setGoal(e.getX(), e.getY());
            }
            return true;
	}

	public boolean doScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
            if (mMode == STATE_RUNNING) {
                snake.setGoal(e2.getX(), e2.getY());
                return true;
            }
            else
                return false;
	}

	public boolean doFling (MotionEvent e1, MotionEvent e2,
				float vX, float vY) {
            //ball.setGoal(e2.getX(), e2.getY(), vX, vY);
            //ball.setConstants(10, 10);
            /*ball.setMode(Ball.MODE_FREE);
            ball.state.x = e2.getX();
            ball.state.y = e2.getY();*/
	    return false;
	}

	public boolean doDoubleTap(MotionEvent e) {
            togglePause();
            return true;
        }


        public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    //map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    map.putFloat("snake.pos.x", snake.pos.x);
                    map.putFloat("snake.pos.y", snake.pos.y);
                }
            }
            return map;
        }
    }

    private GestureDetector gestures;
    private SnakeThread thread;
    private Context mContext;
    private TextView mStatusText;

    public SnakeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new SnakeThread(holder, this, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });

        gestures = new GestureDetector
            (context,
             new GestureListener(thread));
    }

    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
        thread.doStart();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        thread.setSurfaceSize(width, height);
    }

    public SnakeThread getThread() {
        return thread;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestures.onTouchEvent(event);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TAG, "SnakeView.onWindowFocusChanged: " + hasFocus);

        if (!hasFocus)
            thread.pause();

        /*if (hasFocus)
            thread.unpause();
        else
        thread.pause();*/
    }

    class Snake {
        protected class State extends PointF
        {
            public State() {}
            public State(float x, float y) { super(x, y); }
            //public State(PointF p) { super(p); }

            @Override
            public String toString() {
                return "State(" + x + ", " + y + ")";
            }

            public State scale(float a) {
                return new State(a*x, a*y);
            }

            public State add(PointF other) {
                return new State(this.x + other.x, this.y + other.y);
            }

            public State subtract(PointF other) {
                return new State(this.x - other.x, this.y - other.y);
            }

            public State normed() {
                if ( equals(0f, 0f) )
                    return this;
                return scale(1f/length());
            }
        }

        protected class Derivative
        {
            public Derivative() {}
            public Derivative(State v, State a) {
                vel = v;
                acc = a;
            }

            public State vel;
            public State acc;
        }

	private int color;
	protected float width;

	protected Rect boundary;
	//protected RectF inset;

        protected final State pos;
        protected final State vel;

        protected final Path path;
        protected final PathMeasure pathMeasure;

        private Bitmap bitmap;

        protected float maxLength;
        protected float startLength;
        protected float speed;

        protected final State goal;
        protected float goal_dist = 50f;
        protected float max_goal_dist = 50f;

        public static final int MODE_FREE = 1;
        public static final int MODE_FORCED = 2;
        public static final int MODE_CRASH = 3;
        protected int mode;

        /** Spring tightness */
        protected float k;

        /** Damping coefficient */
        protected float b;

        protected long last_vibrate_time = 0;
        protected final long vibrate_length = 40;
        protected final Vibrator vibrator =
            (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
        protected boolean human;

	public Snake(int color) {
            this.color = color;
            width = 10f;
            maxLength = 500f;
            startLength = 50f;
            speed = 100f;

            pos = new State(0,0);
            vel = new State(0,0);

            path = new Path();
            pathMeasure = new PathMeasure();

            goal = new State(0,0);

            mode = MODE_FREE;

            setConstants(80f, 50f);
	}

	public void setPosition(float x, float y) {
            Log.d(TAG, "Snake: setPosition=" + x + ", " + y);
            pos.x = goal.x = x;
            pos.y = goal.y = y;
            vel.x = vel.y = 0;

            path.reset();
            path.setLastPoint(x, y + startLength);
            path.lineTo(x, y);
	}

        public void setGoal(PointF g) {
            setGoal(g.x, g.y);
        }

	public void setGoal(float x, float y) {
            goal.x = x;
            goal.y = y;

            State dir = goal.subtract(pos);
            dir = dir.normed().scale(goal_dist);
            goal.set(pos.add(dir));
	}

        public void setMode(int m) {
            //if (human && m == MODE_COLLISION && mode != MODE_COLLISION &&
            //    lastBallContact.difference.dnorm() > 150)
            //    vibrate();
            mode = m;
        }

        public void setConstants(float k, float b) {
            this.k = k;
            this.b = b;
        }

        public void setBoundary(Rect b) {
            boundary = b;
            // the lookup bitmap. ALPHA_8 does not seem to work.
            bitmap = Bitmap.createBitmap(b.width(), b.height(),
                                         Bitmap.Config.RGB_565);
            //inset = new RectF(b);
            //inset.inset(radius, radius);
        }

	public void draw(Canvas canvas) {
	    Paint paint = new Paint();
	    paint.setAntiAlias(true);

            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(width);

            canvas.drawPath(path, paint);

            // // DEBUG: draw circle around goal
            // paint.setColor(0xffaabbdd);
            // paint.setStrokeWidth(2);
            // canvas.drawCircle(goal.x, goal.y, max_goal_dist, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.YELLOW);
            canvas.drawCircle(pos.x, pos.y, width, paint);

            //if ( true )
            //    return;

            // DEBUG: draw tail path with collision indication
            Path tail = tailPath();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.GREEN);
            if ( mode == MODE_CRASH )
                paint.setColor(Color.WHITE);
            canvas.drawPath(tail, paint);
	}

        protected Derivative evaluate(float t, float dt, Derivative D) {
            Derivative result = new Derivative();

            State newPos = pos.add(D.vel.scale(dt));
            State newVel = vel.add(D.acc.scale(dt));

            result.vel = newVel;

            State dir = newPos.subtract(goal);
            result.acc = dir.scale(-k).subtract(newVel.scale(b));

            return result;
        }

        protected void integrate(float t, float dt) {
            // gafferongames.com/game-physics/integration-basics/
            Derivative a = evaluate(t, 0.0f, new Derivative(new State(),
                                                            new State()));
            Derivative b = evaluate(t+dt*0.5f, dt*0.5f, a);
            Derivative c = evaluate(t+dt*0.5f, dt*0.5f, b);
            Derivative d = evaluate(t+dt, dt, c);

            float dxdt  = 1f/6f * (a.vel.x + 2f*(b.vel.x + c.vel.x) + d.vel.x);
            float dydt  = 1f/6f * (a.vel.y + 2f*(b.vel.y + c.vel.y) + d.vel.y);
            float ddxdt = 1f/6f * (a.acc.x + 2f*(b.acc.x + c.acc.x) + d.acc.x);
            float ddydt = 1f/6f * (a.acc.y + 2f*(b.acc.y + c.acc.y) + d.acc.y);

            pos.x = pos.x + dxdt * dt;
            pos.y = pos.y + dydt * dt;
            vel.x = vel.x + ddxdt * dt;
            vel.y = vel.y + ddydt * dt;

            path.lineTo(pos.x, pos.y);

            pathMeasure.setPath(path, false);
            float length = pathMeasure.getLength();
            Path dst = new Path();

            if ( length > maxLength ) {
                pathMeasure.getSegment(length - maxLength, length,
                                       dst, true);
                path.set(dst);
            }

            Path tail = tailPath();
            bitmap.eraseColor(Color.BLACK);
            Canvas canvas = new Canvas(bitmap);

	    Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(width);
            paint.setColor(Color.WHITE);

            canvas.drawPath(tail, paint);

            if ( bitmap.getPixel((int)pos.x, (int)pos.y) == Color.WHITE ) {
                if (mode != MODE_CRASH)
                    Log.d(TAG, "Collision!");
                setMode(MODE_CRASH);
             }
             else {
                 if (mode == MODE_CRASH)
                     Log.d(TAG, "Uncrash");
                 setMode(MODE_FORCED);
             }

            setGoal(goal.add(vel.normed().scale(10)));
        }

        private Path tailPath() {
            Path result = new Path();
            pathMeasure.setPath(path, false);
            pathMeasure.getSegment(0, 0.95f*pathMeasure.getLength(),
                                   result, true);
            return result;
        }

        // public Rect headRect() {
        //     Rect result = new Rect((int)pos.x, (int)pos.y,
        //                            (int)pos.x, (int)pos.y);
        //     result.inset(-(int)width, -(int)width);
        //     return result;
        // }

        public void vibrate() {
            long now = System.currentTimeMillis();
            if (now - last_vibrate_time > 150)
                vibrator.vibrate(vibrate_length);
            last_vibrate_time = now;
        }

        @Override
        public String toString() {
            return getClass().getName() + ": Color=" + color;
        }

        public float distanceToGoal() {
            return pos.subtract(goal).length();
        }
    }

    private class GestureListener implements GestureDetector.OnGestureListener,
					     GestureDetector.OnDoubleTapListener
    {
	SnakeThread thread;

        public GestureListener(SnakeThread thread) {
	    this.thread = thread;
	}

	@Override
	public boolean onDown(MotionEvent e) {
            return thread.doDown(e);
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float dX, float dY) {
	    //player.moveTo(e2.getX(), e2.getY());
            //Log.d(TAG, "onScroll: " + e2.toString());
            return thread.doScroll(e1, e2, dX, dY);
	}

	@Override
	public boolean onFling (MotionEvent e1, MotionEvent e2,
                                float vX, float vY) {
            //Log.d(TAG, "onFling" + e1.toString());
            return thread.doFling(e1, e2, vX, vY);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
            //Log.d(TAG, "onDoubleTap" + e.toString());
            return thread.doDoubleTap(e);
	}

	@Override
	public void onLongPress(MotionEvent e) {
            //Log.d(TAG, "onLongPress" + e.toString());
	}

	@Override
	public void onShowPress(MotionEvent e) {
            //Log.d(TAG, "onShowPress" + e.toString());
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
            //Log.d(TAG, "onSingleTapUp: " + e.toString());
            //thread.doSingleTap(e);
	    return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
            //Log.d(TAG, "onDoubleTapEvent: " + e.toString());
	    return false;
	}

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
            //Log.d(TAG, "onSingleTapConfirmed: " + e.toString());
            return true;
	}
    }
}
