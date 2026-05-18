package org.runnerup.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import org.runnerup.BuildConfig;
import org.runnerup.common.tracker.TrackerState;
import org.runnerup.tracker.Tracker;
import org.runnerup.util.Formatter;
import org.runnerup.util.TickListener;
import org.runnerup.workout.Scope;
import org.runnerup.workout.Workout;

import java.util.Timer;
import java.util.TimerTask;

public abstract class BaseRunActivity extends AppCompatActivity implements TickListener {

    protected Workout workout = null;
    protected Tracker mTracker = null;
    protected Formatter formatter = null;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Timer timer = null;
    private boolean mIsBound = false;

    protected final void initRunSession() {
        bindGpsTracker();
    }

    protected void onWorkoutReady() {
        // Optional for subclasses
    }

    protected void onRunTick() {
        // Optional for subclasses
    }

    protected abstract void onRunDataUpdated();

    protected abstract void onPauseStateChanged(boolean paused);

    private void onGpsTrackerBound() {
        if (mTracker == null) {
            return;
        }

        workout = mTracker.getWorkout();

        if (workout == null) {
            return;
        }

        onWorkoutReady();
        onPauseStateChanged(workout.isPaused());
        onRunDataUpdated();

        startTimer();
        mTracker.displayNotificationState();
    }

    @Override
    public final void onTick() {
        if (workout == null) {
            return;
        }

        workout.onTick();

        if (mTracker != null
                && mTracker.getState() == TrackerState.STOPPED
                && !workout.isPaused()) {
            stopCurrentRun();
            return;
        }

        onRunTick();
        onRunDataUpdated();
    }

    protected final void togglePauseState() {
        if (workout == null) {
            return;
        }

        if (workout.isPaused()) {
            workout.onResume(workout);
        } else {
            workout.onPause(workout);
        }

        onPauseStateChanged(workout.isPaused());
        onRunDataUpdated();
    }

    protected final void stopCurrentRun() {
        if (timer == null || workout == null || mTracker == null) {
            return;
        }

        workout.onStop(workout);
        stopTimer();

        mTracker.stopForeground(true);

        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("mode", "save");
        intent.putExtra("ID", mTracker.getActivityId());

        startActivityForResult(intent, workout.isPaused() ? 1 : 0);
    }

    private void startTimer() {
        stopTimer();

        timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        handler.post(BaseRunActivity.this::onTick);
                    }
                },
                0,
                500
        );
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (workout == null || mTracker == null) {
            finish();
            return;
        }

        if (resultCode == RESULT_OK) {
            Double manualDistance = null;

            if (data != null && data.hasExtra("MANUAL_DISTANCE")) {
                manualDistance = data.getDoubleExtra("MANUAL_DISTANCE", 0);
            }

            workout.onComplete(Scope.ACTIVITY, workout);
            mTracker.completeActivity(true, manualDistance);

            mTracker = null;
            finish();

        } else if (resultCode == RESULT_CANCELED) {
            workout.onComplete(Scope.ACTIVITY, workout);
            mTracker.completeActivity(false, null);

            mTracker = null;
            finish();

        } else if (resultCode == RESULT_FIRST_USER) {
            startTimer();

            if (requestCode == 0) {
                workout.onResume(workout);
            }

            onPauseStateChanged(workout.isPaused());
            onRunDataUpdated();

        } else {
            if (BuildConfig.DEBUG) {
                throw new AssertionError();
            }
        }
    }

    private final ServiceConnection mConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    if (mTracker == null) {
                        mTracker = ((Tracker.LocalBinder) service).getService();
                        onGpsTrackerBound();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName className) {
                    mIsBound = false;
                    mTracker = null;
                }
            };

    private void bindGpsTracker() {
        mIsBound =
                getApplicationContext().bindService(
                        new Intent(this, Tracker.class),
                        mConnection,
                        Context.BIND_AUTO_CREATE
                );
    }

    private void unbindGpsTracker() {
        if (mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindGpsTracker();
        stopTimer();
    }
}