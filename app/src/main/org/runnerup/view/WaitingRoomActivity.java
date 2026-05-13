package org.runnerup.view;

import android.os.Bundle;
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

    private ArrayList<String> participants = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_waiting_room);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.waiting_room_root), (v, insets) -> {
                    int top = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top;
                    v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
                    return insets;
                });

        String token      = getIntent().getStringExtra(EXTRA_TOKEN);
        String runName    = getIntent().getStringExtra(EXTRA_RUN_NAME);
        String playerName = getIntent().getStringExtra(EXTRA_PLAYER_NAME);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(runName);
        }

        ((TextView) findViewById(R.id.tv_token)).setText(getString(R.string.code_placeholder, token));

        participants.add(playerName + " (Host)");
        adapter = new ArrayAdapter<>(this,
                R.layout.item_participant, participants);
        ((ListView) findViewById(R.id.lv_participants)).setAdapter(adapter);

        ((Button) findViewById(R.id.btn_start_run)).setOnClickListener(v -> {
            // TODO: Lauf starten
            finish();
        });
    }
}