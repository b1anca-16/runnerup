package org.runnerup.tracker;

import okhttp3.*;
import android.util.Log;

public class LiveChallenge {
    private WebSocket ws;
    private String roomId;

    public void connect(String roomId) {
        this.roomId = roomId;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("ws://10.0.2.2:8080")
                .build();

        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d("LiveChallenge", "Verbunden! ✅");
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d("LiveChallenge", "Empfangen: " + text);
                // Hier kommt Pace / Distanz des Partners an
                // → gleich weitergeben an RunActivity
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e("LiveChallenge", "Fehler: " + t.getMessage());
            }
        });
    }

    public void sendUpdate(double km, double pace, double timeSeconds) {
        String json = "{\"room\":\"" + roomId + "\","
                + "\"data\":{"
                + "\"km\":" + km + ","
                + "\"pace\":" + pace + ","
                + "\"time\":" + timeSeconds
                + "}}";
        ws.send(json);
        Log.d("LiveChallenge", "Gesendet: " + json);
    }

    public void disconnect() {
        ws.close(1000, "Session ended");
    }
}