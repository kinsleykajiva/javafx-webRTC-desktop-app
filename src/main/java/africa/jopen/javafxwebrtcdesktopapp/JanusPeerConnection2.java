package africa.jopen.javafxwebrtcdesktopapp;

import africa.jopen.javafxwebrtcdesktopapp.Entities.Publisher;
import africa.jopen.javafxwebrtcdesktopapp.Entities.Room;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStream;
import dev.onvoid.webrtc.media.video.VideoFrame;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.scene.layout.VBox;


import java.math.BigInteger;

public class JanusPeerConnection2 {



    private RTCPeerConnection peerConnection;
    //  CreateAnswerCallback callback;
    private VBox centerRoot;
    private Room room;
    private PeerConnectionFactory peerConnectionFactory;


    public JanusPeerConnection2(VBox centerRoot, PeerConnectionFactory peerConnectionFactory, Room room, JanusClient janusClient,
                                BigInteger sender, String sdp, Publisher publisher) {

        this.peerConnectionFactory = peerConnectionFactory;

        this.centerRoot = centerRoot;
        this.room = room;
        peerConnection = createPeerConnection(new CreatePeerConnectionCallback() {
            @Override
            public void onIceGatheringComplete() {
                janusClient.trickleCandidateComplete(sender);
            }

            @Override
            public void onIceCandidate(RTCIceCandidate candidate) {
                janusClient.trickleCandidate(sender, candidate);
            }

            @Override
            public void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
            }


            @Override
            public void publishFrame(VideoTrack videoTrack, VideoFrame frame) {

            }

            @Override
            public void onRemoveStream(MediaStream stream) {
                System.out.println("onRemoveStream");
            }
        });

        peerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.OFFER, sdp),
                new SetSessionDescriptionObserver() {

                    @Override
                    public void onSuccess() {
                        System.out.println("setRemoteDescription onSetSuccess");

                        createAnswer(peerConnection, new CreateAnswerCallback() {
                            @Override
                            public void onSetAnswerSuccess(RTCSessionDescription sdp) {
                                janusClient.subscriptionStart(publisher.getHandleId(), room.getId(), sdp);
                            }

                            @Override
                            public void onSetAnswerFailed(String error) {
                                System.err.println("setRemoteDescription onSetAnswerFailed   - " + error);

                            }
                        });
                    }

                    @Override
                    public void onFailure(String s) {
                        System.err.println("setRemoteDescription onFailure  : " + s);
                    }
                });
    }

    private RTCPeerConnection createPeerConnection(CreatePeerConnectionCallback callback) {




        return peerConnectionFactory.createPeerConnection(JanusUtilsFactory.getRTCConfig(), new JanusPeerConnection(centerRoot, callback));

    }


    private void createAnswer(RTCPeerConnection peerConnection, CreateAnswerCallback callback) {

        RTCAnswerOptions answerOptions = new RTCAnswerOptions();
        peerConnection.createAnswer(answerOptions, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription rtcSessionDescription) {
                peerConnection.setLocalDescription(rtcSessionDescription, new SetSessionDescriptionObserver() {

                    @Override
                    public void onSuccess() {
                        System.out.println("setLocalDescription success");
                        if (callback != null) {
                            callback.onSetAnswerSuccess(rtcSessionDescription);
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        System.out.println("setLocalDescription failure");
                        if (callback != null) {
                            callback.onSetAnswerFailed(error);
                        }
                    }
                });

            }

            @Override
            public void onFailure(String s) {
                System.out.println("createAnswer failure" + s);
            }
        });
    }


}
