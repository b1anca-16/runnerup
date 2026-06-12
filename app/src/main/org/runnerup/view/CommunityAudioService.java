package org.runnerup.view;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import org.runnerup.R;
import org.runnerup.workout.feedback.RUTextToSpeech;
import org.runnerup.workout.feedback.UtterancePrio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommunityAudioService {
    private static CommunityAudioService instance;
    private RUTextToSpeech rutts;
    private TextToSpeech tts;
    private boolean initialized = false;
    private final Context appContext;
    private final List<String> pendingAnnouncements = new ArrayList<>();

    private CommunityAudioService(Context context) {
        this.appContext = context.getApplicationContext();
        Log.d("CommunityAudioService", "Initializing TextToSpeech...");
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Log.d("CommunityAudioService", "TTS initialized successfully");
                
                // Set language explicitly to be sure
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("CommunityAudioService", "Language not supported");
                }

                synchronized (CommunityAudioService.this) {
                    // mute=false so it actually speaks if not in workout
                    rutts = new RUTextToSpeech(tts, false, appContext);
                    initialized = true;
                    Log.d("CommunityAudioService", "Processing " + pendingAnnouncements.size() + " pending announcements");
                    for (String text : pendingAnnouncements) {
                        speakInternal(text);
                    }
                    pendingAnnouncements.clear();
                }
            } else {
                Log.e("CommunityAudioService", "TTS initialization failed with status: " + status);
            }
        });
    }

    public static synchronized CommunityAudioService getInstance(Context context) {
        if (instance == null) {
            instance = new CommunityAudioService(context);
        }
        return instance;
    }

    public void announceRunCreated(String runName) {
        speak(appContext.getString(R.string.community_run_created, runName));
    }

    public void announceRunStarted() {
        speak(appContext.getString(R.string.community_run_started));
    }

    public void announceRunEnded() {
        speak(appContext.getString(R.string.community_run_ended));
    }

    public void announcePartnerJoined(String partnerName) {
        if (partnerName == null) {
            speak(appContext.getString(R.string.community_partner_joined_generic));
        } else {
            speak(appContext.getString(R.string.community_partner_joined_your_run, partnerName));
        }
    }

    public void announceYouJoined(String runName, String hostName) {
        if (runName != null && hostName != null) {
            speak(appContext.getString(R.string.community_you_joined_full, runName, hostName));
        } else if (hostName != null) {
            speak(appContext.getString(R.string.community_you_joined, hostName));
        } else {
            speak(appContext.getString(R.string.community_partner_joined_generic));
        }
    }

    public void announceConnectionLost() {
        speak(appContext.getString(R.string.community_connection_lost));
    }

    private synchronized void speak(String text) {
        if (!initialized || rutts == null) {
            Log.d("CommunityAudioService", "TTS not ready, queuing: " + text);
            pendingAnnouncements.add(text);
        } else {
            Log.d("CommunityAudioService", "TTS ready, speaking immediately: " + text);
            speakInternal(text);
        }
    }

    private void speakInternal(String text) {
        if (rutts != null) {
            Log.d("CommunityAudioService", "Speaking: " + text);
            // In RUTextToSpeech, speak() only buffers. We need to call emit() to actually speak.
            // Using flush=true to ensure it speaks immediately
            rutts.speak(text, UtterancePrio.PRIO_COACH, true, null);
            rutts.emit();
        }
    }

    public void onDestroy() {
        synchronized (this) {
            if (tts != null) {
                tts.shutdown();
                tts = null;
            }
            initialized = false;
            rutts = null;
            instance = null;
        }
    }
}
