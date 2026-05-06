package org.runnerup.tracker;

import okhttp3.*;
import android.util.Log;

public class LiveChallenge {
    private WebSocket ws;
    private String roomId;
    private OnTokenReceived tokenCallback;

    public interface OnTokenReceived {
        void onToken(String token);
        void onPartnerJoined();
        void onError(String message);
    }

    public void connect(String serverUrl, OnTokenReceived callback) {
        this.tokenCallback = callback;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d("LiveChallenge", "Verbunden! ✅");
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                Log.d("LiveChallenge", "Empfangen: " + text);
                // JSON parsen
                if (text.contains("\"action\":\"created\"")) {
                    String token = text.split("\"token\":\"")[1].replace("\"}", "");
                    roomId = token;
                    if (tokenCallback != null) tokenCallback.onToken(token);

                } else if (text.contains("\"action\":\"joined\"")) {
                    String token = text.split("\"token\":\"")[1].replace("\"}", "");
                    roomId = token;
                    if (tokenCallback != null) tokenCallback.onToken(token);

                } else if (text.contains("\"action\":\"partner_joined\"")) {
                    if (tokenCallback != null) tokenCallback.onPartnerJoined();

                } else if (text.contains("\"action\":\"error\"")) {
                    if (tokenCallback != null) tokenCallback.onError("Room nicht gefunden");
                }
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e("LiveChallenge", "Fehler: " + t.getMessage());
            }
        });
    }

    public void createRoom() {
        ws.send("{\"action\":\"create\"}");
    }

    public void joinRoom(String token) {
        ws.send("{\"action\":\"join\",\"room\":\"" + token + "\"}");
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