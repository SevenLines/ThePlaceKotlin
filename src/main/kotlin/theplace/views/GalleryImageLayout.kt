package theplace.views

import javafx.animation.Animation
import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import javafx.event.EventHandler
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import theplace.parsers.elements.GalleryImage
import tornadofx.Fragment
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.prefs.Preferences

/**
 * Created by mk on 03.04.16.
 */
class GalleryImageLayout(val img: GalleryImage,
                         var isDownloadable: Boolean = true,
                         var isFitToParent: Boolean = false) : Fragment() {
    override val root: AnchorPane by fxml()
    val image: ImageView by fxid()
    val overlayPane: BorderPane by fxid()
    val imageContainer: AnchorPane by fxid()

    var img_data: InputStream? = null
    var iconDownload: ImageView = ImageView(imageDownload)
    var iconRemove: ImageView = ImageView(imageRemove)
    var iconLoading: ImageView = ImageView(imageLoading)
    var isDownloading = false
    var slideTransition = TranslateTransition(Duration(300.0), overlayPane)
    var downloadableProperty = SimpleBooleanProperty(isDownloadable)
    var fitToParentProperty = SimpleBooleanProperty(isFitToParent)
    var onImageClick: EventHandler<MouseEvent>? = null

    companion object {
        @JvmStatic val CURRENT_IMAGE = SimpleObjectProperty<Fragment>()
        @JvmStatic val imageDownload = Image(GalleryImageLayout::class.java.getResourceAsStream("images/download.png"))
        @JvmStatic val imageRemove = Image(GalleryImageLayout::class.java.getResourceAsStream("images/Trash.png"))
        @JvmStatic val imageLoading = Image(GalleryImageLayout::class.java.getResourceAsStream("images/loading.gif"))
        @JvmStatic val CACHE_DIR = "./"
    }

    fun update_interface(check_exists: Boolean = false) {
        if (check_exists) {
            var path = img.exists(Paths.get(savePath()))
            if (path != null) {
                root.styleClass.clear()
                root.styleClass.add("exists")
            } else {
                root.styleClass.clear()
            }
            overlayPane.center = if (path!=null) iconRemove else iconDownload
        }

        if (!isDownloading) {
            if (CURRENT_IMAGE.value != this) {
                slideTransition.rate = -1.0
                slideTransition.setOnFinished { overlayPane.isVisible = false }
                slideTransition.play()
                return
            } else {
                overlayPane.isVisible = true
                slideTransition.setOnFinished {}
                slideTransition.rate = 1.0
                slideTransition.play()
            }
        }
    }

    fun savePath() = Preferences.userRoot().get("savepath", ".")
    fun  updateFitToParentProperty(new: Boolean) {
        if (new) {
            image.fitHeightProperty().bind(imageContainer.prefHeightProperty())
            image.fitWidthProperty().bind(imageContainer.prefWidthProperty())
        } else {
            image.fitHeightProperty().unbind()
            image.fitWidthProperty().unbind()
        }
    }

    init {
        var clipRect = Rectangle(root.width, root.height)
        clipRect.heightProperty().bind(root.heightProperty())
        clipRect.widthProperty().bind(root.widthProperty())
        root.clip = clipRect

        overlayPane.center = iconLoading
        slideTransition.interpolator = Interpolator.EASE_OUT
        slideTransition.fromYProperty().bind(root.heightProperty())
        slideTransition.toYProperty().bind(root.heightProperty().subtract(overlayPane.heightProperty()).subtract(3))

        fitToParentProperty.addListener({ obs, old, new ->
            updateFitToParentProperty(new)
        })
        updateFitToParentProperty(isFitToParent)


        background {
            img_data = img.downloadThumb()?.body
        } ui {
            if (img_data!=null) {
                image.image = Image(img_data)
                overlayPane.layoutX = root.height
            } else {
                image.image = null
            }
        }

        CURRENT_IMAGE.addListener({ obs ->
            if (downloadableProperty.value) {
                update_interface()
            }
        })

        root.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, {
            if (downloadableProperty.value) {
                CURRENT_IMAGE.set(this)
            }
        })

        listOf(iconRemove, iconDownload, iconLoading).forEach {
            it.isPreserveRatio = true
            it.isSmooth = true
        }

        overlayPane.onMouseClicked = EventHandler {
            if (downloadableProperty.value) {
                var dir_path = savePath()
                if (it.button == MouseButton.PRIMARY) {
                    overlayPane.center = iconLoading
                    isDownloading = true
                    background {
                        var path = img.exists(Paths.get(dir_path))
                        if (path!=null) {
                            path.toFile().delete()
                        } else {
                            img.saveToPath(Paths.get(dir_path))
                        }
                    } ui {
                        isDownloading = false
                        update_interface(true)
                    }
                }
            }
        }
        image.onMouseClicked = EventHandler { onImageClick?.handle(it) }
        update_interface(true)
    }
}