package org.runnerup.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import org.runnerup.R;
import org.runnerup.tracker.LiveChallenge;
import org.runnerup.util.Formatter;
import org.runnerup.workout.Scope;

import java.util.ArrayList;
import java.util.List;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

public class LiveRunActivity extends BaseRunActivity {

    public static final String EXTRA_RUN_NAME = "run_name";

    public class Participant {
        public String name;
        public double km;
        public boolean finished;
        public int place;

        public Participant(String name, double km, boolean finished, int place) {
            this.name = name;
            this.km = km;
            this.finished = finished;
            this.place = place;
        }
    }

    private Button pauseButton;
    private Button stopButton;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private boolean sendingProgress = false;
    private boolean isPaused = false;
    private RecyclerView participantsRecyclerView;
    private ParticipantAdapterRun adapter;
    private final List<Participant> participants = new ArrayList<>();
    private double targetKm = 5.0;
    private TextView activityTime;
    private TextView activityDistance;
    private TextView activityPace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live_run);
        formatter = new Formatter(this);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content), (v, insets) -> {
                    int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(
                            v.getPaddingLeft(),
                            top,
                            v.getPaddingRight(),
                            v.getPaddingBottom()
                    );
                    return insets;
                });

        String runName = getIntent().getStringExtra(EXTRA_RUN_NAME);
        if (getSupportActionBar() != null && runName != null) {
            getSupportActionBar().setTitle(runName);
        }

        bindViews();
        setupParticipantsList();
        setupButtons();
        setupLiveCallbacks();
        initRunSession();
        Log.d("LiveRun", "tracker=" + mTracker);
        Log.d("LiveRun", "workout=" + workout);
        startProgressSending();
    }

    @Override
    protected void onWorkoutReady() {
        Log.d("LiveRun", "✅ onWorkoutReady fired, workout=" + workout);
        updateRunStats();
        updatePauseButton(!workout.isPaused());
    }

    @Override
    protected void onRunDataUpdated() {
        Log.d("LiveRun", "onRunDataUpdated called");
        updateRunStats();
    }

    @Override
    protected void onPauseStateChanged(boolean paused) {
        updatePauseButton(!paused);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sendingProgress = false;
        progressHandler.removeCallbacksAndMessages(null);

        LiveChallenge.getInstance().setParticipantCallback(null);
        LiveChallenge.getInstance().setRunStartedListener(null);
    }

    private void bindViews() {
        participantsRecyclerView = findViewById(R.id.rv_participants);
        pauseButton  = findViewById(R.id.pause_button);
        stopButton   = findViewById(R.id.stop_button);
        activityTime     = findViewById(R.id.run_activity_time);
        activityDistance = findViewById(R.id.intervall_distance);
        activityPace     = findViewById(R.id.interval_pace);
    }

    private void setupParticipantsList() {
        adapter = new ParticipantAdapterRun(participants);
        participantsRecyclerView.setAdapter(adapter);
        participantsRecyclerView.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this)
        );
    }

    private void setupButtons() {
        pauseButton.setOnClickListener(v -> togglePauseState());
        stopButton.setOnClickListener(v -> stopCurrentRun());
    }

    // updatePauseButton: Parameter ändern (running statt isPaused-Toggle)
    private void updatePauseButton(boolean running) {
        if (running) {
            setPauseButtonState(
                    org.runnerup.common.R.string.Pause,
                    R.drawable.btn_blue,
                    org.runnerup.common.R.drawable.ic_av_pause
            );
        } else {
            setPauseButtonState(
                    org.runnerup.common.R.string.Resume,
                    R.drawable.btn_green,
                    org.runnerup.common.R.drawable.ic_av_play_arrow
            );
        }
    }

    private void setupLiveCallbacks() {
        LiveChallenge.getInstance().setLeaderboardListener(json -> {
            runOnUiThread(() -> {
                List<Participant> newList = parseLeaderboard(json);
                updateParticipantsDiff(newList);
            });
        });

        LiveChallenge.getInstance().setRunStartedListener(() -> {
            runOnUiThread(() -> Log.d("ActiveRun", "Run started"));
        });
    }

    private double fakeCurrentKm = 0.0;

    private void startProgressSending() {
        sendingProgress = true;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sendingProgress) return;

                double delta = 0.5 + Math.random() * 0.5;
                fakeCurrentKm += delta;

                // Nicht mehr senden wenn Ziel erreicht
                if (fakeCurrentKm >= targetKm) {
                    fakeCurrentKm = targetKm; // exakt auf Ziel setzen
                    LiveChallenge.getInstance().sendUpdate(fakeCurrentKm);
                    Log.d("ActiveRun", "🏁 Finished at " + fakeCurrentKm + " km – stopping updates");
                    sendingProgress = false; // kein weiterer postDelayed
                    return;
                }

                LiveChallenge.getInstance().sendUpdate(fakeCurrentKm);
                Log.d("ActiveRun", "📡 Progress sent: " + fakeCurrentKm + " km");

                progressHandler.postDelayed(this, 10_000);
            }
        };

        progressHandler.post(progressRunnable);
    }

    private void setPauseButtonState(int textResId, int backgroundResId, int iconResId) {
        pauseButton.setText(textResId);
        ViewCompat.setBackground(pauseButton, AppCompatResources.getDrawable(this, backgroundResId));
        pauseButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconResId, 0);
    }

    private List<Participant> parseLeaderboard(String json) {
        List<Participant> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray array = root.getJSONArray("list");

            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String name      = obj.getString("name");
                double km        = obj.getDouble("km");
                boolean finished = obj.optBoolean("finished", false);
                int place        = obj.optInt("place", 0);
                result.add(new Participant(name, km, finished, place));
            }
        } catch (org.json.JSONException e) {
            Log.e("ActiveRun", "parseLeaderboard failed: " + e.getMessage());
        }
        return result;
    }

    private void updateParticipantsDiff(List<Participant> newList) {
        // Serverreihenfolge übernehmen: per Name suchen, nicht per Position
        for (int i = 0; i < newList.size(); i++) {
            Participant incoming = newList.get(i);

            int existingIndex = -1;
            for (int j = 0; j < participants.size(); j++) {
                if (participants.get(j).name.equals(incoming.name)) {
                    existingIndex = j;
                    break;
                }
            }

            if (existingIndex == -1) {
                // Neuer Teilnehmer
                participants.add(i, incoming);
                adapter.notifyItemInserted(i);
            } else {
                Participant existing = participants.get(existingIndex);

                // Position hat sich geändert → Item verschieben
                if (existingIndex != i) {
                    participants.remove(existingIndex);
                    participants.add(i, existing);
                    adapter.notifyItemMoved(existingIndex, i);
                }

                // Werte aktualisieren (auch wenn finished)
                boolean changed = existing.finished != incoming.finished
                        || existing.km != incoming.km
                        || existing.place != incoming.place;

                if (changed) {
                    existing.km       = incoming.km;
                    existing.finished = incoming.finished;
                    existing.place    = incoming.place;
                    adapter.notifyItemChanged(i, "km_update");
                }
            }
        }

        // Überschüssige Einträge entfernen
        while (participants.size() > newList.size()) {
            int last = participants.size() - 1;
            participants.remove(last);
            adapter.notifyItemRemoved(last);
        }
    }

    private void updateRunStats() {
        Log.d("LiveRun", "updateRunStats called");

        if (workout == null) {
            Log.d("LiveRun", "workout == null");
            return;
        }

        if (mTracker == null) {
            Log.d("LiveRun", "mTracker == null");
            return;
        }

        if (formatter == null) {
            Log.d("LiveRun", "formatter == null");
            return;
        }

        double time     = workout.getTime(Scope.ACTIVITY);
        double distance = workout.getDistance(Scope.ACTIVITY);
        double pace     = workout.getSpeed(Scope.ACTIVITY);

        Log.d("LiveRun", "TIME = " + time);
        Log.d("LiveRun", "DIST = " + distance);
        Log.d("LiveRun", "PACE = " + pace);

        activityTime.setText(
                formatter.formatElapsedTime(
                        Formatter.Format.TXT_SHORT,
                        Math.round(time)
                )
        );

        activityDistance.setText(
                formatter.formatDistance(
                        Formatter.Format.TXT_SHORT,
                        Math.round(distance)
                )
        );

        activityPace.setText(
                formatter.formatVelocityByPreferredUnit(
                        Formatter.Format.TXT_SHORT,
                        pace
                )
        );
    }
}