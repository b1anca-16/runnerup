/**
 * Dialog zur Eingabe von Nickname und Lauf-Name (CREATE-Modus)
 * oder Nickname und Raumcode (JOIN-Modus).
 */

package org.runnerup.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import org.runnerup.R;
import android.app.AlertDialog;

public class CreateRunDialogFragment extends DialogFragment {

    public enum Mode { CREATE, JOIN }
    private Mode mode = Mode.CREATE;

    public void setMode(Mode mode) {   // NEU
        this.mode = mode;
    }
    public interface OnRunCreated {
        void onConfirm(String playerName, String value);
    }

    private OnRunCreated listener;

    public void setOnRunCreated(OnRunCreated listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater()
                .inflate(R.layout.dialog_create_run, null);

        EditText etPlayerName = view.findViewById(R.id.et_player_name);
        EditText etRunName    = view.findViewById(R.id.et_run_name);
        Button btnCancel      = view.findViewById(R.id.btn_cancel);
        Button btnConfirm     = view.findViewById(R.id.btn_confirm);

        if (mode == Mode.JOIN) {
            etRunName.setHint("Room Code");
            btnConfirm.setText("Join");
        }

        Dialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String playerName = etPlayerName.getText().toString().trim();
            String value = etRunName.getText().toString().trim();

            if (playerName.isEmpty()) {
                etPlayerName.setError("Enter your Nickname");
                return;
            }
            if (value.isEmpty()) {
                etRunName.setError(mode == Mode.JOIN ? "Enter the room code" : "Enter the run name");
                return;
            }

            if (listener != null) {
                listener.onConfirm(playerName, value);
            }
            dialog.dismiss();
        });

        return dialog;
    }
}