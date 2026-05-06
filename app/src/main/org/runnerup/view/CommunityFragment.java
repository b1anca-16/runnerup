package org.runnerup.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import org.runnerup.R;

public class CommunityFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);

        Button createButton = view.findViewById(R.id.btn_create_run);
        Button joinButton = view.findViewById(R.id.btn_join_run);

        createButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Lauf erstellen...", Toast.LENGTH_SHORT).show();
            // TODO: Token generieren + anzeigen
        });

        joinButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Lauf beitreten...", Toast.LENGTH_SHORT).show();
            // TODO: Token eingeben
        });

        return view;
    }
}