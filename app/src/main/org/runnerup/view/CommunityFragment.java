package org.runnerup.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import org.runnerup.R;
import org.runnerup.tracker.LiveChallenge;
import android.os.Handler;
import android.os.Looper;

public class CommunityFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        Button createButton = view.findViewById(R.id.btn_create_run);
        Button joinButton = view.findViewById(R.id.btn_join_run);

        createButton.setOnClickListener(v -> {
            LiveChallenge challenge = new LiveChallenge();
            challenge.connect("ws://10.0.2.2:8080", new LiveChallenge.OnTokenReceived() {
                @Override
                public void onToken(String token) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(requireContext(), "Dein Code: " + token, Toast.LENGTH_LONG).show();
                        Log.d("LiveChallenge", "Token: " + token);
                    });
                }

                @Override
                public void onPartnerJoined() {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Partner ist beigetreten! 🏃", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String message) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Fehler: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });

            challenge.createRoom();
        });

        joinButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Lauf beitreten...", Toast.LENGTH_SHORT).show();
            // TODO: Token eingeben
        });

        return view;
    }
}