```kotlin
@OptIn(InternalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel
@Inject constructor(
) : ViewModel() {

    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun onConnectError(
        anies: Array<Any>
    ) {
        anies.forEach {
            when (it) {
                is EngineIOException -> {
                    Log.e(
                        "WebSocket",
                        "Socket.IO >> ${it.localizedMessage}",
                        it
                    )
                }

                else -> {
                    Log.e("WebSocket", "Socket.IO >> Un known shit")
                }
            }
        }
    }

    fun onConnect(
        anies: Array<Any>
    ) {
        Log.e("WebSocket", "Socket got connected")
    }

    fun disconnect(
        anies: Array<Any>
    ) {
        Log.e("WebSocket", "Socket got connected")
    }

    init {
        scope.launch {
            suspendCancellableCoroutine {
                var socket: Socket? = null
                runCatching {
                    socket = socket(url, options())?.apply {
                        connect()
                        open()
                        on("connect_error", this@ChatViewModel::onConnectError)
                        on("connect", this@ChatViewModel::onConnect)
                        on("disconnect", this@ChatViewModel::disconnect)
                    }
                }.onFailure {
                    socket?.disconnect()
                }
                it.invokeOnCancellation {
                    socket?.disconnect()
                }
            }

        }
    }

    private fun options(): IO.Options {
        val opts = IO.Options()
        opts.path = "/if/you/have/any"
        opts.transports = arrayOf(WebSocket.NAME)
        return opts
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }
}
```
