package org.runnerup.view;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.runnerup.R;

import java.util.List;
import java.util.Locale;

public class ParticipantAdapterRun extends RecyclerView.Adapter<ParticipantAdapterRun.ViewHolder> {

    private final List<LiveRunActivity.Participant> data;

    public ParticipantAdapterRun(List<LiveRunActivity.Participant> data) {
        this.data = data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView avatar;
        TextView name;
        TextView km;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.tv_rank_circle);
            name   = itemView.findViewById(R.id.tv_name);
            km     = itemView.findViewById(R.id.tv_km);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant_run, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && "km_update".equals(payloads.get(0))
                && !data.get(position).finished) {
            holder.km.setText(
                    String.format(Locale.getDefault(), "%.2f km", data.get(position).km)
            );
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LiveRunActivity.Participant p = data.get(position);

        // Avatar: erster Buchstabe des Namens
        String displayName = p.name != null ? p.name : "?";
        holder.avatar.setText(
                displayName.isEmpty() ? "?" : String.valueOf(displayName.charAt(0)).toUpperCase()
        );

        holder.name.setText(p.place + ". " + p.name);

        if (p.finished) {
            holder.km.setText("Finished");
            holder.itemView.setBackgroundColor(
                    android.graphics.Color.parseColor("#1A3D1A")
            );
            holder.name.setTextColor(
                    android.graphics.Color.parseColor("#A5D6A7")
            );
            holder.km.setTextColor(
                    android.graphics.Color.parseColor("#4CAF50")
            );
            holder.avatar.setTextColor(
                    android.graphics.Color.parseColor("#4CAF50")
            );
            holder.avatar.setBackgroundResource(R.drawable.bg_avatar_circle_green);
        } else {
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.km.setText(String.format(Locale.getDefault(), "%.2f km", p.km));
            // Farben zurücksetzen
            int blue = android.graphics.Color.parseColor("#378ADD");
            int light = android.graphics.Color.parseColor("#E8F2FB");
            holder.name.setTextColor(light);
            holder.km.setTextColor(light);
            holder.avatar.setTextColor(blue);
            holder.avatar.setBackgroundResource(R.drawable.bg_avatar_circle);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}