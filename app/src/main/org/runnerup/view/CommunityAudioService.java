package org.runnerup.view;

import android.content.Context;
import org.runnerup.workout.feedback.RUTextToSpeech;
import org.runnerup.workout.feedback.UtterancePrio;

public class CommunityAudioService {
    private final RUTextToSpeech tts;

    public CommunityAudioService(Context context) {
        this.tts = new RUTextToSpeech(context, true);
    }

    public void announceJoin(String name) {
        tts.emit(name + " joined the run", UtterancePrio.PRIO_COMMUNITY, false);
    }

    public void announceStart() {
        tts.emit("The run is starting", UtterancePrio.PRIO_COMMUNITY, true);
    }

    public void announceConnectionLost() {
        tts.emit("Connection to community server lost", UtterancePrio.PRIO_COMMUNITY, true);
    }

    public void announceCreate(String runName) {
        tts.emit("Created run " + runName, UtterancePrio.PRIO_COMMUNITY, false);
    }

    public void announceJoinSuccess(String runName) {
        tts.emit("Joined run " + runName, UtterancePrio.PRIO_COMMUNITY, false);
    }

    public void announcePartnerJoined() {
        tts.emit("A partner has joined", UtterancePrio.PRIO_COMMUNITY, false);
    }

    public void announceError(String error) {
        tts.emit("Error: " + error, UtterancePrio.PRIO_COMMUNITY, true);
    }

    public void announceFinished(String name, int place) {
        tts.emit(name + " finished in place " + place, UtterancePrio.PRIO_COMMUNITY, false);
    }

    public void announceRank(int place) {
        tts.emit("You are currently in place " + place, UtterancePrio.PRIO_COMMUNITY, false);
    }
    
    public void stop() {
        tts.stop();
    }
}
