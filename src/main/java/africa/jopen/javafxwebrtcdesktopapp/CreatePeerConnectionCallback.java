package africa.jopen.javafxwebrtcdesktopapp;

import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrack;

public interface CreatePeerConnectionCallback {
    void onIceGatheringComplete();

    void onIceCandidate(RTCIceCandidate candidate);

    void onIceCandidatesRemoved(RTCIceCandidate[] candidates);

    void publishFrame(VideoTrack videoTrack, VideoFrame frame);

    void onRemoveStream(MediaStream stream);
}