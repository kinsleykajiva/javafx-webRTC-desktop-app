package africa.jopen.javafxwebrtcdesktopapp;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCRtpCodecCapability;
import dev.onvoid.webrtc.RTCSessionDescription;
import dev.onvoid.webrtc.media.Device;
import dev.onvoid.webrtc.media.DeviceChangeListener;
import dev.onvoid.webrtc.media.MediaDevices;
import dev.onvoid.webrtc.media.MediaType;
import dev.onvoid.webrtc.media.audio.*;
import dev.onvoid.webrtc.media.video.VideoDevice;
import dev.onvoid.webrtc.media.video.VideoDeviceSource;
import dev.onvoid.webrtc.media.video.VideoTrack;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.jsdp.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;



public class WebRTCUtils {
   public static final String JANUS_URL = "wss://janus.conf.meetecho.com/ws";
    private static  boolean isMediaReady = false;
    private static final Pattern SPLIT = Pattern.compile("\\r?\\n");
    public static RTCSessionDescription setCodecPreference(final RTCSessionDescription sessionDescription,
                                                           final CodecPreference codecPreference) {
        return new RTCSessionDescription(
                sessionDescription.sdpType,
                setCodecPreference(sessionDescription.sdp, codecPreference)
        );
    }

    private static DeviceChangeListener listener = new DeviceChangeListener() {

        @Override
        public void deviceConnected(Device device) {
            System.out.println(WebRTCUtils.class + "- Device connected: " + device.getName());

        }

        @Override
        public void deviceDisconnected(Device device) {
            System.out.println(WebRTCUtils.class + "- Device connected: " + device.getName());
        }
    };
    public static boolean isMediaReady(){
        MediaDevices.addDeviceChangeListener(listener);
       System.out.println("isMediaReady: " + isMediaReady);

        return false;
    }

    public  static  void initWebRTC(){
        System.setProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("java.net.preferIPv4Stack", "true");


    }

    public static PeerConnectionFactory createPeerConnectionFactory() {

        PeerConnectionFactory factory11 = new PeerConnectionFactory();

        VideoDeviceSource vid = new VideoDeviceSource();
        VideoDevice device = MediaDevices.getVideoCaptureDevices().get(0);
        vid.setVideoCaptureDevice(device);
        vid.setVideoCaptureCapability(MediaDevices.getVideoCaptureCapabilities(device).get(0)); //I believe index 0 is auto-resolution, 17 is 1280x720 @ 10fps

        AudioTrack audioTrack;
        VideoTrack videoTrack;
        AudioOptions audioOptions = new AudioOptions();
        audioOptions.noiseSuppression = true;

        AudioTrackSource audioSource = factory11.createAudioSource(audioOptions);
        audioTrack = factory11.createAudioTrack("AUDIO", audioSource);
        videoTrack = factory11.createVideoTrack("CAM", vid);

        return factory11;
    }

    private static final class CodecInfo {

        private final String format;
        private final String name;
        private final String attr;

        private CodecInfo(final String format, final String name, final String attr) {
            this.format = format;
            this.name = name;
            this.attr = attr;
        }
    }
    public enum CodecPreference {
        // The order in which values are declared is the order in which it will be presented to the user
        H264("H264", (o1, o2) -> 0),
        VP8("VP8", (o1, o2) -> 0),
        VP9("VP9", (o1, o2) -> {
            final int profile1 = Integer.parseInt(o1.attr.split("profile-id=")[1].substring(0, 1));
            final int profile2 = Integer.parseInt(o2.attr.split("profile-id=")[1].substring(0, 1));
            return -Integer.compare(profile1, profile2);
        }),
        AV1("AV1", (o1, o2) -> 0);

        private final String name;
        private final Comparator<CodecInfo> comparator;

        CodecPreference(final String name, final Comparator<CodecInfo> comparator) {
            this.name = name;
            this.comparator = comparator;
        }

