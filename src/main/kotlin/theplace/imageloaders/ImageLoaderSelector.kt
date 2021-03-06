package theplace.imageloaders

import com.mashape.unirest.http.Unirest
import org.reflections.Reflections
import java.io.InputStream
import java.net.URL

/**
 * Created by mk on 10.04.16.
 */
object  ImageLoaderSelector {
    val reflection = Reflections("theplace.imageloaders")

    val loaders = reflection.getSubTypesOf(BaseImageLoaderInterface::class.java).map {
        it.getConstructor().newInstance()
    }

    fun getLoader(url: String): BaseImageLoaderInterface? {
        return loaders.find { it.checkUrl(url) }
    }

    fun getTitle(url: String): String {
        var loader = getLoader(url)
        return loader?.getTitle(url) ?: URL(url).file
    }

    fun download(url: String): LoadedImage? {
        var loader = getLoader(url)
        return loader?.download(url) ?: LoadedImage(url, Unirest.get("$url").header("referer", "$url").asBinary().body)
    }
}