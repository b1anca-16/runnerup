package org.runnerup.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import org.runnerup.R;

import java.util.ArrayList;
import java.util.List;

public class ActiveRunFragment extends Fragment {

    private ListView participantsListView;
    private Button pauseButton;
    private Button stopButton;

    private final List<String> participants = new ArrayList<>();
    private boolean isPaused = false;

    public ActiveRunFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.active_run, container, false);

        bindViews(view);
        setupParticipantsList();
        setupButtons();

        return view;
    }

    private void bindViews(@NonNull View view) {
        participantsListView = view.findViewById(R.id.lv_participants);
        pauseButton = view.findViewById(R.id.pause_button);
        stopButton = view.findViewById(R.id.stop_button);
    }

    private void setupParticipantsList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
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
                AppCompatResources.getDrawable(requireContext(), backgroundResId)
        );

        pauseButton.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                iconResId,
                0
        );
    }
}