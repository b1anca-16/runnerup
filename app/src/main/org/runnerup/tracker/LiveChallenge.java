/**
 * Singleton-Klasse, die die WebSocket-Verbindung zum Relay-Server verwaltet.
 * Ermöglicht das Erstellen und Beitreten von Live-Lauf-Räumen sowie
 * das Senden von Echtzeit-Updates (km, pace, time) an alle Teilnehmer.
 */

package org.runnerup.tracker;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import okhttp3.*;
import androidx.annotation.NonNull;

public class LiveChallenge {

    private static final String TAG = "LiveChallenge";
    private static final String SERVER_URL = "ws://10.0.2.2:8080";
    private static LiveChallenge instance;

    private WebSocket ws;
    private String roomId;
    private OnTokenReceived tokenCallback;
    private List<String> lastParticipants = new ArrayList<>();

    public interface OnTokenReceived {
        void onToken(String token);
        void onPartnerJoined();
        void onError(String message);
    }

    public interface OnParticipantsChanged {
        void onParticipantsUpdated(List<String> names);
    }
    public interface OnRunStartedListener {
        void onRunStarted();
    }

    public interface OnLeaderboardListener {
        void onLeaderboard(String json);
    }

    private OnLeaderboardListener leaderboardListener;

    public void setLeaderboardListener(OnLeaderboardListener listener) {
        this.leaderboardListener = listener;
    }

    private OnParticipantsChanged participantCallback;
    private OnRunStartedListener runStartedListener;

    public void setParticipantCallback(OnParticipantsChanged callback) {
        this.participantCallback = callback;
        if (callback != null && !lastParticipants.isEmpty()) {
            postToMain(() -> callback.onParticipantsUpdated(lastParticipants));
        }
    }

    public void setRunStartedListener(OnRunStartedListener listener) {
        this.runStartedListener = listener;
    }

    public static LiveChallenge getInstance() {
        if (instance == null) {
            instance = new LiveChallenge();
        }
        return instance;
    }

    public static class LeaderboardEntry {
        public String name;
        public double km;

        public LeaderboardEntry(String name, double km) {
            this.name = name;
            this.km = km;
        }
    }

    public void connectAndCreate(String playerName, float distance, OnTokenReceived callback) {
        this.tokenCallback = callback;
        connectInternal(() -> send("{\"action\":\"create\",\"name\":\"" + playerName + "\",\"distance\":" + distance + "}"));
    }

    public void connectAndJoin(String roomCode, String playerName, OnTokenReceived callback) {
        this.tokenCallback = callback;
        connectInternal(() -> send("{\"action\":\"join\",\"room\":\"" + roomCode + "\",\"name\":\"" + playerName + "\"}"));
    }

    private void connectInternal(Runnable onOpen) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(SERVER_URL).build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                ws = webSocket;
                Log.d(TAG, "Verbunden ✅");
                onOpen.run();
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "Empfangen: " + text);
                handleMessage(text);
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "Verbindungsfehler: " + t.getMessage());
                postToMain(() -> {
                    if (tokenCallback != null) tokenCallback.onError("Verbindung fehlgeschlagen");
                });
            }
        });
    }

    private void handleMessage(String text) {
        if (text.contains("\"action\":\"created\"") || text.contains("\"action\":\"joined\"")) {
            String token = extractToken(text);
            roomId = token;

            // Teilnehmer direkt aus der Antwort lesen falls vorhanden
            if (text.contains("\"participants\"")) {
                List<String> names = extractParticipants(text.replace("\"participants\"", "\"list\""));
                lastParticipants = names;
                postToMain(() -> { if (participantCallback != null) participantCallback.onParticipantsUpdated(names); });
            }

            postToMain(() -> { if (tokenCallback != null) tokenCallback.onToken(token); });

        } else if (text.contains("\"action\":\"partner_joined\"")) {
            postToMain(() -> { if (tokenCallback != null) tokenCallback.onPartnerJoined(); });

        }else if (text.contains("\"action\":\"started\"")) {
            postToMain(() -> {
                if (runStartedListener != null) {
                    runStartedListener.onRunStarted();
                }
            });
        }else if (text.contains("\"action\":\"leaderboard\"")) {
            postToMain(() -> {
                if (leaderboardListener != null) {
                    leaderboardListener.onLeaderboard(text);
                }
            });
        } else if (text.contains("\"action\":\"error\"")) {
            // Echte Fehlermeldung vom Server extrahieren
            String message = "Unbekannter Fehler";
            try {
                message = text.split("\"message\":\"")[1].split("\"")[0];
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Parsen der Error-Message");
            }
            final String finalMessage = message;
            postToMain(() -> { if (tokenCallback != null) tokenCallback.onError(finalMessage); });
        } else if (text.contains("\"action\":\"participants\"")) {
            List<String> names = extractParticipants(text);
            lastParticipants = names;
            postToMain(() -> { if (participantCallback != null) participantCallback.onParticipantsUpdated(names); });
        }
    }

    public void sendUpdate(double km) {
        String json =
                "{\"action\":\"progress\"," +
                        "\"distance\":" + km + "}";
        send(json);
    }

    public void startRun() {
        String json = "{\"action\":\"start\"}";
        send(json);
    }

    public void sendLeave() {
        Log.d(TAG, "sendLeave() aufgerufen, ws=" + ws);
        send("{\"action\":\"leave\"}");
    }

    public void disconnect() {
        if (ws != null) ws.close(1000, "Session ended");
    }

    private void send(String json) {
        if (ws != null) {
            ws.send(json);
            Log.d(TAG, "Gesendet: " + json);
        } else {
            Log.e(TAG, "WebSocket ist null!");
        }
    }

    private void postToMain(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    private String extractToken(String text) {
        return text.split("\"token\":\"")[1].split("\"")[0];
    }

    private List<String> extractParticipants(String text) {
        List<String> names = new ArrayList<>();
        try {
            // "list":[{"name":"Max","role":"HOST"},{"name":"Anna","role":"JOIN"}]
            String listPart = text.split("\"list\":")[1];
            String[] entries = listPart.split("\\{");
            for (String entry : entries) {
                if (entry.contains("\"name\":")) {
                    String name = entry.split("\"name\":\"")[1].split("\"")[0];
                    String role = entry.contains("\"role\":\"HOST\"") ? " (Host)" : "";
                    names.add(name + role);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Parsen der Teilnehmer: " + e.getMessage());
        }
        return names;
    }
}