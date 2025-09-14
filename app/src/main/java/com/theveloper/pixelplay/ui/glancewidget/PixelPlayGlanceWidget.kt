package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.PlayerInfo
import com.theveloper.pixelplay.ui.glancewidget.layouts.ExtraLargeWidgetLayout
import com.theveloper.pixelplay.ui.glancewidget.layouts.LargeWidgetLayout
import com.theveloper.pixelplay.ui.glancewidget.layouts.MediumWidgetLayout
import com.theveloper.pixelplay.ui.glancewidget.layouts.OneByOneWidgetLayout
import com.theveloper.pixelplay.ui.glancewidget.layouts.SmallWidgetLayout
import com.theveloper.pixelplay.ui.glancewidget.sizing.WidgetSize
import com.theveloper.pixelplay.ui.glancewidget.sizing.getWidgetSize
import com.theveloper.pixelplay.utils.createScalableBackgroundBitmap
import timber.log.Timber

class PixelPlayGlanceWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            val currentSize = LocalSize.current

            Timber.tag("PixelPlayGlanceWidget")
                .d("Providing Glance. PlayerInfo: title='${playerInfo.songTitle}', artist='${playerInfo.artistName}', isPlaying=${playerInfo.isPlaying}, hasBitmap=${playerInfo.albumArtBitmapData != null}, progress=${playerInfo.currentPositionMs}/${playerInfo.totalDurationMs}")

            GlanceTheme {
                WidgetUi(playerInfo = playerInfo, size = currentSize, context = context)
            }
        }
    }

    @Composable
    fun WidgetUi(
        playerInfo: PlayerInfo, size: DpSize, context: Context
    ) {
        val title = playerInfo.songTitle.ifEmpty { "PixelPlay" }
        val artist = playerInfo.artistName.ifEmpty { "Toca para abrir" }
        val isPlaying = playerInfo.isPlaying
        val isFavorite = playerInfo.isFavorite
        val albumArtBitmapData = playerInfo.albumArtBitmapData

        Timber.tag("PixelPlayGlanceWidget")
            .d("WidgetUi: PlayerInfo received. Title: $title, Artist: $artist, HasBitmapData: ${albumArtBitmapData != null}, BitmapDataSize: ${albumArtBitmapData?.size ?: "N/A"}")

        val actualBackgroundColor = GlanceTheme.colors.surface
        val onBackgroundColor = GlanceTheme.colors.onSurface

        val baseModifier =
            GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>())

        Box(
            GlanceModifier.fillMaxSize()
        ) {
            when (getWidgetSize(size)) {
                WidgetSize.TINY -> OneByOneWidgetLayout(
                    modifier = baseModifier,
                    backgroundColor = actualBackgroundColor,
                    bgCornerRadius = 100.dp,
                    isPlaying = isPlaying
                )

                WidgetSize.SMALL -> SmallWidgetLayout(
                    modifier = baseModifier,
                    backgroundColor = actualBackgroundColor,
                    bgCornerRadius = 28.dp,
                    albumArtBitmapData = albumArtBitmapData,
                    isPlaying = isPlaying,
                    context = context
                )

                WidgetSize.MEDIUM -> MediumWidgetLayout(
                    modifier = baseModifier,
                    title = title,
                    artist = artist,
                    albumArtBitmapData = albumArtBitmapData,
                    isPlaying = isPlaying,
                    textColor = onBackgroundColor,
                    context = context,
                    backgroundColor = actualBackgroundColor,
                    bgCornerRadius = 28.dp
                )

                WidgetSize.LARGE -> LargeWidgetLayout(
                    modifier = baseModifier,
                    title = title,
                    artist = artist,
                    albumArtBitmapData = albumArtBitmapData,
                    isPlaying = isPlaying,
                    isFavorite = isFavorite,
                    backgroundColor = actualBackgroundColor,
                    bgCornerRadius = 28.dp,
                    textColor = onBackgroundColor,
                    context = context
                )

                WidgetSize.EXTRA_LARGE -> ExtraLargeWidgetLayout(
                    modifier = baseModifier,
                    title = title,
                    artist = artist,
                    albumArtBitmapData = albumArtBitmapData,
                    isPlaying = isPlaying,
                    backgroundColor = actualBackgroundColor,
                    bgCornerRadius = 28.dp,
                    textColor = onBackgroundColor,
                    context = context,
                    queue = playerInfo.queue
                )
            }
        }
    }
}

