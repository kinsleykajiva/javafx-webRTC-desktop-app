package africa.jopen.javafxwebrtcdesktopapp.Entities;

import dev.onvoid.webrtc.RTCPeerConnection;
import dev.onvoid.webrtc.media.video.VideoTrack;

import java.math.BigInteger;

public class VideoItem {

    private RTCPeerConnection peerConnection;
    private BigInteger userId;
    private  String display;
    private VideoTrack videoTrack;

    public RTCPeerConnection getPeerConnection() {
        return peerConnection;
    }

    public void setPeerConnection(RTCPeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public BigInteger getUserId() {
        return userId;
    }

    public void setUserId(BigInteger userId) {
        this.userId = userId;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public VideoTrack getVideoTrack() {
        return videoTrack;
    }

    public void setVideoTrack(VideoTrack videoTrack) {
        this.videoTrack = videoTrack;
    }
}
