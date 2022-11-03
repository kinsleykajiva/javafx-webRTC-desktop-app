package africa.jopen.javafxwebrtcdesktopapp;


import okhttp3.*;


public class WebSocketChannel {

    private WebSocket webSocket;
    private boolean connected;
    private WebSocketCallback webSocketCallback;

    public void connect(String url) {
        OkHttpClient client = new OkHttpClient();
        /* WebSocket
         * Response
         * Sec-WebSocket-Protocol=janus-protocol"
         */
        Request request = new Request.Builder()
                .header("Sec-WebSocket-Protocol", "janus-protocol")
                .url(url)
                .build();
        webSocket = client.newWebSocket(request, new WebSocketHandler());
    }

    public boolean isConnected() {
        return connected;
    }

    public void sendMessage(String message) {
        if (webSocket != null && connected) {

           System.out.println("send==>>" + message);
           // System.out.println("send==>>" + message);
            webSocket.send(message);
        } else {

            System.out.println("send failed socket not connected");
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "manual close");
            webSocket = null;
        }
    }

    private class WebSocketHandler extends WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            connected = true;

            System.out.println("onOpen");
            if (webSocketCallback != null) {
                System.out.println("onOpen111");
                webSocketCallback.onOpen();
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {

            System.err.println("onMessage " + text);
            if (webSocketCallback != null) {
                webSocketCallback.onMessage(text);
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {

            System.out.println("onClosed " + reason);
            connected = false;
            if (webSocketCallback != null) {
                webSocketCallback.onClosed();
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {

            System.out.println("onFailure " + t.getMessage());
            connected = false;
            if (webSocketCallback != null) {
                webSocketCallback.onClosed();
            }
        }
    }

    public void setWebSocketCallback(WebSocketCallback webSocketCallback) {
        this.webSocketCallback = webSocketCallback;
    }

    public interface WebSocketCallback {
        void onOpen();

        void onMessage(String text);

        void onClosed();
    }
}
