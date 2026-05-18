package org.runnerup.view;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.runnerup.R;
import org.runnerup.util.Formatter;
import org.runnerup.workout.Scope;

import java.util.ArrayList;
import java.util.List;

public class ActiveRunActivity extends BaseRunActivity {

    public static final String EXTRA_RUN_NAME = "run_name";

    private ListView participantsListView;
    private Button pauseButton;
    private Button stopButton;

    private TextView activityTime;
    private TextView activityDistance;
    private TextView activityPace;

    private final List<String> participants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_run);

        formatter = new Formatter(this);

        applyWindowInsets();
        setupActionBarTitle();

        bindViews();
        setupParticipantsList();
        setupButtons();

        initRunSession();
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (view, insets) -> {
                    int topInset =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;

                    view.setPadding(
                            view.getPaddingLeft(),
                            topInset,
                            view.getPaddingRight(),
                            view.getPaddingBottom()
                    );

                    return insets;
                }
        );
    }

    private void setupActionBarTitle() {
        String runName = getIntent().getStringExtra(EXTRA_RUN_NAME);

        if (getSupportActionBar() != null && runName != null) {
            getSupportActionBar().setTitle(runName);
        }
    }

    private void bindViews() {
        participantsListView = findViewById(R.id.lv_participants);

        pauseButton = findViewById(R.id.pause_button);
        stopButton = findViewById(R.id.stop_button);

        activityTime = findViewById(R.id.run_activity_time);
        activityDistance = findViewById(R.id.run_activity_distance);
        activityPace = findViewById(R.id.run_activity_pace);
    }

    private void setupParticipantsList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                participants
        );

        participantsListView.setAdapter(adapter);
    }

    private void setupButtons() {
        pauseButton.setOnClickListener(v -> togglePauseState());
        stopButton.setOnClickListener(v -> stopCurrentRun());
    }

    @Override
    protected void onRunDataUpdated() {
        updateRunStats();
    }

    @Override
    protected void onPauseStateChanged(boolean paused) {
        updatePauseButton(!paused);
    }

    private void updateRunStats() {
        if (workout == null || formatter == null) {
            return;
        }

        double time = workout.getTime(Scope.ACTIVITY);
        double distance = workout.getDistance(Scope.ACTIVITY);
        double pace = workout.getSpeed(Scope.ACTIVITY);

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

    private void updatePauseButton(boolean running) {
        if (running) {
            pauseButton.setText(org.runnerup.common.R.string.Pause);

            ViewCompat.setBackground(
                    pauseButton,
                    AppCompatResources.getDrawable(this, R.drawable.btn_blue)
            );

            pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    org.runnerup.common.R.drawable.ic_av_pause,
                    0
            );
        } else {
            pauseButton.setText(org.runnerup.common.R.string.Resume);

            ViewCompat.setBackground(
                    pauseButton,
                    AppCompatResources.getDrawable(this, R.drawable.btn_green)
            );

            pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    org.runnerup.common.R.drawable.ic_av_play_arrow,
                    0
            );
        }
    }
}