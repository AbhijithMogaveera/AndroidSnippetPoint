# GlidePoint
## Cache support from glide for generated place holder
I used this for blurHashImage place holder but for simlification considring alphabet
![image](https://user-images.githubusercontent.com/46936065/211061232-26ddac10-1e17-48d0-a6b5-c1356c3dbed1.png)


```kotlin
        GlideApp
            .with(this)
            .load(
                Alphabet(
                    chars = "K",
                    backgroundColor = "#B125EA".toColorInt(),
                    textColor = Color.BLACK
                )
            ).into(binding.iv1)

        GlideApp
            .with(this)
            .load(andoridLogoPath)
            .circleCrop()
            .thumbnail(
                GlideApp
                    .with(this)
                    .load(
                        Alphabet(
                            chars = "A",
                            backgroundColor = "#3ddc84".toColorInt(),
                            textColor = Color.WHITE
                        )
                    )
            )
            .into(binding.iv2)
```

```kotlin

//build.gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}
dependencies {
    implementation 'com.github.bumptech.glide:glide:4.14.2'
    kapt 'com.github.bumptech.glide:compiler:4.14.2'
}
```

```xml
 <uses-permission android:name="android.permission.INTERNET"/>
    <application
        android:allowBackup="true"
        android:name=".MyApp"/>
```

```kotlin
class MyApp: Application() {
    override fun onCreate() {
        super.onCreate()
        GlideModule.setGlideComponentRegister(AppGlideComponentRegister())
    }
}
```

```kotlin
data class Alphabet(
    val chars:String,
    val backgroundColor:Int,
    val textColor:Int
)
```

```kotlin
class AlphabetDecoder(val context: Context): ResourceDecoder<Alphabet, Bitmap> {

    override fun handles(source: Alphabet, options: Options): Boolean {
        return true
    }

    override fun decode(
        source: Alphabet,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap> {
        return SimpleResource(
            createImageRounded(context = context, width, height, source)
        )
    }

}
```

```kotlin
class AlphabetLoader private constructor() : ModelLoader<Alphabet, Alphabet> {
    override fun buildLoadData(
        blurHash: Alphabet,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Alphabet>? {
        return ModelLoader.LoadData(
            ObjectKey(
                blurHash 
            ), BlurDataFetcher(blurHash)
        )
    }

    override fun handles(blurHash: Alphabet): Boolean {
        return true
    }

    private inner class BlurDataFetcher constructor(blurHash: Alphabet) :
        DataFetcher<Alphabet> {
        private val blurHash: Alphabet
        override fun loadData(
            priority: Priority,
            callback: DataFetcher.DataCallback<in Alphabet?>
        ) {
            callback.onDataReady(blurHash)
        }

        override fun cleanup() {}
        override fun cancel() {}
        override fun getDataClass(): Class<Alphabet> {
            return Alphabet::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }

        init {
            this.blurHash = blurHash
        }
    }

    class Factory : ModelLoaderFactory<Alphabet, Alphabet> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Alphabet, Alphabet> {
            return AlphabetLoader()
        }

        override fun teardown() {}
    }
}
```

```kotlin
class AppGlideComponentRegister: RegisterGlideComponents {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(Alphabet::class.java, Bitmap::class.java, AlphabetDecoder(context))
        registry.prepend(Alphabet::class.java, Alphabet::class.java, AlphabetLoader.Factory())
    }
}
```
```kotlin
@GlideModule
class GlideModule : AppGlideModule() {

    companion object {
        private lateinit var registerGlideComponents: RegisterGlideComponents
        fun setGlideComponentRegister(registerGlideComponents: RegisterGlideComponents){
            if(this::registerGlideComponents.isInitialized)
                throw IllegalStateException("Component already registered")
            this.registerGlideComponents = registerGlideComponents
        }
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        registerGlideComponents.registerComponents(context, glide, registry)
    }
}
```

```kotlin
fun createImageRounded(
    context: Context,
    width: Int,
    height: Int,
    alphabet: Alphabet
): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paintCircle = Paint()
    val paintText = Paint()
    val rect = Rect(0, 0, width, height)
    val rectF = RectF(rect)
    val density = context.resources.displayMetrics.density
    val roundPx = 100 * density
    paintCircle.color = alphabet.backgroundColor
    paintCircle.isAntiAlias = true
    canvas.drawARGB(0, 0, 0, 0)
    paintCircle.style = Paint.Style.FILL
    val strokeWidth = (height * 0.1).toFloat()
    paintCircle.strokeWidth = strokeWidth
    canvas.drawRoundRect(rectF, roundPx, roundPx, paintCircle)
    paintText.color = alphabet.textColor
    val textSize = height - (height * 0.3)
    paintText.textSize = textSize.toFloat()
    val textWidth = paintText.measureText(alphabet.chars)/2
    canvas.drawText(
        alphabet.chars,
        (width/2).toFloat()-textWidth,
        (height/2+(textSize/3.5)).toFloat(),
        paintText
    );
    paintCircle.style = Paint.Style.STROKE
    paintCircle.color = alphabet.textColor
    canvas.drawCircle(rectF.centerX(), rectF.centerY(), (width/2).toFloat()-strokeWidth/2, paintCircle)
    return output
}
```

```kotlin
interface RegisterGlideComponents {
    fun registerComponents(context: Context, glide: Glide, registry: Registry)
}
```
