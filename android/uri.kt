interface DataUrlEncoder<T> {
    fun encode(): String
}

interface DataUrlDecoder<T> {
    fun decode(url: String): T
}

data class UserDetails(
    val username: String,
    val userId: String,
    val images: List<String>,
    val bestFriend: UserDetails?
) : DataUrlEncoder<UserDetails> {

    companion object : DataUrlDecoder<UserDetails> {
        override fun decode(url: String): UserDetails {
            return Uri.parse(url).let { uri: Uri ->
                val userId = uri.pathSegments[0]
                UserDetails(
                    username = uri.getQueryParameter("username")
                        ?: error("Key username is missing"),
                    userId = userId,
                    images = uri.getQueryParameters("images"),
                    bestFriend = uri.getQueryParameter("bestFriend")?.let(Companion::decode)
                )
            }
        }
    }

    override fun encode(): String {
        return "https://www.userdetailcodec.com/${userId}".toUri().buildUpon().apply {
            appendQueryParameter("username", username)
            images.onEach { imageUrl -> appendQueryParameter("images", imageUrl) }
            bestFriend?.let { bestFriend ->
                appendQueryParameter("bestFriend", bestFriend.encode())
            }
        }.build().toString()
    }
}


fun HashMap<String, String>.encodeToUriString(): String {
    return "https://www.encodeToUriString.com/".toUri().buildUpon().apply {
        keys.forEach {
            appendQueryParameter(it, get(it))
        }
    }.build().toString()
}

fun String.toMapFromUriString(): HashMap<String, String> {
    return hashMapOf<String, String>().apply {
        toUri().apply {
            queryParameterNames.filterNotNull().onEach {
                put(it, getQueryParameter(it) ?: "")
            }
        }
    }
}


fun List<String>.encodeToUriString(
): String {
    return "https://www.encodeToUriString.com/".toUri().buildUpon().apply {
        onEach {
            appendQueryParameter("_key", it)
        }
    }.build().toString()
}

fun String.toListFromUriString(): List<String> {
    return Uri.parse(this).let { uri ->
        buildList {
            uri.queryParameterNames.onEach {
                add(uri.getQueryParameter(it) ?: "")
            }
        }
    }
}





Log.e("UrlEncoder", "-----------------------CLASS_BASED-------------------------------")

        val encode = UserDetails(
            username = "android",
            userId = "uid01",
            images = listOf("htttp://www.example.com/img1.jpg"),
            bestFriend = UserDetails(
                username = "android",
                userId = "uid01",
                images = listOf("htttp://www.example.com/img1.jpg"),
                bestFriend = null
            )
        ).encode()
        Log.e(
            "UrlEncoder", "ENCODE = $encode"
        )
        Log.e(
            "UrlEncoder", "DECODE = ${UserDetails.decode(encode)}"
        )

        Log.e("UrlEncoder", "------------------------MAP_BASED------------------------------")
        val hashMap = hashMapOf(
            "key1" to "item1",
            "key2" to "item2"
        )
        val mapInUrl:String = hashMap.encodeToUriString()
        Log.e("UrlEncoder", "Victim $hashMap")
        Log.e(
            "UrlEncoder", "ENCODE = $mapInUrl"
        )
        Log.e(
            "UrlEncoder", "DECODE = ${mapInUrl.toMapFromUriString()}"
        )
        Log.e("UrlEncoder", "------------------------List_BASED-----------------------------")
        val list = listOf(
            "value1",
            "value2"
        )
        val listInUrl:String = list.encodeToUriString()
        Log.e("UrlEncoder", "Victim $list")
        Log.e(
            "UrlEncoder", "ENCODE = $listInUrl"
        )
        Log.e(
            "UrlEncoder", "DECODE = ${listInUrl.toListFromUriString()}"
        )
//-----------------------CLASS_BASED-------------------------------
// Victim UserDetails(username=android, userId=uid01, images=[htttp://www.example.com/img1.jpg], bestFriend=UserDetails(username=android, userId=uid01, images=[htttp://www.example.com/img1.jpg], bestFriend=null))
// ENCODE = https://www.userdetailcodec.com/uid01?username=android&images=htttp%3A%2F%2Fwww.example.com%2Fimg1.jpg&bestFriend=https%3A%2F%2Fwww.userdetailcodec.com%2Fuid01%3Fusername%3Dandroid%26images%3Dhtttp%253A%252F%252Fwww.example.com%252Fimg1.jpg
// DECODE = UserDetails(username=android, userId=uid01, images=[htttp://www.example.com/img1.jpg], bestFriend=UserDetails(username=android, userId=uid01, images=[htttp://www.example.com/img1.jpg], bestFriend=null))

//------------------------MAP_BASED------------------------------
// Victim {key1=item1, key2=item2}
// ENCODE = https://www.encodeToUriString.com/?key1=item1&key2=item2
// DECODE = {key1=item1, key2=item2}

//-----------------------List Based-----------------------------
//Victim [value1, value2]
//ENCODE = https://www.encodeToUriString.com/?_key=value1&_key=value2
//DECODE = [value1]
