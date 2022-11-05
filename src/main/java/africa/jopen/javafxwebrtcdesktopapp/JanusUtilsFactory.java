package africa.jopen.javafxwebrtcdesktopapp;

import africa.jopen.javafxwebrtcdesktopapp.Entities.Publisher;
import africa.jopen.javafxwebrtcdesktopapp.Entities.Room;
import africa.jopen.javafxwebrtcdesktopapp.Entities.VideoItem;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.audio.AudioOptions;
import dev.onvoid.webrtc.media.audio.AudioTrack;
import dev.onvoid.webrtc.media.audio.AudioTrackSource;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import static africa.jopen.javafxwebrtcdesktopapp.WebRTCUtils.JANUS_URL;


public class JanusUtilsFactory {
    private static JanusClient janusClient;
    private static RTCPeerConnection peerConnection;
    private static Room room;
    private static  String userName = "KKJFX";
    private static PeerConnectionFactory peerConnectionFactory;
    private static ObservableList<VideoItem> videoItemsList = FXCollections.observableArrayList();
    private static VideoTrack videoTrack;
    private static VideoDeviceSource videoSource;
    private static VBox centerRoot;
    public static PeerConnectionFactory initZero(JanusCallback janusCallback) {
        peerConnectionFactory = WebRTCUtils.createPeerConnectionFactory();
        janusClient = new JanusClient(JANUS_URL);
        janusClient.setJanusCallback(janusCallback);
        janusClient.connect();
        return peerConnectionFactory;
    }
    public static void initOne(VBox centerRootNode,final boolean needsAudio , final boolean needsVideo , RTCPeerConnection peerConnection) {
        centerRoot = centerRootNode;

        if(needsAudio) {
            AudioOptions audioOptions = new AudioOptions();
            AudioTrackSource audioSource = peerConnectionFactory.createAudioSource(audioOptions);
            AudioTrack audioTrack = peerConnectionFactory.createAudioTrack("audioTrack", audioSource);
            peerConnection.addTrack(audioTrack, List.of("stream"));
        }
        if(needsVideo) {
            videoSource = new VideoDeviceSource();
            VideoDevice device = MediaDevices.getVideoCaptureDevices().get(0);
            videoSource.setVideoCaptureDevice(device);
            videoSource.setVideoCaptureCapability(MediaDevices.getVideoCaptureCapabilities(device).get(0)); //I believe index 0 is auto-resolution, 17 is 1280x720 @ 10fps
            videoSource.start();
            videoTrack = peerConnectionFactory.createVideoTrack("CAM", videoSource);

            peerConnection.addTrack(videoTrack, List.of("stream"));
        }

        //return peerConnection;

    }