        public static List<CodecPreference> getAvailablePreferences() {
            final List<RTCRtpCodecCapability> codecCapabilities =
                    new PeerConnectionFactory().getRtpReceiverCapabilities(MediaType.VIDEO).getCodecs();
            return Arrays.stream(CodecPreference.values())
                    .filter(codecPreference ->
                            codecCapabilities.stream().anyMatch(rtcRtpCodecCapability ->
                                    rtcRtpCodecCapability.getName().contains(codecPreference.name)))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
    /**
     * Convert SCTP SDP v21 format to SCTP SDP v5.
     * webrtc-java generates a v21 SDP string, but jsdp doesn't accept it.
     * The v5 string should be handled correctly by WebRTC endpoints.
     * See https://blog.mozilla.org/webrtc/how-to-avoid-data-channel-breaking/
     * @param sdp SDP v21 string
     * @return SDP v5 string
     */
    private static String v21tov5format(final String sdp) {
        final String[] lines = SPLIT.split(sdp);
        int mIndex = -1;
        int port = -1;
        int sctpPort = -1;
        int sctpIndex = -1;
        String protocol = "";
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (line.startsWith("m=application") && line.contains("webrtc-datachannel")) {
                // Expected something like: "m=application 54111 UDP/DTLS/SCTP webrtc-datachannel"
                mIndex = i;
                final String[] parts = line.split(" ");
                port = Integer.parseInt(parts[1]);
                protocol = parts[2];
            } else if (mIndex >= 0) {
                if (line.startsWith("a=sctp-port")) {
                    sctpPort = Integer.parseInt(line.split(":")[1]);
                    sctpIndex = i;
                    break;
                }
            }
        }
        if (mIndex < 0 || sctpIndex < 0) {
            // Seems like already using the old format
            return sdp;
        } else {
            lines[mIndex] = "m=application " + port + " " + protocol + " " + sctpPort;
            lines[sctpIndex] = "a=sctpmap:" + sctpPort + " webrtc-datachannel 256";
            return String.join("\r\n", lines);
        }
    }

    public static String setCodecPreference(final String sdp, final CodecPreference codecPreference) {
        final String newSdp = v21tov5format(sdp);
        try {
            final SessionDescription sessionDescription = SDPFactory.parseSessionDescription(newSdp);
            final MediaDescription[] mediaDescriptions = sessionDescription.getMediaDescriptions();
            MediaDescription videoDescription = null;
            for (final MediaDescription mediaDescription : mediaDescriptions) {
                final Media media = mediaDescription.getMedia();
                if (media.getMediaType().equals("video")) {
                    videoDescription = mediaDescription;
                    break;
                }
            }

            if (videoDescription == null) {
                return sdp;
            }

            final String[] formats = videoDescription.getMedia().getMediaFormats();

            final Attribute[] rtpMapAttrs = videoDescription.getAttributes("rtpmap");
            final HashMap<String, String> rtpMap = new HashMap<>(rtpMapAttrs.length);
            for (final Attribute attr : rtpMapAttrs) {
                final String[] split = attr.getValue().split(" ");
                rtpMap.put(split[0], split[1]);
            }

            final Attribute[] fmtp = videoDescription.getAttributes("fmtp");
            final HashMap<String, String> fmtpMap = new HashMap<>(fmtp.length);
            for (final Attribute attr : fmtp) {
                final String[] split = attr.getValue().split(" ");
                fmtpMap.put(split[0], split[1]);
            }

            final List<CodecInfo> codecs = new ArrayList<>(formats.length);
            for (final String format : formats) {
                codecs.add(new CodecInfo(
                        format,
                        rtpMap.get(format),
                        fmtpMap.getOrDefault(format, null)
                ));
            }
            codecs.sort(getCodecComparator(codecPreference));

            videoDescription.getMedia().setMediaFormats(codecs.stream()
                    .map(codecInfo -> codecInfo.format)
                    .toArray(String[]::new)
            );
            // TODO: v5tov21format(sessionDescription.toString())
            return sessionDescription.toString();
        } catch (final SDPException e) {
            return sdp;
        }
    }
    private static Comparator<CodecInfo> getCodecComparator(final CodecPreference codecPreference) {
        return (o1, o2) -> {
            if (o1.name.contains(codecPreference.name)) {
                if (o2.name.contains(codecPreference.name)) {
                    return codecPreference.comparator.compare(o1, o2);
                } else {
                    return -1;
                }
            }
            if (o2.name.contains(codecPreference.name)) {
                return 1;
            }
            return 0;
        };
    }

    public static List<AudioDevice> getAudioRenderDevicesBlocking() {
        final CompletableFuture<List<AudioDevice>> future = new CompletableFuture<>();
        getAudioRenderDevices(future::complete);
        return future.join();
    }
    public static void getAudioRenderDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioRenderDevices();
            callback.onReady(list);
        }).start();
    }
    public static void getAudioCaptureDevices(final AudioDevicesCallback callback) {
        new Thread(() -> {
            final List<AudioDevice> list = MediaDevices.getAudioCaptureDevices();
            callback.onReady(list);
        }).start();
    }

    public static List<AudioDevice> getAudioCaptureDevicesBlocking() {
        final CompletableFuture<List<AudioDevice>> future = new CompletableFuture<>();
        getAudioCaptureDevices(future::complete);
        return future.join();
    }
    public interface AudioDevicesCallback {
        void onReady(final List<AudioDevice> audioDevices);
    }

    public static void getAudioDeviceModule(final AudioDeviceModuleCallback callback) {
        new Thread(() -> {
            final AudioDeviceModule audioDeviceModule = new AudioDeviceModule();
            callback.onCreated(audioDeviceModule);
        }).start();
    }
    public interface AudioDeviceModuleCallback {
        void onCreated(final AudioDeviceModule audioDeviceModule);
    }

}
