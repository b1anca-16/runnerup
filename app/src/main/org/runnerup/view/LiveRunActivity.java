package org.runnerup.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import android.widget.ImageView;
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

    private Button stopButton;
    private Button leaveButton;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private boolean sendingProgress = false;
    private RecyclerView participantsRecyclerView;
    private ParticipantAdapterRun adapter;
    private final List<Participant> participants = new ArrayList<>();
    private double targetKm = 5.0;
    private TextView activityTime;
    private TextView activityDistance;
    private TextView activityPace;
    private String ownName;
    private ImageView finishedFlag;
    private double fakeCurrentKm = 0.0;

    private boolean isOwnRunFinished() {
        for (Participant p : participants) {
            if (ownName != null && ownName.equals(p.name)) {
                return p.finished;
            }
        }
        return false;
    }

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
        ownName = getIntent().getStringExtra("PLAYER_NAME");
        targetKm = getIntent().getFloatExtra("DISTANCE", 5.0f);

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
    }

    @Override
    protected void onRunDataUpdated() {
        Log.d("LiveRun", "onRunDataUpdated called");
        updateRunStats();
    }

    //das nach dem Testen zum Senden der echten Daten rein kommentieren!!
    //@Override
    //protected void onRunTick() {
    //    if (workout == null) return;
    //    double realDistance = workout.getDistance(Scope.ACTIVITY);
    //    LiveChallenge.getInstance().sendUpdate(realDistance);
    //}

    @Override
    protected void onPauseStateChanged(boolean paused) {
    }

    @Override
    protected void stopCurrentRun() {
        LiveChallenge.getInstance().setLeaderboardListener(null);
        LiveChallenge.getInstance().setRunStartedListener(null);
        LiveChallenge.getInstance().setParticipantCallback(null);

        if (isOwnRunFinished()) {
            LiveChallenge.getInstance().sendLeave();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                launchDetailActivity();
            }, 300);
        } else {
            LiveChallenge.getInstance().disconnect();
            launchDetailActivity();
        }

        if (timer == null || workout == null || mTracker == null) return;

        workout.onStop(workout);
        stopTimer();

        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("mode", "save");
        intent.putExtra("ID", mTracker.getActivityId());
        intent.putExtra("no_resume", true);
        detailActivityLauncher.launch(intent);
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
        stopButton       = findViewById(R.id.stop_button);
        leaveButton      = findViewById(R.id.leave_button);
        activityTime     = findViewById(R.id.run_activity_time);
        activityDistance = findViewById(R.id.intervall_distance);
        activityPace     = findViewById(R.id.interval_pace);
        finishedFlag     = findViewById(R.id.iv_finished_flag);
    }

    private void setupParticipantsList() {
        adapter = new ParticipantAdapterRun(participants);
        participantsRecyclerView.setAdapter(adapter);
        participantsRecyclerView.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this)
        );
    }

    private void setupButtons() {
        stopButton.setOnClickListener(v -> stopCurrentRun());
        leaveButton.setOnClickListener(v -> stopCurrentRun());
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

    private void startProgressSending() {
        sendingProgress = true;

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (!sendingProgress) return;

                double delta = 0.5 + Math.random() * 0.5;
                fakeCurrentKm += delta;

                if (fakeCurrentKm >= targetKm) {
                    fakeCurrentKm = targetKm;
                    LiveChallenge.getInstance().sendUpdate(fakeCurrentKm);
                    runOnUiThread(() -> {
                        updateFakeUI(fakeCurrentKm);
                        // Finish lokal auslösen – unabhängig vom Server
                        showFinishState();
                    });
                    sendingProgress = false;
                    return;
                }

                LiveChallenge.getInstance().sendUpdate(fakeCurrentKm);
                Log.d("ActiveRun", "📡 Progress sent: " + fakeCurrentKm + " km");

                // UI direkt mit Fake-Km updaten – gleiche Quelle wie Leaderboard
                runOnUiThread(() -> updateFakeUI(fakeCurrentKm));

                progressHandler.postDelayed(this, 10_000);
            }
        };

        progressHandler.post(progressRunnable);
    }

    /**
     * Aktualisiert Distance-Anzeige und Progressbar mit den Fake-Km.
     * Gleiche Logik wie das Live-Ranking – direkt aus fakeCurrentKm.
     */
    private void updateFakeUI(double km) {
        // Distance-Karte updaten
        if (activityDistance != null) {
            activityDistance.setText(String.format(java.util.Locale.US, "%.2f km", km));
        }

        // Progressbar updaten
        View fill = findViewById(R.id.v_progress_fill);
        if (fill == null) return;

        View track = (View) fill.getParent();
        if (track == null) return;

        int trackWidth = track.getWidth();
        if (trackWidth == 0) {
            // Layout noch nicht gezeichnet → nächsten Frame abwarten
            track.post(() -> updateFakeUI(km));
            return;
        }

        float ratio = (float) Math.min(km / targetKm, 1.0);
        android.view.ViewGroup.LayoutParams lp = fill.getLayoutParams();
        lp.width = (int) (trackWidth * ratio);
        fill.setLayoutParams(lp);

        int percent = (int) (ratio * 100);

        TextView tvLabel     = findViewById(R.id.tv_progress_label);
        TextView tvPercent   = findViewById(R.id.tv_progress_percent);
        TextView tvRemaining = findViewById(R.id.tv_progress_remaining);

        if (tvLabel != null)
            tvLabel.setText(String.format(java.util.Locale.US,
                    "%.2f / %.1f km", km, targetKm));
        if (tvPercent != null)
            tvPercent.setText(String.format(java.util.Locale.US, "%d%%", percent));
        if (tvRemaining != null)
            tvRemaining.setText(String.format(java.util.Locale.US,
                    "%.2f km left", Math.max(targetKm - km, 0)));
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

    private void launchDetailActivity() {
        if (timer == null || workout == null || mTracker == null) return;
        workout.onStop(workout);
        stopTimer();
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("mode", "save");
        intent.putExtra("ID", mTracker.getActivityId());
        intent.putExtra("no_resume", true);
        detailActivityLauncher.launch(intent);
    }

    private void updateParticipantsDiff(List<Participant> newList) {
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
                participants.add(i, incoming);
                adapter.notifyItemInserted(i);
            } else {
                Participant existing = participants.get(existingIndex);

                if (existingIndex != i) {
                    participants.remove(existingIndex);
                    participants.add(i, existing);
                    adapter.notifyItemMoved(existingIndex, i);
                }

                boolean changed = existing.finished != incoming.finished
                        || existing.km != incoming.km
                        || existing.place != incoming.place;

                if (changed) {
                    existing.km       = incoming.km;
                    existing.finished = incoming.finished;
                    existing.place    = incoming.place;
                    adapter.notifyItemChanged(i, "km_update");
                    if (existing.finished
                            && ownName != null
                            && existing.name.equals(ownName)
                            && workout != null
                            && !workout.isPaused()) {
                        togglePauseState();
                        stopButton.setVisibility(View.GONE);
                        leaveButton.setVisibility(View.VISIBLE);
                        finishedFlag.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        while (participants.size() > newList.size()) {
            int last = participants.size() - 1;
            participants.remove(last);
            adapter.notifyItemRemoved(last);
        }
    }

    private void updateRunStats() {
        Log.d("LiveRun", "updateRunStats called");

        if (workout == null) { Log.d("LiveRun", "workout == null"); return; }
        if (mTracker == null) { Log.d("LiveRun", "mTracker == null"); return; }
        if (formatter == null) { Log.d("LiveRun", "formatter == null"); return; }

        double time     = workout.getTime(Scope.ACTIVITY);
        double distance = workout.getDistance(Scope.ACTIVITY);
        double pace     = workout.getSpeed(Scope.ACTIVITY);

        activityTime.setText(
                formatter.formatElapsedTime(Formatter.Format.TXT_SHORT, Math.round(time)));

        // Distance und Progressbar nur mit echten GPS-Daten überschreiben
        // im Test-Modus (distance == 0) macht updateFakeUI() das stattdessen
        if (distance > 0) {
            activityDistance.setText(
                    formatter.formatDistance(Formatter.Format.TXT_SHORT, Math.round(distance)));
            double coveredKm = distance / 1000.0;
            View fill = findViewById(R.id.v_progress_fill);
            if (fill != null) fill.post(() -> updateFakeUI(coveredKm));
        }

        activityPace.setText(
                formatter.formatVelocityByPreferredUnit(Formatter.Format.TXT_SHORT, pace));
    }

    private void showFinishState() {

        View finishBlock = findViewById(R.id.layout_finish_block);
        if (finishBlock != null) finishBlock.setVisibility(View.VISIBLE);

        // Eigenen Eintrag zuerst als finished markieren
        for (int i = 0; i < participants.size(); i++) {
            if (ownName != null && ownName.equals(participants.get(i).name)) {
                participants.get(i).finished = true;
                adapter.notifyItemChanged(i);
                break;
            }
        }

        // Danach zählen
        int finishedCount = 0;
        int runningCount = 0;

        for (Participant p : participants) {
            if (p.finished) {
                finishedCount++;
            } else {
                runningCount++;
            }
        }

        TextView tvFinished = findViewById(R.id.tv_finished_count);
        TextView tvRunning = findViewById(R.id.tv_running_count);

        if (tvFinished != null) tvFinished.setText(String.valueOf(finishedCount));
        if (tvRunning != null) tvRunning.setText(String.valueOf(runningCount));

        // Buttons umschalten
        if (workout != null && !workout.isPaused()) togglePauseState();

        stopButton.setVisibility(View.GONE);
        leaveButton.setVisibility(View.VISIBLE);
    }
}
