package org.runnerup.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.runnerup.R;
import org.runnerup.view.ActiveRunActivity;

import java.util.List;
import java.util.Locale;

public class ParticipantAdapterRun extends RecyclerView.Adapter<ParticipantAdapterRun.ViewHolder> {

    private final List<ActiveRunActivity.Participant> data;

    public ParticipantAdapterRun(List<ActiveRunActivity.Participant> data) {
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
        if (!payloads.isEmpty() && "km_update".equals(payloads.get(0))) {
            holder.km.setText(
                    String.format(Locale.getDefault(), "%.2f km", data.get(position).km)
            );
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActiveRunActivity.Participant p = data.get(position);
        holder.name.setText(p.name);
        holder.km.setText(String.format(Locale.getDefault(), "%.2f km", p.km));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
}