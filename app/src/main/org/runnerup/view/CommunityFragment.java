/**
 * Fragment (=Baustein innerhalb eines Screens) für den Community-Tab.
 * Zeigt Buttons zum Erstellen und Beitreten eines Live-Laufs an
 * und öffnet den entsprechenden Dialog zur Eingabe von Name und Raumcode.
 */

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

        createButton.setOnClickListener(v -> {
            Log.d(TAG, "🖱️ createButton geklickt");

            CreateRunDialogFragment dialog = new CreateRunDialogFragment();
            Log.d(TAG, "Dialog-Objekt erstellt");

            dialog.setOnRunCreated((playerName, runName) -> {
                Log.d(TAG, "✅ onRunCreated() ausgelöst – playerName=" + playerName + ", runName=" + runName);

                try {
                    LiveChallenge challenge = new LiveChallenge();
                    Log.d(TAG, "LiveChallenge-Objekt erstellt");

                    challenge.connect("ws://10.0.2.2:8080", new LiveChallenge.OnTokenReceived() {
                        @Override
                        public void onToken(String token) {
                            Log.d(TAG, "✅ onToken() empfangen: " + token);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Log.d(TAG, "Toast wird angezeigt für Token: " + token);
                                String msg = "Lauf \"" + runName + "\" erstellt!\nCode: " + token;
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                            });
                        }

                        @Override
                        public void onPartnerJoined() {
                            Log.d(TAG, "✅ onPartnerJoined() ausgelöst");
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Partner ist beigetreten! 🏃", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "❌ onError() ausgelöst: " + message);
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), "Fehler: " + message, Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                    Log.d(TAG, "challenge.connect() aufgerufen");

                    challenge.createRoom();
                    Log.d(TAG, "challenge.createRoom() aufgerufen");

                } catch (Exception e) {
                    Log.e(TAG, "💥 Exception in onRunCreated: " + e.getMessage(), e);
                }
            });

            Log.d(TAG, "Zeige Dialog...");
            try {
                dialog.show(getParentFragmentManager(), "create_run_dialog");
                Log.d(TAG, "✅ Dialog.show() erfolgreich");
            } catch (Exception e) {
                Log.e(TAG, "💥 Exception bei dialog.show(): " + e.getMessage(), e);
            }
        });

        joinButton.setOnClickListener(v -> {
            Log.d(TAG, "🖱️ joinButton geklickt");
            Toast.makeText(getContext(), "Lauf beitreten...", Toast.LENGTH_SHORT).show();
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