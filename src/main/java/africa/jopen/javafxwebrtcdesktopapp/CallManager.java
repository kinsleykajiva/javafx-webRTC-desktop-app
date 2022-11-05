package africa.jopen.javafxwebrtcdesktopapp;

import africa.jopen.javafxwebrtcdesktopapp.Entities.Publisher;
import africa.jopen.javafxwebrtcdesktopapp.Entities.VideoItem;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.video.VideoCapture;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import dev.onvoid.webrtc.media.video.desktop.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.json.JSONObject;
import africa.jopen.javafxwebrtcdesktopapp.Entities.Room;


import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

public class CallManager {

    private VideoCapture videoCapturer;
    private PeerConnectionFactory peerConnectionFactory;
    private RTCPeerConnection peerConnection;
    private BigInteger videoRoomHandlerId;
    private MutableMap<String, VideoView> videoViewNodeMap = Maps.mutable.with();
    private ObservableList<VideoItem> videoItemList = FXCollections.observableArrayList();
    private Room room = new Room(1234);
    private String userName = "janus user";
    private VBox centerRoot;

    private HelloController mainControllerView;
//    private WindowCapturer windowCapturer;


    public CallManager(HelloController mainControllerView , VBox centerRoot) {
        this.centerRoot = centerRoot;
        this.mainControllerView = mainControllerView;
    }

    private void onScreenCapture(DesktopCapturer.Result result, DesktopFrame frame) {


    }





