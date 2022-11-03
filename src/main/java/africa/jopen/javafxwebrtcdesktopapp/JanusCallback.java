package africa.jopen.javafxwebrtcdesktopapp;

import org.json.JSONObject;

import java.math.BigInteger;

public interface JanusCallback {
    void onCreateSession(BigInteger sessionId);

    void onAttached(BigInteger handleId);

    /**
     *
     * @param subscribeHandleId HandlerId
     * @param feedId            feedId
     */
    void onSubscribeAttached(BigInteger subscribeHandleId, BigInteger feedId);

    void onDetached(BigInteger handleId);

    void onHangup(BigInteger handleId);

    void onMessage(BigInteger sender, BigInteger handleId, JSONObject msg, JSONObject jsep);

    void onIceCandidate(BigInteger handleId, JSONObject candidate);

    void onDestroySession(BigInteger sessionId);

    void onError(String error);
}
