package org.runnerup.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;

import org.runnerup.R;

import java.util.ArrayList;
import java.util.List;
import androidx.core.view.WindowInsetsCompat;

public class ActiveRunActivity extends AppCompatActivity {

    public static final String EXTRA_RUN_NAME = "run_name"; // ← neu

    private ListView participantsListView;
    private Button pauseButton;
    private Button stopButton;

    private final List<String> participants = new ArrayList<>();
    private boolean isPaused = false;

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
    }

    private void bindViews() {
        participantsListView = findViewById(R.id.lv_participants);
        pauseButton = findViewById(R.id.pause_button);
        stopButton = findViewById(R.id.stop_button);
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
}