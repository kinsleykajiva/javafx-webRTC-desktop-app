package africa.jopen.javafxwebrtcdesktopapp;




import africa.jopen.javafxwebrtcdesktopapp.Entities.JanusMessageType;
import africa.jopen.javafxwebrtcdesktopapp.Entities.PluginHandle;
import africa.jopen.javafxwebrtcdesktopapp.Entities.Transaction;
import dev.onvoid.webrtc.RTCIceCandidate;
import dev.onvoid.webrtc.RTCSessionDescription;
import org.json.JSONException;
import org.json.JSONObject;


import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class JanusClient implements WebSocketChannel.WebSocketCallback {

    private final ConcurrentHashMap<BigInteger, PluginHandle> attachedPlugins = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Transaction> transactions = new ConcurrentHashMap<>();
    private BigInteger sessionId = null;
    private JanusCallback janusCallback;

    private volatile boolean isKeepAliveRunning;
    private Thread keepAliveThread;

    private String janusUrl;
    private WebSocketChannel webSocketChannel;

    public JanusClient(String janusUrl) {
        this.janusUrl = janusUrl;
        webSocketChannel = new WebSocketChannel();
        webSocketChannel.setWebSocketCallback(this);
    }

    public void setJanusCallback(JanusCallback janusCallback) {
        this.janusCallback = janusCallback;
    }

    public void connect() {
        webSocketChannel.connect(janusUrl);
    }

    public void disConnect() {
        stopKeepAliveTimer();
        if (webSocketChannel != null) {
            webSocketChannel.close();
            webSocketChannel = null;
        }
    }

    private void createSession() {
        String tid = randomString(12);
        transactions.put(tid, new Transaction(tid) {
            @Override
            public void onSuccess(JSONObject msg) throws Exception {
                JSONObject data = msg.getJSONObject("data");
                System.out.println("createSession onSuccess:" + data.toString());
                sessionId = new BigInteger(String.valueOf(data.getBigInteger("id")));
                startKeepAliveTimer();
                if (janusCallback != null) {
                    janusCallback.onCreateSession(sessionId);
                }
            }
        });
        try {
            JSONObject obj = new JSONObject();
            obj.put("janus", "create");
            obj.put("transaction", tid);
            webSocketChannel.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroySession() {
        String tid = randomString(12);
        transactions.put(tid, new Transaction(tid) {
            @Override
            public void onSuccess(JSONObject msg) throws Exception {
                stopKeepAliveTimer();
                if (janusCallback != null) {
                    janusCallback.onDestroySession(sessionId);
                }
            }
        });
        try {
            JSONObject obj = new JSONObject();
            obj.put("janus", "destroy");
            obj.put("transaction", tid);
            obj.put("session_id", sessionId);
            webSocketChannel.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attachPlugin(String pluginName) {
        String tid = randomString(12);
        transactions.put(tid, new Transaction(tid) {
            @Override
            public void onSuccess(JSONObject msg) throws Exception {
                JSONObject data = msg.getJSONObject("data");
                BigInteger handleId = new BigInteger(String.valueOf(data.getBigInteger("id")));
                if (janusCallback != null) {
                    janusCallback.onAttached(handleId);
                }
                PluginHandle handle = new PluginHandle(handleId);
                attachedPlugins.put(handleId, handle);
            }
        });

        try {
            JSONObject obj = new JSONObject();
            obj.put("janus", "attach");
            obj.put("transaction", tid);
            obj.put("plugin", pluginName);
            obj.put("session_id", sessionId);
            System.err.println("panooo - -2");
            webSocketChannel.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * attach handler
     * attach 一 sdp, SFU
     *
     * @param feedId
     */
    public void subscribeAttach(BigInteger feedId) {
        String tid = randomString(12);
        transactions.put(tid, new Transaction(tid, feedId) {
            @Override
            public void onSuccess(JSONObject msg, BigInteger feedId) throws Exception {
                JSONObject data = msg.getJSONObject("data");
                BigInteger handleId = new BigInteger(String.valueOf(data.getBigInteger("id")));
                if (janusCallback != null) {
                    janusCallback.onSubscribeAttached(handleId, feedId);
                }
                PluginHandle handle = new PluginHandle(handleId);
                attachedPlugins.put(handleId, handle);
            }
        });

        try {
            JSONObject obj = new JSONObject();
            obj.put("janus", "attach");
            obj.put("transaction", tid);
            obj.put("plugin", "janus.plugin.videoroom");
            obj.put("session_id", sessionId);
            webSocketChannel.sendMessage(obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
/**
 * This can be used to create for video and audiobridge pligins*/
    public void creatRoom(){



        JSONObject msg = new JSONObject();
        try {
            msg.putOpt("request", "create");
           // msg.putOpt("room", room);
            msg.putOpt("description","description of room");
            msg.putOpt("secret", XUtils.ROOMS_JANUS_PASSWORD);
            msg.putOpt("pin", XUtils.ROOMS_JANUS_PASSWORD);
        } catch (JSONException e) {
           e.printStackTrace();
        }
        webSocketChannel.sendMessage(msg.toString());
    }


    public void createOffer(BigInteger handleId, RTCSessionDescription sdp) {
        JSONObject message = new JSONObject();
        try {
            JSONObject publish = new JSONObject();
            publish.putOpt("audio", true);
            publish.putOpt("video", true);

            JSONObject jsep = new JSONObject();
            jsep.putOpt("type", sdp.sdpType);
            jsep.putOpt("sdp", sdp.sdp);

            message.putOpt("janus", "message");
            message.putOpt("body", publish);
            message.putOpt("jsep", jsep);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketChannel.sendMessage(message.toString());
    }

    /**
     * @param subscriptionHandleId
     * @param sdp
     */
    public void subscriptionStart(BigInteger subscriptionHandleId, int roomId, RTCSessionDescription sdp) {
        JSONObject message = new JSONObject();
        try {
            JSONObject body = new JSONObject();
            body.putOpt("request", "start");
            body.putOpt("room", roomId);
            message.putOpt("janus", "message");
            message.putOpt("body", body);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", subscriptionHandleId);

            if (sdp != null) {
                JSONObject jsep = new JSONObject();
                jsep.putOpt("type", sdp.sdpType);
                jsep.putOpt("sdp", sdp.sdp);
                message.putOpt("jsep", jsep);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketChannel.sendMessage(message.toString());
    }

    public void publish(BigInteger handleId, RTCSessionDescription sdp) {
        JSONObject message = new JSONObject();
        try {
            JSONObject publish = new JSONObject();
            publish.putOpt("request", "publish");
            publish.putOpt("audio", true);
            publish.putOpt("video", true);

            JSONObject jsep = new JSONObject();
            jsep.putOpt("type", sdp.sdpType);
            jsep.putOpt("sdp", sdp.sdp);

            message.putOpt("janus", "message");
            message.putOpt("body", publish);
            message.putOpt("jsep", jsep);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketChannel.sendMessage(message.toString());
    }

    /**
     * @param roomId
     * @param feedId
     */
    public void subscribe(BigInteger subscriptionHandleId, int roomId, BigInteger feedId) {
        JSONObject message = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            body.putOpt("ptype", "subscriber");
            body.putOpt("request", "join");
            body.putOpt("room", roomId);
            body.putOpt("feed", feedId);

            message.put("body", body);
            message.putOpt("janus", "message");
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", subscriptionHandleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketChannel.sendMessage(message.toString());
    }

    public void trickleCandidate(BigInteger handleId, RTCIceCandidate iceCandidate) {
        JSONObject candidate = new JSONObject();
        JSONObject message = new JSONObject();
        try {
            candidate.putOpt("candidate", iceCandidate.sdp);
            candidate.putOpt("sdpMid", iceCandidate.sdpMid);
            candidate.putOpt("sdpMLineIndex", iceCandidate.sdpMLineIndex);

            message.putOpt("janus", "trickle");
            message.putOpt("candidate", candidate);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webSocketChannel.sendMessage(message.toString());
    }

    public void trickleCandidateComplete(BigInteger handleId) {
        JSONObject candidate = new JSONObject();
        JSONObject message = new JSONObject();
        try {
            candidate.putOpt("completed", true);

            message.putOpt("janus", "trickle");
            message.putOpt("candidate", candidate);
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        webSocketChannel.sendMessage(message.toString());
    }

    public void joinRoom(BigInteger handleId, int roomId, String displayName) {
        JSONObject message = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            body.putOpt("display", displayName);
            body.putOpt("ptype", "publisher");
            body.putOpt("request", "join");
            body.putOpt("room", roomId);
            message.put("body", body);

            message.putOpt("janus", "message");
            message.putOpt("transaction", randomString(12));
            message.putOpt("session_id", sessionId);
            message.putOpt("handle_id", handleId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketChannel.sendMessage(message.toString());
    }

    @Override
    public void onOpen() {
        createSession();
    }

    @Override
    public void onMessage(String message) {

        try {
            JSONObject obj = new JSONObject(message);
            JanusMessageType type = JanusMessageType.fromString(obj.getString("janus"));
            String transaction = null;
            BigInteger sender = null;
            if (obj.has("transaction")) {
                transaction = obj.getString("transaction");
            }
            if (obj.has("sender")) {
                sender = new BigInteger(String.valueOf(obj.getBigInteger("sender")));
            }
            PluginHandle handle = null;
            if (sender != null) {
                handle = attachedPlugins.get(sender);
            }
            System.out.println(" Event type - onMessage:::  " + type);
            switch (type) {
                case keepalive:
                    break;
                case ack:
                    break;
                case success:
                    if (transaction != null) {
                        Transaction cb = transactions.get(transaction);
                        if (cb != null) {
                            try {
                                if (cb.getFeedId() != null) {
                                    cb.onSuccess(obj, cb.getFeedId());
                                } else {
                                    cb.onSuccess(obj);
                                }
                                transactions.remove(transaction);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case error: {
                    if (transaction != null) {
                        Transaction cb = transactions.get(transaction);
                        if (cb != null) {
                            cb.onError();
                            transactions.remove(transaction);
                        }
                    }
                    break;
                }
                case hangup: {
                    break;
                }
                case detached: {
                    if (handle != null) {
                        if (janusCallback != null) {
                            janusCallback.onDetached(handle.getHandleId());
                        }
                    }
                    break;
                }
                case event: {
                    if (handle != null) {
                        JSONObject plugin_data = null;
                        if (obj.has("plugindata")) {
                            plugin_data = obj.getJSONObject("plugindata");
                        }
                        if (plugin_data != null) {
                            JSONObject data = null;
                            JSONObject jsep = null;
                            if (plugin_data.has("data")) {
                                data = plugin_data.getJSONObject("data");
                            }
                            if (obj.has("jsep")) {
                                jsep = obj.getJSONObject("jsep");
                            }
                            if (janusCallback != null) {
                                System.err.println("panoo 09 - : " + obj.toString());
                                janusCallback.onMessage(sender, handle.getHandleId(), data, jsep);
                            }
                        }
                    }
                }
                case trickle:
                    if (handle != null) {
                        if (obj.has("candidate")) {
                            JSONObject candidate = obj.getJSONObject("candidate");
                            if (janusCallback != null) {
                                janusCallback.onIceCandidate(handle.getHandleId(), candidate);
                            }
                        }
                    }
                    break;
                case destroy:
                    if (janusCallback != null) {
                        janusCallback.onDestroySession(sessionId);
                    }
                    break;
            }
        } catch (JSONException ex) {
            System.err.println("Exception " + ex);
            if (janusCallback != null) {
                janusCallback.onError(ex.getMessage());
            }
        }
    }


    private void startKeepAliveTimer() {
        isKeepAliveRunning = true;
        keepAliveThread = new Thread(() -> {
            while (isKeepAliveRunning && !Thread.interrupted()) {
                try {
                    Thread.sleep(25000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                if (webSocketChannel != null && webSocketChannel.isConnected()) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("janus", "keepalive");
                        obj.put("session_id", sessionId);
                        obj.put("transaction", randomString(12));
                        webSocketChannel.sendMessage(obj.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {

                    System.out.println("keepAlive failed websocket is null or not connected");
                }
            }

            System.out.println("keepAlive thread stopped");
        }, "KeepAlive");
        keepAliveThread.start();
    }

    private void stopKeepAliveTimer() {
        isKeepAliveRunning = false;
        if (keepAliveThread != null) {
            keepAliveThread.interrupt();
        }
    }

    @Override
    public void onClosed() {
        stopKeepAliveTimer();
    }

    public String randomString(int length) {
        String str = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(str.charAt(random.nextInt(str.length())));
        }
        return sb.toString();
    }


}

