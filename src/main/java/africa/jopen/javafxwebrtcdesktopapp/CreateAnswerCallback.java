package africa.jopen.javafxwebrtcdesktopapp;

import dev.onvoid.webrtc.RTCSessionDescription;

public interface CreateAnswerCallback {
        void onSetAnswerSuccess(RTCSessionDescription sdp);

        void onSetAnswerFailed(String error);
        }