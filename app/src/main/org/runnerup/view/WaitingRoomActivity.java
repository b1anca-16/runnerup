/**
 * Activity, die nach dem Erstellen oder Beitreten eines Laufs angezeigt wird.
 * Zeigt den Raumcode und die aktuelle Teilnehmerliste an,
 * die automatisch via WebSocket aktualisiert wird.
 */

package org.runnerup.view;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import androidx.preference.PreferenceManager;

import org.runnerup.R;
import org.runnerup.common.tracker.TrackerState;
import org.runnerup.common.util.ValueModel;
import org.runnerup.tracker.LiveChallenge;
import org.runnerup.tracker.Tracker;
import org.runnerup.workout.Dimension;
import org.runnerup.workout.Workout;
import org.runnerup.workout.WorkoutBuilder;

import java.util.ArrayList;

public class WaitingRoomActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN       = "token";
    public static final String EXTRA_RUN_NAME    = "run_name";
    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_DISTANCE = "DISTANCE";

    private ArrayList<String> participants = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private static final String EXTRA_ROLE = "ROLE";
    private Tracker mTracker = null;
    private boolean mIsBound = false;
    private org.runnerup.tracker.GpsStatus mGpsStatus = null;
    private boolean runStartRequested = false;
    private String runName;
    private String playerName;
    private Button startButton;
    private float distance = 5.0f;
    private CommunityAudioService communityAudioService;


    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTracker = ((Tracker.LocalBinder) service).getService();
            mTracker.registerTrackerStateListener(trackerStateListener);
            Log.d("WaitingRoom", "GPS enabled: " +
                    ((android.location.LocationManager) getSystemService(LOCATION_SERVICE))
                            .isProviderEnabled(android.location.LocationManager.GPS_PROVIDER));

            switch (mTracker.getState()) {
                case INIT:
                case CLEANUP:
                    mTracker.setup();
                    break;
                case INITIALIZED:
                    mTracker.connect();
                    break;
                case CONNECTING:
                case CONNECTED:
                case STARTED:
                    break;
                default:
                    mTracker.setup();
                    break;
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTracker = null;
        }
    };

    private void tryStartRun() {
        Log.d("WaitingRoom", "tryStartRun: requested=" + runStartRequested
                + " tracker=" + mTracker
                + " state=" + (mTracker != null ? mTracker.getState() : "null"));
        if (!runStartRequested) return;
        if (mTracker == null) return;
        if (mTracker.getState() != TrackerState.CONNECTED) return;

        Workout w = WorkoutBuilder.createDefaultWorkout(
                getResources(),
                PreferenceManager.getDefaultSharedPreferences(this),
                Dimension.DISTANCE
        );
        WorkoutBuilder.prepareWorkout(getResources(),
                PreferenceManager.getDefaultSharedPreferences(this), w);
        mTracker.setWorkout(w);

        Intent intent = new Intent(this, CountdownActivity.class);
        intent.putExtra(CountdownActivity.EXTRA_RUN_NAME, runName);
        intent.putExtra(CountdownActivity.EXTRA_PLAYER_NAME, playerName);
        intent.putExtra(CountdownActivity.EXTRA_TOKEN, getIntent().getStringExtra(EXTRA_TOKEN));
        intent.putExtra("DISTANCE", distance);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_waiting_room);
        String role = getIntent().getStringExtra(EXTRA_ROLE);
        runName = getIntent().getStringExtra(EXTRA_RUN_NAME);
        playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);

        mIsBound = getApplicationContext().bindService(
                new Intent(this, Tracker.class),
                mConnection,
                Context.BIND_AUTO_CREATE
        );

        startButton = findViewById(R.id.btn_start_run);
        startButton.setEnabled(false);
        startButton.setAlpha(0.4f);
        TextView statusText = findViewById(R.id.tv_status);
        boolean isHost = "HOST".equals(role);
        startButton.setVisibility(isHost ? View.VISIBLE : View.GONE);
        if (isHost) {
            statusText.setText(R.string.waiting_participants);
        } else {
            statusText.setText(R.string.wait_host);
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.waiting_room_root), (v, insets) -> {
                    int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        String token      = getIntent().getStringExtra(EXTRA_TOKEN);
        distance = getIntent().getFloatExtra(EXTRA_DISTANCE, 5f);

        communityAudioService = new CommunityAudioService(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(runName);
        }

        ((TextView) findViewById(R.id.tv_token)).setText(token);
        ((TextView) findViewById(R.id.tv_distance)).setText(String.format(java.util.Locale.US, "%.1f km", distance));

        adapter = new ArrayAdapter<String>(this, R.layout.item_participant, participants) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_participant, parent, false);
                }
                String fullName = getItem(position);

                // Avatar: erster Buchstabe des Namens
                TextView tvAvatar = convertView.findViewById(R.id.tv_avatar);
                TextView tvName   = convertView.findViewById(R.id.tv_participant_name);
                TextView tvBadge  = convertView.findViewById(R.id.tv_host_badge);

                String displayName = fullName;
                boolean isHost = fullName != null && fullName.contains("(Host)");

                if (isHost && fullName != null) {
                    displayName = fullName.replace("(Host)", "").trim();
                }

                tvAvatar.setText(displayName != null && !displayName.isEmpty()
                        ? String.valueOf(displayName.charAt(0)).toUpperCase()
                        : "?");
                tvName.setText(displayName);
                tvBadge.setVisibility(isHost ? View.VISIBLE : View.GONE);

                return convertView;
            }
        };

        ((ListView) findViewById(R.id.lv_participants)).setAdapter(adapter);

        // 2. Callback registrieren ← HIER
        org.runnerup.tracker.LiveChallenge.getInstance().setParticipantCallback(names -> {
            if (names.size() > participants.size()) {
                // Someone joined
                for (String name : names) {
                    if (!participants.contains(name)) {
                        communityAudioService.announceJoin(name);
                    }
                }
            }
            participants.clear();
            participants.addAll(names);
            adapter.notifyDataSetChanged();
        });

        LiveChallenge.getInstance().setRunStartedListener(() -> {
            Log.d("WaitingRoom", "RunStartedListener fired!");
            communityAudioService.announceStart();
            runStartRequested = true;
            runOnUiThread(this::tryStartRun);
        });

        ((Button) findViewById(R.id.btn_start_run)).setOnClickListener(v -> {
            LiveChallenge.getInstance().startRun();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (communityAudioService != null) {
            communityAudioService.stop();
        }
        if (mIsBound) {
            getApplicationContext().unbindService(mConnection);
            mIsBound = false;
        }
        if (mGpsStatus != null) {
            mGpsStatus.stop(null);
        }
        if (mTracker != null) {
            mTracker.unregisterTrackerStateListener(trackerStateListener);
        }
    }

    private void updateGpsStatus(TrackerState state) {
        TextView statusText = findViewById(R.id.tv_status);
        if (state == TrackerState.CONNECTED) {
            statusText.setText(R.string.gps_ready);
        } else {
            statusText.setText(R.string.Waiting_for_GPS);
        }
    }

    private final ValueModel.ChangeListener<TrackerState> trackerStateListener =
            (instance, oldValue, newValue) -> {
                Log.d("WaitingRoom", "Tracker state changed: " + oldValue + " -> " + newValue);

                runOnUiThread(() -> {
                    switch (newValue) {
                        case INITIALIZED:
                            mTracker.connect();
                            break;
                        case INIT:
                        case CLEANUP:
                            Log.d("WaitingRoom", "Tracker reset, calling setup() again");
                            mTracker.setup();
                            break;
                        case CONNECTED:
                            Log.d("WaitingRoom", "✅ GPS CONNECTED!");
                            startButton.setEnabled(true);
                            startButton.setAlpha(1.0f);
                            updateGpsStatus(newValue);
                            tryStartRun();
                            break;
                        default:
                            updateGpsStatus(newValue);
                            break;
                    }
                });
            };
}