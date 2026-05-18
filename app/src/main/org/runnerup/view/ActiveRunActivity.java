package org.runnerup.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import org.runnerup.R;
import org.runnerup.tracker.LiveChallenge;

import java.util.ArrayList;
import java.util.List;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;

public class ActiveRunActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_NAME = "run_name";
    public class Participant {
        public String name;
        public double km;
        public Participant(String name, double km) {
            this.name = name;
            this.km = km;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_run);

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
        startProgressSending();
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
        pauseButton = findViewById(R.id.pause_button);
        stopButton = findViewById(R.id.stop_button);
    }

    private void setupParticipantsList() {

        adapter = new ParticipantAdapterRun(participants);

        participantsRecyclerView.setAdapter(adapter);
        participantsRecyclerView.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(this)
        );
    }

    private void setupButtons() {
        pauseButton.setOnClickListener(v -> {
            isPaused = !isPaused;
            updatePauseButton();
        });

        updatePauseButton();
    }

    private void updatePauseButton() {
        if (isPaused) {
            setPauseButtonState(
                    org.runnerup.common.R.string.Resume,
                    R.drawable.btn_green,
                    org.runnerup.common.R.drawable.ic_av_play_arrow
            );
        } else {
            setPauseButtonState(
                    org.runnerup.common.R.string.Pause,
                    R.drawable.btn_blue,
                    org.runnerup.common.R.drawable.ic_av_pause
            );
        }
    }

    private void setupLiveCallbacks() {

        LiveChallenge.getInstance().setLeaderboardListener(json -> {
            runOnUiThread(() -> {

                Log.d("DEBUG", "Participants JSON: " + json);
                List<Participant> list = parseLeaderboard(json);
                Log.d("DEBUG", "Parsed participants: " + list.size());

                participants.clear();
                participants.addAll(list);

                adapter.notifyDataSetChanged();
            });
        });

        LiveChallenge.getInstance().setRunStartedListener(() -> {
            runOnUiThread(() -> {
                Log.d("ActiveRun", "Run started");
            });
        });
    }

    private void startProgressSending() {
        sendingProgress = true;

        progressRunnable = new Runnable() {
            @Override
            public void run() {

                if (!sendingProgress) return;

                double fakeKm = Math.random() * 5; // TEST-WERT

                LiveChallenge.getInstance().sendUpdate(fakeKm);

                Log.d("ActiveRun", "📡 Progress sent: " + fakeKm + " km");

                progressHandler.postDelayed(this, 20_000); // 20 Sekunden
            }
        };

        progressHandler.post(progressRunnable);
    }

    private void setPauseButtonState(
            int textResId,
            int backgroundResId,
            int iconResId
    ) {
        pauseButton.setText(textResId);

        ViewCompat.setBackground(
                pauseButton,
                AppCompatResources.getDrawable(this, backgroundResId)
        );

        pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                iconResId,
                0
        );
    }

    private List<Participant> parseLeaderboard(String json) {
        List<Participant> result = new ArrayList<>();
        if (json == null || json.isEmpty()) return result;
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray array = root.getJSONArray("list");

            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                double km   = obj.getDouble("km");
                result.add(new Participant(name, km));
            }
        } catch (org.json.JSONException e) {
            Log.e("ActiveRun", "parseLeaderboard failed: " + e.getMessage());
        }
        return result;
    }
}