@Composable
fun AlbumArtImageGlance(
    bitmapData: ByteArray?,
    size: Dp? = null,
    context: Context,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 16.dp
) {
    val TAG_AAIG = "AlbumArtImageGlance"
    Timber.tag(TAG_AAIG)
        .d("Init. bitmapData is null: ${bitmapData == null}. Requested Dp size: $size")
    if (bitmapData != null) Timber.tag(TAG_AAIG).d("bitmapData size: ${bitmapData.size} bytes")

    val sizingModifier = if (size != null) modifier.size(size) else modifier
    val widgetDpSize = LocalSize.current // Get the actual size of the composable

    val imageProvider = bitmapData?.let { data ->
        val cacheKey = AlbumArtBitmapCache.getKey(data)
        var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)

        if (bitmap != null) {
            Timber.tag(TAG_AAIG).d("Bitmap cache HIT for key: $cacheKey. Using cached bitmap.")
        } else {
            Timber.tag(TAG_AAIG).d("Bitmap cache MISS for key: $cacheKey. Decoding new bitmap.")
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(data, 0, data.size, options)
                Timber.tag(TAG_AAIG).d("Initial bounds: ${options.outWidth}x${options.outHeight}")

                val imageHeight = options.outHeight
                val imageWidth = options.outWidth
                var inSampleSize = 1

                // Determine target size in pixels
                val targetWidthPx: Int
                val targetHeightPx: Int
                with(context.resources.displayMetrics) {
                    if (size != null) {
                        // If size is provided, use it for both width and height (maintains square logic)
                        val targetSizePx = (size.value * density).toInt()
                        targetWidthPx = targetSizePx
                        targetHeightPx = targetSizePx
                        Timber.tag(TAG_AAIG).d("Target Px size for Dp $size: $targetSizePx")
                    } else {
                        // If size is not provided, use the actual widget size
                        targetWidthPx = (widgetDpSize.width.value * density).toInt()
                        targetHeightPx = (widgetDpSize.height.value * density).toInt()
                        Timber.tag(TAG_AAIG)
                            .d("Target Px size from widget DpSize ${widgetDpSize}: ${targetWidthPx}x${targetHeightPx}")
                    }
                }

                if (imageHeight > targetHeightPx || imageWidth > targetWidthPx) {
                    val halfHeight: Int = imageHeight / 2
                    val halfWidth: Int = imageWidth / 2
                    // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                    // height and width larger than the requested height and width.
                    while (halfHeight / inSampleSize >= targetHeightPx && halfWidth / inSampleSize >= targetWidthPx) {
                        inSampleSize *= 2
                    }
                }
                Timber.tag(TAG_AAIG).d("Calculated inSampleSize: $inSampleSize")

                options.inSampleSize = inSampleSize
                options.inJustDecodeBounds = false
                val sampledBitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

                if (sampledBitmap == null) {
                    Timber.tag(TAG_AAIG)
                        .e("BitmapFactory.decodeByteArray returned null after sampling.")
                    return@let null
                }
                Timber.tag(TAG_AAIG)
                    .d("Sampled bitmap size: ${sampledBitmap.width}x${sampledBitmap.height}")

                bitmap = sampledBitmap

                Timber.tag(TAG_AAIG)
                    .d("Final bitmap size: ${bitmap.width}x${bitmap.height}. Putting into cache with key: $cacheKey")
                bitmap.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }

            } catch (e: Exception) {
                Timber.tag(TAG_AAIG).e(e, "Error decoding or scaling bitmap: ${e.message}")
                bitmap = null
            }
        }
        bitmap?.let { ImageProvider(it) }
    }

    Box(
        modifier = sizingModifier
    ) {
        if (imageProvider != null) {
            Image(
                provider = imageProvider,
                contentDescription = "Album Art",
                modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder with tint
            Image(
                provider = ImageProvider(R.drawable.rounded_album_24),
                contentDescription = "Album Art Placeholder",
                modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
            )
        }
    }
}

@Composable
fun PlayPauseButtonGlance(
    modifier: GlanceModifier = GlanceModifier,
    isPlaying: Boolean,
    iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
    backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 0.dp
) {
    val params = actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
    Box(
        modifier = modifier.background(backgroundColor).cornerRadius(cornerRadius)
            .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(if (isPlaying) R.drawable.rounded_pause_24 else R.drawable.rounded_play_arrow_24),
            contentDescription = if (isPlaying) "Pause" else "Play",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Composable
fun NextButtonDGlance(
    modifier: GlanceModifier = GlanceModifier,
    width: Dp? = null,
    height: Dp? = null,
    backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
    iconProvider: ImageProvider = ImageProvider(R.drawable.rounded_skip_next_24),
    iconSize: Dp = 24.dp,
    iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
    topLeftCorner: Dp = 8.dp,
    topRightCorner: Dp = 8.dp,
    bottomLeftCorner: Dp = 8.dp,
    bottomRightCorner: Dp = 8.dp,
) {
    val context = LocalContext.current
    val bgColor = backgroundColor.getColor(context)
    val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)

    val sizeModifier = modifier.then(
        when {
            width != null && height != null -> GlanceModifier.size(width, height)
            width != null -> GlanceModifier.width(width)
            height != null -> GlanceModifier.height(height)
            else -> GlanceModifier
        }
    )

    val backgroundBitmap = createScalableBackgroundBitmap(
        context = context,
        color = bgColor,
        topLeft = topLeftCorner,
        topRight = topRightCorner,
        bottomLeft = bottomLeftCorner,
        bottomRight = bottomRightCorner,
        width = width,
        height = height
    )

    Box(
        modifier = sizeModifier.background(ImageProvider(backgroundBitmap))
            .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = iconProvider,
            contentDescription = "Next",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Composable
fun NextButtonGlance(
    modifier: GlanceModifier = GlanceModifier,
    iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
    backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 8.dp
) {
    val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
    Box(
        modifier = modifier.background(backgroundColor).cornerRadius(cornerRadius)
            .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.rounded_skip_next_24),
            contentDescription = "Next",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Composable
fun PreviousButtonGlance(
    modifier: GlanceModifier = GlanceModifier,
    iconColor: ColorProvider = GlanceTheme.colors.onSurfaceVariant,
    backgroundColor: ColorProvider = GlanceTheme.colors.surfaceVariant,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 8.dp
) {
    val params = actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
    Box(
        modifier = modifier.background(backgroundColor).cornerRadius(cornerRadius)
            .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.rounded_skip_previous_24),
            contentDescription = "Previous",
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(iconColor)
        )
    }
}

@Composable
fun EndOfQueuePlaceholder(
    modifier: GlanceModifier = GlanceModifier, size: Dp, cornerRadius: Dp
) {
    Box(
        modifier = modifier.size(size).background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(cornerRadius)
    ) {

    }
}


// Helper para formatear duración en Glance (no puede usar TimeUnit directamente)
private fun formatDurationGlance(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