    public void initJanusWebrtcSession() {

        peerConnectionFactory =JanusUtilsFactory.initZero( janusCallback);
        peerConnection = peerConnectionFactory.createPeerConnection(JanusUtilsFactory.getRTCConfig(), new PeerConnectionObserver() {

            @Override
            public void onIceGatheringChange(RTCIceGatheringState state) {
                JanusUtilsFactory.getJanusClient().trickleCandidateComplete(videoRoomHandlerId);
            }

            @Override
            public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {

            }
        });

       JanusUtilsFactory.initOne(centerRoot,true, true ,peerConnection );

        JanusUtilsFactory.initTwo(room);


        videoItemList.addListener((ListChangeListener<VideoItem>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (VideoItem item : c.getAddedSubList()) {

                        if (item.getDisplay().equals(userName)) {
                            Platform.runLater(() -> {


                                System.out.println("local video view");
                                VideoView localVideoView = new VideoView();
                                localVideoView.resize(400, 400);
                                localVideoView.setId(String.valueOf(item.getUserId()));
                                centerRoot.getChildren().add(localVideoView);
                                JanusUtilsFactory.getLocalVideoTrack().addSink(localVideoView::setVideoFrame);

                            });

                        } else {
                            //if (!videoViewNodeMap.containsKey(item.getUserId())) {
                                Platform.runLater(() -> {
                                    System.out.println("remote video view");
                                    VideoView remoteVideoView = new VideoView();
                                    remoteVideoView.resize(400, 400);
                                    remoteVideoView.setId(String.valueOf(item.getUserId()));
                                    centerRoot.getChildren().add(remoteVideoView);
                                    videoViewNodeMap.put(String.valueOf(item.getUserId()), remoteVideoView);
                                });
                           // }
                        }
                    }
                }
                if (c.wasRemoved()) {
                    for (VideoItem item : c.getRemoved()) {
                        System.out.println("removing local video view" + item.getUserId());
                        //  centerRoot.getChildren().remove(item);
                        Node target = centerRoot.getChildren().stream()
                                .filter(node -> node instanceof VideoView &&
                                        !node.getId().equals(null) && !node.getId().equals("null") &&
                                        node.getId().equals(String.valueOf(item.getUserId())))
                                .findFirst()
                                .orElse(null);
                        if (target != null) {
                            Platform.runLater(() -> {
                                System.out.println("removing local video view Node");
                                centerRoot.getChildren().remove(target);

                            });
                        }
                    }
                }
            }
        });

    }


    private JanusCallback janusCallback = new JanusCallback() {
        @Override
        public void onCreateSession(BigInteger sessionId) {
            JanusUtilsFactory.getJanusClient().attachPlugin("janus.plugin.videoroom");
        }

        @Override
        public void onAttached(BigInteger handleId) {
            System.out.println("onAttached");
            videoRoomHandlerId = handleId;
            JanusUtilsFactory.getJanusClient().joinRoom(handleId, room.getId(), userName);

        }

        @Override
        public void onSubscribeAttached(BigInteger subscriptionHandleId, BigInteger feedId) {
            Publisher publisher = room.findPublisherById(feedId);
            if (publisher != null) {
                publisher.setHandleId(subscriptionHandleId);
                JanusUtilsFactory.getJanusClient().subscribe(subscriptionHandleId, room.getId(), feedId);
            }
        }

        @Override
        public void onDetached(BigInteger handleId) {
            videoRoomHandlerId = null;
        }

        @Override
        public void onHangup(BigInteger handleId) {

        }

        @Override
        public void onMessage(BigInteger sender, BigInteger handleId, JSONObject msg, JSONObject jsep) {
            System.out.println("onMessage  : " + msg);
            if (msg.has("videoroom")) {
                JanusUtilsFactory. OnMessage(peerConnection, videoItemList ,videoRoomHandlerId,sender,  handleId,  msg,  jsep);
            }
        }

        @Override
        public void onIceCandidate(BigInteger handleId, JSONObject candidate) {
        }

        @Override
        public void onDestroySession(BigInteger sessionId) {
        }

        @Override
        public void onError(String error) {
            System.out.println("onError  : " + error);
        }
    };




    public static FlowPane renderCallers(List<VideoItem> videoItemList, VideoTrack videoTrack) {
        FlowPane flowContainer = new FlowPane();


        videoItemList.forEach(caller -> {
            VideoView videoView = new VideoView();
            Button videoBtn = new Button(caller.getDisplay());
            videoBtn.setMnemonicParsing(false);
            videoBtn.getStyleClass().add("cat-button");
            videoBtn.setPrefWidth(160.0);
            videoBtn.setMaxWidth(160.0);
            videoBtn.setPrefHeight(200.0);
            videoBtn.setMaxHeight(200.0);
            videoBtn.setWrapText(true);
            videoBtn.setTextAlignment(TextAlignment.CENTER);
            videoBtn.setAlignment(Pos.CENTER);
            videoBtn.setContentDisplay(ContentDisplay.TOP);

            videoView.resize(100, 100);
            videoBtn.setGraphic(videoView);
            videoBtn.setOnAction(e2 -> {
                System.out.println("videoBtn.setOnAction");
            });
            videoTrack.addSink(videoView::setVideoFrame);
            flowContainer.getChildren().add(videoBtn);
        });

        return flowContainer;
    }

    void addViewItem(VideoTrack videoTrack, boolean islocal) {

        VideoView localVideoView = new VideoView();

        localVideoView.resize(400, 400);

        centerRoot.getChildren().add(localVideoView);
        videoTrack.addSink(localVideoView::setVideoFrame);



    }

    public static FlowPane renderACaller(VideoItem videoItem, VideoTrack videoTrack, VideoDeviceSource videoSource) {
        FlowPane flowContainer = new FlowPane();

        VideoView videoView = new VideoView();
        Button videoBtn = new Button(videoItem == null ? "mewadii" : videoItem.getDisplay());
        videoBtn.setMnemonicParsing(false);
        videoBtn.getStyleClass().add("cat-button");
        videoBtn.setPrefWidth(160.0);
        videoBtn.setMaxWidth(160.0);
        videoBtn.setPrefHeight(200.0);
        videoBtn.setMaxHeight(200.0);
        videoBtn.setWrapText(true);
        videoBtn.setTextAlignment(TextAlignment.CENTER);
        videoBtn.setAlignment(Pos.CENTER);
        videoBtn.setContentDisplay(ContentDisplay.TOP);

        videoView.resize(100, 100);
        videoBtn.setGraphic(videoView);
        videoBtn.setOnAction(e2 -> {
            System.out.println("videoBtn.setOnAction");
        });
        videoTrack.addSink(videoView::setVideoFrame);
        flowContainer.getChildren().add(videoBtn);
        try {
            videoSource.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return flowContainer;
    }

}
