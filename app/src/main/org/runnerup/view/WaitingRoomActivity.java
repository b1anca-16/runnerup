package org.runnerup.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import org.runnerup.R;
import org.runnerup.common.tracker.TrackerState;
import org.runnerup.common.util.ValueModel;
import org.runnerup.tracker.LiveChallenge;
import org.runnerup.tracker.Tracker;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

import java.util.ArrayList;

public class WaitingRoomActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN       = "token";
    public static final String EXTRA_RUN_NAME    = "run_name";
    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_DISTANCE    = "DISTANCE";
    private static final String EXTRA_ROLE       = "ROLE";

    private static final String TAG = "WaitingRoomActivity";

    private final ArrayList<String> participants = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private Tracker mTracker;
    private boolean mTrackerBound = false;
    private boolean startRequested = false;

    private String runName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_waiting_room);

        applyWindowInsets();

        String role = getIntent().getStringExtra(EXTRA_ROLE);
        String token = getIntent().getStringExtra(EXTRA_TOKEN);
        runName = getIntent().getStringExtra(EXTRA_RUN_NAME);
        float distance = getIntent().getFloatExtra(EXTRA_DISTANCE, 5f);

        setupHeader(role, runName);
        setupRoomInfo(token, distance);
        setupParticipantsList();
        setupLiveChallengeCallback();
        setupStartButton(role);

        bindTracker();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.waiting_room_root),
                (view, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

                    view.setPadding(
                            view.getPaddingLeft(),
                            top,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return insets;
                }
        );
    }

    private void setupHeader(String role, String runName) {
        Button startButton = findViewById(R.id.btn_start_run);
        TextView statusText = findViewById(R.id.tv_status);

        boolean isHost = "HOST".equals(role);

        startButton.setVisibility(isHost ? View.VISIBLE : View.GONE);

        if (isHost) {
            statusText.setText(R.string.waiting_participants);
        } else {
            statusText.setText(R.string.wait_host);
        }

        if (getSupportActionBar() != null && runName != null) {
            getSupportActionBar().setTitle(runName);
        }
    }

    private void setupRoomInfo(String token, float distance) {
        TextView tokenView = findViewById(R.id.tv_token);
        TextView distanceView = findViewById(R.id.tv_distance);

        tokenView.setText(getString(R.string.code_placeholder, token));
        distanceView.setText(getString(R.string.distance_placeholder, distance));
    }

    private void setupParticipantsList() {
        adapter = new ArrayAdapter<>(
                this,
                R.layout.item_participant,
                participants
        );

        ListView participantsList = findViewById(R.id.lv_participants);
        participantsList.setAdapter(adapter);
    }

    private void setupLiveChallengeCallback() {
        LiveChallenge.getInstance().setParticipantCallback(names -> {
            participants.clear();
            participants.addAll(names);
            adapter.notifyDataSetChanged();
        });
    }

    private void setupStartButton(String role) {
        Button startButton = findViewById(R.id.btn_start_run);

        startButton.setOnClickListener(v -> {
            if (!"HOST".equals(role)) {
                return;
            }

            requestCommunityRunStart();
        });
    }

    private void requestCommunityRunStart() {
        if (mTracker == null) {
            showToast("Tracker wird noch vorbereitet.");
            return;
        }

        startRequested = true;

        TrackerState state = mTracker.getState();

        if (state == TrackerState.CONNECTED) {
            startCommunityRun();
            return;
        }

        if (state == TrackerState.CONNECTING) {
            showToast("GPS/Tracker wird verbunden...");
            return;
        }

        Log.d(TAG, "Tracker state before connect: " + state);
        showToast("GPS/Tracker wird gestartet...");

        mTracker.connect();
    }

    private void startCommunityRun() {
        if (!startRequested || mTracker == null) {
            return;
        }

        if (mTracker.getState() != TrackerState.CONNECTED) {
            return;
        }

        Workout workout = prepareCommunityWorkout();

        mTracker.setWorkout(workout);
        mTracker.start();

        Intent intent = new Intent(this, ActiveRunActivity.class);
        intent.putExtra(ActiveRunActivity.EXTRA_RUN_NAME, runName);

        startActivity(intent);
        finish();
    }

    private Workout prepareCommunityWorkout() {
        Context context = getApplicationContext();
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        // Basic Workout ohne Pace-/HR-Ziel.
        Workout workout =
                WorkoutBuilder.createDefaultWorkout(
                        getResources(),
                        prefs,
                        null
                );

        WorkoutBuilder.prepareWorkout(
                getResources(),
                prefs,
                workout
        );

        SharedPreferences audioPrefs =
                WorkoutBuilder.getAudioCuePreferences(
                        context,
                        prefs,
                        getString(R.string.pref_basic_audio)
                );

        WorkoutBuilder.addAudioCuesToWorkout(
                getResources(),
                workout,
                audioPrefs,
                prefs
        );

        return workout;
    }

    private final ValueModel.ChangeListener<TrackerState> trackerStateListener =
            (instance, oldValue, newValue) -> {
                Log.d(TAG, "Tracker state changed: " + oldValue + " -> " + newValue);

                if (newValue == TrackerState.CONNECTED && startRequested) {
                    runOnUiThread(this::startCommunityRun);
                }
            };

    private final ServiceConnection trackerConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mTracker = ((Tracker.LocalBinder) service).getService();
                    mTrackerBound = true;

                    mTracker.registerTrackerStateListener(trackerStateListener);

                    Log.d(TAG, "Tracker service bound, state=" + mTracker.getState());
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mTrackerBound = false;
                    mTracker = null;
                }
            };

    private void bindTracker() {
        bindService(
                new Intent(this, Tracker.class),
                trackerConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    private void unbindTracker() {
        if (mTracker != null) {
            mTracker.unregisterTrackerStateListener(trackerStateListener);
        }

        if (mTrackerBound) {
            unbindService(trackerConnection);
            mTrackerBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindTracker();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}