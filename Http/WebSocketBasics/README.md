### client dependency
```kotlin
implementation 'com.squareup.okhttp3:okhttp:x.x.x'
```
### preparing client and listner
```kotlin
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder().url("wss://your-websocket-url.com").build();
WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        // WebSocket connection opened
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        // Received message from WebSocket server
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        // WebSocket connection is about to close
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        // WebSocket connection closed
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        // WebSocket connection failed
    }
});
```

### sending message
```kotlin
webSocket.send("Hello, WebSocket server!");
```

### closing connection
```kotlin
webSocket.close(1000, "Goodbye, WebSocket server!");
```