    public static void OnMessage(RTCPeerConnection peerConnection,ObservableList<VideoItem> videoItemList,BigInteger videoRoomHandlerId,BigInteger sender, BigInteger handleId, JSONObject msg, JSONObject jsep){
        try {
            String type = msg.getString("videoroom");
            switch (type) {
                case "created":
                    System.out.println(" created ");
                    room = new Room(msg.getInt("room"));
                    break;
                case "joined":
                    JanusUtilsFactory.createOffer(peerConnection, new CreateOfferCallback() {
                        @Override
                        public void onCreateOfferSuccess(RTCSessionDescription sdp) {
                            if (videoRoomHandlerId != null) {
                                JanusUtilsFactory.getJanusClient().publish(videoRoomHandlerId, sdp);
                            }
                        }

                        @Override
                        public void onCreateFailed(String error) {

                        }
                    });

                    JSONArray publishers = msg.getJSONArray("publishers");
                    JanusUtilsFactory.handleNewPublishers(publishers);
                    break;
                case "event":
                    if (msg.has("configured") && msg.getString("configured").equals("ok") && jsep != null) {
                        //  sdp answer
                        String sdp = jsep.getString("sdp");
                        peerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.ANSWER, sdp), new SetSessionDescriptionObserver() {
                            @Override
                            public void onSuccess() {
                                System.out.println(" setRemoteDescription onCreateSuccess");
                                XUtils.invoke(() -> JanusUtilsFactory.addNewVideoItem(videoItemList, null, userName));

                            }

                            @Override
                            public void onFailure(String error) {
                                System.err.println("setRemoteDescription onFailure   - " + error);
                            }
                        });
                    } else if (msg.has("unpublished")) {
                        Long unPublishdUserId = msg.getLong("unpublished");
                        System.out.println("unPublishdUserId  " + unPublishdUserId);
                    } else if (msg.has("leaving")) {
                        JanusUtilsFactory.leavingRoom(videoItemList, msg);
                    } else if (msg.has("publishers")) {
                        JSONArray publishers_ = msg.getJSONArray("publishers");
                        JanusUtilsFactory.handleNewPublishers(publishers_);
                    } else if (msg.has("started") && msg.getString("started").equals("ok")) {
                        System.out.println("subscription started ok");
                    }
                    break;
                case "attached":
                    if (jsep != null) {
                        String sdp = jsep.getString("sdp");
                        BigInteger feedId = msg.getBigInteger("id");
                        String display = msg.getString("display");
                        Publisher publisher = room.findPublisherById(feedId);
                        JanusUtilsFactory.addNewVideoItem(videoItemList, feedId, display);

                        new JanusPeerConnection2(centerRoot, peerConnectionFactory, room, JanusUtilsFactory.getJanusClient(), sender, sdp, publisher);
                    }
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public static void initTwo(final Room room) {

        JanusUtilsFactory.room = room;
        videoItemsList.clear();
    }

    public static VideoTrack getLocalVideoTrack() {
        return videoTrack;
    }

    public static JanusClient getJanusClient() {
        return janusClient;
    }

    public static  void leavingRoom(ObservableList<VideoItem> videoItemList, final JSONObject msg){
        System.out.println("leaving - ### userId :  " + msg);
        BigInteger leavingUserId = (msg.getBigInteger("leaving"));
        room.removePublisherById(leavingUserId);
        Iterator<VideoItem> it = videoItemList.iterator();
        int index = 0;
        while (it.hasNext()) {
            VideoItem next = it.next();
            if (leavingUserId.equals(next.getUserId())) {
                XUtils.invoke(()->it.remove());

            }
            index++;
        }
    }
    public static  VideoItem addNewVideoItem(ObservableList<VideoItem> videoItemList,BigInteger userId, String display) {
        VideoItem videoItem = new VideoItem();
        videoItem.setUserId(userId);
        videoItem.setDisplay(display);
        videoItemList.add(videoItem);
        return videoItem;
    }



    public static void handleNewPublishers(JSONArray publishers) {
        System.out.println("publishers - ###");
        for (int i = 0; i < publishers.length(); i++) {
            try {
                JSONObject publishObj = publishers.getJSONObject(i);
                BigInteger feedId = publishObj.getBigInteger("id");
                String display = publishObj.getString("display");
                janusClient.subscribeAttach(feedId);
                room.addPublisher(new Publisher(feedId, display));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static RTCIceServer getIceServers() {
        RTCIceServer stunServer = new RTCIceServer();
        stunServer.urls.add("stun:stunserver.org:3478");
        stunServer.urls.add("stun:webrtc.encmed.cn:5349");
        return stunServer;
    }

    public static RTCConfiguration getRTCConfig() {

        RTCConfiguration config = new RTCConfiguration();
        config.iceServers.add(getIceServers());

        return config;
    }

    public static void createOffer(RTCPeerConnection peerConnection, CreateOfferCallback callback) {

        RTCOfferOptions options = new RTCOfferOptions();

        peerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription rtcSessionDescription) {
                peerConnection.setLocalDescription(rtcSessionDescription, new SetSessionDescriptionObserver() {

                    @Override
                    public void onSuccess() {
                        System.out.println(" setLocalDescription success");

                    }

                    @Override
                    public void onFailure(String error) {
                        System.err.println("setLocalDescription failure" + error);

                    }
                });
                if (callback != null) {
                    callback.onCreateOfferSuccess(rtcSessionDescription);
                }
            }

            @Override
            public void onFailure(String error) {
                if (callback != null) {
                    callback.onCreateFailed(error);
                }
            }
        });
    }


}
