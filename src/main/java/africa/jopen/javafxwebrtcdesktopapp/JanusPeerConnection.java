package africa.jopen.javafxwebrtcdesktopapp;

import dev.onvoid.webrtc.PeerConnectionObserver;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCIceGatheringState;
import dev.onvoid.webrtc.RTCRtpTransceiver;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSink;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.scene.layout.VBox;

public class JanusPeerConnection implements PeerConnectionObserver {

    private AudioTrackSink audioTrackSink;
    private CreatePeerConnectionCallback callback;
    private VBox centerRoot;


    public JanusPeerConnection(VBox centerRoot, CreatePeerConnectionCallback createPeerConnectionCallback) {
        callback = createPeerConnectionCallback;
        this.centerRoot = centerRoot;

    }


    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {

        if (callback != null) {
            callback.onIceCandidate(candidate);
        }
    }

    @Override
    public void onIceGatheringChange(RTCIceGatheringState newState) {
        System.out.println("onIceGatheringChange " + newState);

        if (newState == RTCIceGatheringState.COMPLETE) {
            if (callback != null) {
                callback.onIceGatheringComplete();
            }
        }
    }

    @Override
    public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {

        System.out.println("onIceCandidatesRemoved");
        if (callback != null) {
            callback.onIceCandidatesRemoved(candidates);
        }
    }

    @Override
    public void onTrack(RTCRtpTransceiver transceiver) {
        MediaStreamTrack track = transceiver.getReceiver().getTrack();
        String kind = track.getKind();

        if (kind.equals(MediaStreamTrack.VIDEO_TRACK_KIND)) {
            VideoTrack videoTrack = (VideoTrack) track;
            videoTrack.addSink(frame -> centerRoot.getChildren().stream()
                    .filter(node -> node instanceof VideoView && node.getId() != null && !node.getId().equals("null"))
                    .forEach(node -> {
                        VideoView videoView = (VideoView) node;
                        // centerRoot.lookup(String.valueOf(videoItem.getUserId()));
                        if (videoView != null) {
                            try {
                                System.out.println("videoView.getId()vvvvvvvvvvvvvvvvvvvvv " + videoView.getId());
                                frame.retain();
                                videoView.setVideoFrame(frame);
                                frame.release();
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.err.println("Render Error : " + e.getMessage());
                            }
                        }
                    }));

        }
    }


}
