```kotlin
class FlowStreamAdapter<T> : StreamAdapter<T, Flow<T>> {
    override fun adapt(stream: Stream<T>) = callbackFlow<T> {
        stream.start(object : Stream.Observer<T> {
            override fun onComplete() {
                close()
            }

            override fun onError(throwable: Throwable) {
                close(cause = throwable)
            }

            override fun onNext(data: T) {
                if (!isClosedForSend) trySend(data).isSuccess
            }
        })
        awaitClose {}
    }

    object Factory : StreamAdapter.Factory {
        override fun create(type: Type): StreamAdapter<Any, Any> {
            return when (type.getRawType()) {
                Flow::class.java -> FlowStreamAdapter()
                else -> throw IllegalArgumentException()
            }
        }
    }
}
```
```kotlin
interface ChannelSocket {

    @Receive
    fun observeEvent(): Flow<WebSocket.Event>

    @Send
    fun sendSubscribe(
       subscriptionMessage:Any 
    )

}


```
```kotlin
val scarletVersion = "0.1.12"
implementation("com.tinder.scarlet:stream-adapter-coroutines:0.1.10")
implementation ("com.tinder.scarlet:scarlet:$scarletVersion") {
    exclude(group = "com.github.tinder.scarlet", module = "scarlet-core")
}
implementation("com.tinder.scarlet:message-adapter-gson:$scarletVersion")
implementation("com.tinder.scarlet:websocket-okhttp:0.1.8")
```


```kotlin
   kapt{
        correctErrorTypes = true
    }
```

```kotlin
 val client: OkHttpClient = OkHttpClient.Builder().apply {
            providesBodyLogger()
        }.build()
Scarlet.Builder()
            .webSocketFactory(
                client.newWebSocketFactory(
                    url = url
                )
            )
             .addMessageAdapterFactory(GsonMessageAdapter.Factory())
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .build()
            .create<ChannelSocket>()
```

```kotlin
class AnyLifeCycleBasedClass
constructor(
    val channelSocket: ChannelSocket
)  {

    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        channelSocket
            .observeEvent()
            .onEach {
                when (it) {
                    is WebSocket.Event.OnConnectionOpened<*> -> {
                        Log.e("WebSocket", "OnConnectionOpened<*> << ${it.webSocket}")
                    }

                    is WebSocket.Event.OnMessageReceived -> {
                        Log.e("WebSocket", "OnMessageReceived << $it")
                    }

                    is WebSocket.Event.OnConnectionClosing -> {
                        Log.e("WebSocket", "OnConnectionClosing << $it")
                    }

                    is WebSocket.Event.OnConnectionClosed -> {
                        Log.e("WebSocket", "OnConnectionClosed << $it")
                    }

                    is WebSocket.Event.OnConnectionFailed -> {
                        Log.e("WebSocket", "OnConnectionFailed <<", it.throwable)
                    }
                }
            }.launchIn(scope = scope)


    }

    fun onCleared() {
        scope.cancel()
    }
}
```
