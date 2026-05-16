package org.runnerup.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import org.runnerup.R;
import org.runnerup.tracker.LiveChallenge;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        view.findViewById(R.id.btn_create_run).setOnClickListener(v -> showCreateDialog());
        view.findViewById(R.id.btn_join_run).setOnClickListener(v -> showJoinDialog());

        return view;
    }

    private void showCreateDialog() {
        CreateRunDialogFragment dialog = new CreateRunDialogFragment();
        dialog.setOnRunCreated((playerName, runName) -> {
            LiveChallenge.getInstance().connectAndCreate(playerName, new LiveChallenge.OnTokenReceived() {
                @Override
                public void onToken(String token) {
                    openWaitingRoom(token, runName, playerName, "HOST");
                }
                @Override
                public void onPartnerJoined() {
                    showToast("Partner ist beigetreten! 🏃");
                }
                @Override
                public void onError(String message) {
                    Log.e(TAG, "Fehler: " + message);
                    showToast("Fehler: " + message);
                }
            });
        });
        dialog.show(getParentFragmentManager(), "create_run_dialog");
    }

    private void showJoinDialog() {
        CreateRunDialogFragment dialog = new CreateRunDialogFragment();
        dialog.setMode(CreateRunDialogFragment.Mode.JOIN);
        dialog.setOnRunCreated((playerName, code) -> {
            LiveChallenge.getInstance().connectAndJoin(code, playerName, new LiveChallenge.OnTokenReceived() {
                @Override
                public void onToken(String token) {
                    openWaitingRoom(token, null, playerName, "JOIN");
                }
                @Override
                public void onPartnerJoined() {
                    showToast("Partner ist beigetreten! 🏃");
                }
                @Override
                public void onError(String message) {
                    showToast("Fehler: " + message);
                }
            });
        });
        dialog.show(getParentFragmentManager(), "join_run_dialog");
    }

    private void openWaitingRoom(String token, String runName, String playerName, String role) {
        Intent intent = new Intent(requireContext(), WaitingRoomActivity.class);
        intent.putExtra(WaitingRoomActivity.EXTRA_TOKEN, token);
        intent.putExtra(WaitingRoomActivity.EXTRA_RUN_NAME, runName);
        intent.putExtra(WaitingRoomActivity.EXTRA_PLAYER_NAME, playerName);
        intent.putExtra("ROLE", role);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}