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
        TextView name;
        TextView km;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_name);
            km = itemView.findViewById(R.id.tv_km);
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
        holder.name.setText(p.place + ". " + p.name);
        if (p.finished) {
            holder.km.setText("Finished");
            holder.itemView.setBackgroundColor(android.graphics.Color.parseColor("#1A3D1A"));
        } else {
            holder.km.setText(String.format(Locale.getDefault(), "%.2f km", p.km));
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}