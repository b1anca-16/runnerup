/**
 * Activity, die nach dem Erstellen oder Beitreten eines Laufs angezeigt wird.
 * Zeigt den Raumcode und die aktuelle Teilnehmerliste an,
 * die automatisch via WebSocket aktualisiert wird.
 */

package org.runnerup.view;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import org.runnerup.R;
import java.util.ArrayList;

public class WaitingRoomActivity extends AppCompatActivity {

    public static final String EXTRA_TOKEN       = "token";
    public static final String EXTRA_RUN_NAME    = "run_name";
    public static final String EXTRA_PLAYER_NAME = "player_name";
    public static final String EXTRA_DISTANCE = "DISTANCE";

    private ArrayList<String> participants = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private static final String EXTRA_ROLE = "ROLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_waiting_room);
        String role = getIntent().getStringExtra(EXTRA_ROLE);

        Button startButton = findViewById(R.id.btn_start_run);
        TextView statusText = findViewById(R.id.tv_status);
        boolean isHost = "HOST".equals(role);
        startButton.setVisibility(isHost ? View.VISIBLE : View.GONE);
        if (isHost) {
            statusText.setText(R.string.waiting_participants);
        } else {
            statusText.setText(R.string.wait_host);
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.waiting_room_root), (v, insets) -> {
                    int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        String token      = getIntent().getStringExtra(EXTRA_TOKEN);
        String runName    = getIntent().getStringExtra(EXTRA_RUN_NAME);
        float distance = getIntent().getFloatExtra(EXTRA_DISTANCE, 5f);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(runName);
        }

        ((TextView) findViewById(R.id.tv_token)).setText(getString(R.string.code_placeholder, token));
        ((TextView) findViewById(R.id.tv_distance)).setText(getString(R.string.distance_placeholder, distance));

        ((ListView) findViewById(R.id.lv_participants)).setAdapter(adapter);

        // 2. Callback registrieren ← HIER
        org.runnerup.tracker.LiveChallenge.getInstance().setParticipantCallback(names -> {
            participants.clear();
            participants.addAll(names);
            adapter.notifyDataSetChanged();
        });

        ((Button) findViewById(R.id.btn_start_run)).setOnClickListener(v -> {
            // TODO: Lauf starten
            finish();
        });
    }
}