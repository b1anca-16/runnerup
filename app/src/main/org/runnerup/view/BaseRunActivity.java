package org.runnerup.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    protected Timer timer = null;
    private boolean mIsBound = false;

    // FIX #1: startActivityForResult ist deprecated – ActivityResultLauncher verwenden
    private boolean mTickRunning = false; // FIX #2: Schutz gegen gestapelte Tick-Aufrufe

    protected final ActivityResultLauncher<Intent> detailActivityLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    this::handleDetailActivityResult
            );

    protected final void initRunSession() {
        bindGpsTracker();
    }

    protected void onWorkoutReady() {
        // Optional für Subklassen
    }

    protected void onRunTick() {
        // Optional für Subklassen
    }

    protected abstract void onRunDataUpdated();

    protected abstract void onPauseStateChanged(boolean paused);

    private void onGpsTrackerBound() {
        Log.d("BaseRunActivity", "onGpsTrackerBound, mTracker=" + mTracker);
        if (mTracker == null) {
            Log.e("BaseRunActivity", "Tracker binding failed: mTracker is null");
            return;
        }

        workout = mTracker.getWorkout();

        if (workout == null) {
            Log.e("BaseRunActivity", "Tracker is bound, but workout is null");
            return;
        }

        Log.d("BaseRunActivity", "Tracker bound and workout loaded");

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

        // FIX #2: Verhindert gestapelte Aufrufe wenn onTick länger als 500ms dauert
        if (mTickRunning) {
            return;
        }

        mTickRunning = true;

        try {
            workout.onTick();

            if (mTracker != null
                    && mTracker.getState() == TrackerState.STOPPED
                    && !workout.isPaused()) {
                stopCurrentRun();
                return;
            }

            onRunTick();
            onRunDataUpdated();
        } finally {
            mTickRunning = false;
        }
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

    protected void stopCurrentRun() {
        if (timer == null || workout == null || mTracker == null) {
            return;
        }

        workout.onStop(workout);
        stopTimer();

        mTracker.stopForeground(true);

        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("mode", "save");
        intent.putExtra("ID", mTracker.getActivityId());

        // FIX #1: Moderner Ersatz für startActivityForResult
        detailActivityLauncher.launch(intent);
    }

    // FIX #1: Callback-Methode für ActivityResultLauncher
    private void handleDetailActivityResult(ActivityResult result) {
        if (workout == null || mTracker == null) {
            finish();
            return;
        }

        int resultCode = result.getResultCode();
        Intent data = result.getData();

        if (resultCode == RESULT_OK) {
            Double manualDistance = null;

            if (data != null && data.hasExtra("MANUAL_DISTANCE")) {
                manualDistance = data.getDoubleExtra("MANUAL_DISTANCE", 0);
            }

            workout.onComplete(Scope.ACTIVITY, workout);
            mTracker.stopForeground(true);
            mTracker.completeActivity(true, manualDistance);

            mTracker = null;
            Intent intent = new Intent(this, MainLayout.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();

        } else if (resultCode == RESULT_CANCELED) {
            workout.onComplete(Scope.ACTIVITY, workout);
            mTracker.completeActivity(false, null);

            mTracker = null;
            Intent intent = new Intent(this, MainLayout.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();

        } else if (resultCode == RESULT_FIRST_USER) {
            startTimer();
            workout.onResume(workout);

            onPauseStateChanged(workout.isPaused());
            onRunDataUpdated();

        } else {
            if (BuildConfig.DEBUG) {
                throw new AssertionError();
            }
        }
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

    protected void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
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