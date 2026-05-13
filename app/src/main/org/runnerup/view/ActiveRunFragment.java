package org.runnerup.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import org.runnerup.R;

import java.util.ArrayList;
import java.util.List;

public class ActiveRunFragment extends Fragment {
    private ListView lvParticipants;
    private Button pauseButton;
    private Button stopButton;

    private final List<String> participants = new ArrayList<>();

    public ActiveRunFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.active_run, container, false);

        lvParticipants = view.findViewById(R.id.lv_participants);
        pauseButton = view.findViewById(R.id.pause_button);
        stopButton = view.findViewById(R.id.stop_button);

        setupParticipants();
        setupButtons();

        return view;
    }

    private void setupParticipants() {

        ArrayAdapter<String> participantAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                participants
        );

        lvParticipants.setAdapter(participantAdapter);
    }

    private final View.OnClickListener pauseButtonClick =
            v -> {
                if (workout == null) {
                    // "should not happen"
                    return;
                }

                if (workout.isPaused()) {
                    workout.onResume(workout);
                } else {
                    workout.onPause(workout);
                }
                setPauseButtonEnabled(!workout.isPaused());
            };

    private void setPauseButtonEnabled(boolean enabled) {
        if (enabled) {
            pauseButton.setText(org.runnerup.common.R.string.Pause);
            ViewCompat.setBackground(
                    pauseButton, AppCompatResources.getDrawable(this, R.drawable.btn_blue));
            pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, org.runnerup.common.R.drawable.ic_av_pause, 0);
        } else {
            pauseButton.setText(org.runnerup.common.R.string.Resume);
            ViewCompat.setBackground(
                    pauseButton, AppCompatResources.getDrawable(this, R.drawable.btn_green));
            pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, org.runnerup.common.R.drawable.ic_av_play_arrow, 0);
        }
    }
}