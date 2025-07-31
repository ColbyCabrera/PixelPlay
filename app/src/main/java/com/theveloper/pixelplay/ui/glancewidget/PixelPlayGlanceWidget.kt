package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.datastore.preferences.protobuf.ByteString // No longer needed
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.data.model.PlayerInfo // Changed import
import com.theveloper.pixelplay.R
import androidx.core.graphics.scale
import com.theveloper.pixelplay.data.model.QueueItem
import com.theveloper.pixelplay.utils.createScalableBackgroundBitmap
import timber.log.Timber

class PixelPlayGlanceWidget : GlanceAppWidget() {

    companion object {
        // Tamaños definidos para diferentes configuraciones del widget
        private val VERY_THIN_LAYOUT_SIZE = DpSize(width = 200.dp, height = 60.dp)
        private val THIN_LAYOUT_SIZE = DpSize(width = 250.dp, height = 80.dp)
        private val SMALL_HORIZONTAL_LAYOUT_SIZE = DpSize(width = 110.dp, height = 60.dp)
        private val ONE_BY_ONE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 110.dp)
        private val GABE_LAYOUT_SIZE = DpSize(width = 110.dp, height = 220.dp)
        private val GABE_TWO_HEIGHT_LAYOUT_SIZE = DpSize(width = 110.dp, height = 200.dp)
        private val SMALL_LAYOUT_SIZE = DpSize(width = 120.dp, height = 100.dp)
        private val MEDIUM_LAYOUT_SIZE = DpSize(width = 250.dp, height = 150.dp)
        private val LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 180.dp)
        private val EXTRA_LARGE_LAYOUT_SIZE = DpSize(width = 300.dp, height = 220.dp)

        // LruCache for Bitmaps
        private object AlbumArtBitmapCache {
            private const val CACHE_SIZE_BYTES = 4 * 1024 * 1024 // 4 MiB
            private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    return value.byteCount
                }
            }

            fun getBitmap(key: String): Bitmap? = lruCache.get(key)

            fun putBitmap(key: String, bitmap: Bitmap) {
                if (getBitmap(key) == null) {
                    lruCache.put(key, bitmap)
                }
            }

            fun getKey(byteArray: ByteArray): String {
                return byteArray.contentHashCode().toString()
            }
        }
    }

    override val sizeMode = SizeMode.Exact
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
    private fun WidgetUi(
        playerInfo: PlayerInfo,
        size: DpSize,
        context: Context
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

        val baseModifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>())

        Box(
            GlanceModifier.fillMaxSize()
        ) {
            val isOneColumn = size.width < SMALL_LAYOUT_SIZE.width
            val isSmallHeight = size.height < SMALL_LAYOUT_SIZE.height

            if (isOneColumn) {
                when {
                    size.height <= ONE_BY_ONE_LAYOUT_SIZE.height -> OneByOneWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        isPlaying = isPlaying
                    )
                    size.height <= GABE_TWO_HEIGHT_LAYOUT_SIZE.height -> GabeTwoHeightWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        isPlaying = isPlaying,
                        context = context
                    )
                    else -> GabeWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 360.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        isPlaying = isPlaying,
                        context = context
                    )
                }
            } else if (isSmallHeight) {
                when {
                    size.width < VERY_THIN_LAYOUT_SIZE.width -> SmallHorizontalWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        isPlaying = isPlaying,
                        context = context
                    )
                    size.width < THIN_LAYOUT_SIZE.width -> VeryThinWidgetLayout(
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
                    else -> ThinWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 60.dp,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        isPlaying = isPlaying,
                        textColor = onBackgroundColor,
                        context = context
                    )
                }
            } else {
                when {
                    size.width < MEDIUM_LAYOUT_SIZE.width || size.height < MEDIUM_LAYOUT_SIZE.height -> SmallWidgetLayout(
                        modifier = baseModifier,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        albumArtBitmapData = albumArtBitmapData,
                        isPlaying = isPlaying,
                        context = context
                    )
                    size.width < LARGE_LAYOUT_SIZE.width || size.height < LARGE_LAYOUT_SIZE.height -> MediumWidgetLayout(
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
                    size.width < EXTRA_LARGE_LAYOUT_SIZE.width || size.height < EXTRA_LARGE_LAYOUT_SIZE.height -> LargeWidgetLayout(
                        modifier = baseModifier,
                        title = title,
                        artist = artist,
                        albumArtBitmapData = albumArtBitmapData,
                        backgroundColor = actualBackgroundColor,
                        bgCornerRadius = 28.dp,
                        isPlaying = isPlaying,
                        isFavorite = isFavorite,
                        textColor = onBackgroundColor,
                        context = context
                    )
                    else -> ExtraLargeWidgetLayout(
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
    fun VeryThinWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        artist: String,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp) // Padding applied to the outer box
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {

                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .padding(end = 8.dp, top = 6.dp, bottom = 6.dp),
                    bitmapData = albumArtBitmapData,
                    size = 58.dp,
                    context = context,
                    cornerRadius = bgCornerRadius
                )
                Spacer(GlanceModifier.width(2.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                    if (artist.isNotEmpty() && artist != "Toca para abrir") {
                        Text(text = artist, style = TextStyle(fontSize = 14.sp, color = textColor), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(48.dp, 48.dp)
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
                Spacer(GlanceModifier.width(10.dp))
                NextButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(48.dp, 48.dp)
                        .fillMaxHeight(),
                    iconColor = onSecondaryColor,
                    iconSize = 26.dp,
                    backgroundColor = secondaryColor,
                    cornerRadius = 10.dp
                )
            }
        }
    }


    @Composable
    fun ThinWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp) // Padding applied to the outer box
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius)
                ,
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {

                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, top = 6.dp, bottom = 6.dp),
                    bitmapData = albumArtBitmapData,
                    //size = 68.dp,
                    context = context,
                    cornerRadius = bgCornerRadius
                )
                Spacer(GlanceModifier.width(6.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                    if (artist.isNotEmpty() && artist != "Toca para abrir") {
                        Text(text = artist, style = TextStyle(fontSize = 14.sp, color = textColor), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(48.dp, 48.dp)
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
                Spacer(GlanceModifier.width(10.dp))
                NextButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .size(48.dp, 48.dp)
                        .fillMaxHeight(),
                    iconColor = onSecondaryColor,
                    iconSize = 26.dp,
                    backgroundColor = secondaryColor,
                    cornerRadius = 10.dp
                )
            }
        }
    }

    @Composable
    fun GabeTwoHeightWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {

                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 4.dp),
                    bitmapData = albumArtBitmapData,
                    size = 48.dp,
                    context = context,
                    cornerRadius = 64.dp
                )
                Spacer(GlanceModifier.height(14.dp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(bgCornerRadius)
                ) {
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        backgroundColor = primaryContainerColor,
                        iconColor = onPrimaryContainerColor,
                        isPlaying = isPlaying,
                        iconSize = 26.dp,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.dp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.dp
                    )
                }
            }
        }
    }

    @Composable
    fun GabeWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .padding(horizontal = 4.dp),
                    bitmapData = albumArtBitmapData,
                    size = 48.dp,
                    context = context,
                    cornerRadius = 64.dp
                )
                Spacer(GlanceModifier.height(14.dp))
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .cornerRadius(bgCornerRadius)
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.dp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        backgroundColor = primaryContainerColor,
                        iconColor = onPrimaryContainerColor,
                        isPlaying = isPlaying,
                        iconSize = 26.dp,
                        cornerRadius = 10.dp
                    )
                    Spacer(GlanceModifier.height(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth(),
                        iconColor = onSecondaryColor,
                        iconSize = 26.dp,
                        backgroundColor = secondaryColor,
                        cornerRadius = 10.dp
                    )
                }
            }
        }
    }

    @Composable
    fun OneByOneWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean
    ) {
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            PlayPauseButtonGlance(
                modifier = GlanceModifier.fillMaxSize(),
                backgroundColor = primaryContainerColor,
                iconColor = onPrimaryContainerColor,
                isPlaying = isPlaying,
                iconSize = 36.dp,
                cornerRadius = 30.dp
            )
        }
    }

    @Composable
    fun SmallHorizontalWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        context: Context
    ) {
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(bgCornerRadius),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                AlbumArtImageGlance(
                    modifier = GlanceModifier.padding(6.dp),
                    bitmapData = albumArtBitmapData,
                    size = 58.dp,
                    context = context,
                    cornerRadius = 64.dp
                )
                Spacer(GlanceModifier.width(14.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight(),
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor,
                    isPlaying = isPlaying,
                    iconSize = 26.dp,
                    cornerRadius = 10.dp
                )
            }
        }
    }

    @Composable
    fun SmallWidgetLayout(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
        val buttonCornerRadius = 16.dp
        val playButtonCornerRadius = if (isPlaying) 12.dp else 60.dp

        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(12.dp) // Using 12dp for this smaller layout
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    AlbumArtImageGlance(
                        modifier = GlanceModifier.defaultWeight(),
                        bitmapData = albumArtBitmapData,
                        context = context,
                        cornerRadius = 64.dp
                    )
                    Spacer(GlanceModifier.width(10.dp))
                }
                Spacer(GlanceModifier.height(8.dp))
                PlayPauseButtonGlance(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(50.dp),
                    isPlaying = isPlaying,
                    cornerRadius = playButtonCornerRadius,
                    iconSize = 26.dp,
                    backgroundColor = primaryContainerColor,
                    iconColor = onPrimaryContainerColor
                )
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth()
                        .height(50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconSize = 26.dp,
                        cornerRadius = buttonCornerRadius,
                        backgroundColor = secondaryColor,
                        iconColor = onSecondaryColor
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconSize = 26.dp,
                        cornerRadius = buttonCornerRadius,
                        backgroundColor = secondaryColor,
                        iconColor = onSecondaryColor
                    )
                }
            }
        }
    }

    @Composable
    fun MediumWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        albumArtBitmapData: ByteArray?,
        isPlaying: Boolean,
        textColor: ColorProvider,
        context: Context
    ) {
        val secondaryColor = GlanceTheme.colors.secondaryContainer
        val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
        val primaryContainerColor = GlanceTheme.colors.primaryContainer
        val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
        val buttonCornerRadius = 60.dp
        val playButtonCornerRadius = if (isPlaying) 14.dp else 60.dp

        // *** FIX: Apply padding to the outer Box for consistency ***
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            // *** FIX: Removed padding from the inner Column ***
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top part: Album Art + Title/Artist
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtImageGlance(
                        bitmapData = albumArtBitmapData,
                        size = 80.dp,
                        context = context,
                        cornerRadius = 16.dp
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title,
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            ),
                            maxLines = 2
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = artist,
                            style = TextStyle(fontSize = 13.sp, color = textColor),
                            maxLines = 2
                        )
                    }
                }

                // Spacer to push buttons down
                Spacer(GlanceModifier.height(12.dp))

                // Bottom part: Control Buttons
                Row(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                }
            }
        }
    }

    @Composable
    fun LargeWidgetLayout(
        modifier: GlanceModifier,
        title: String,
        artist: String,
        albumArtBitmapData: ByteArray?,
        backgroundColor: ColorProvider,
        bgCornerRadius: Dp,
        isPlaying: Boolean,
        isFavorite: Boolean,
        textColor: ColorProvider,
        context: Context
    ) {
        // *** FIX: Apply padding to the outer Box for consistency ***
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            // *** FIX: Removed padding from the inner Column ***
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    AlbumArtImageGlance(bitmapData = albumArtBitmapData, size = 64.dp, context = context, cornerRadius = 18.dp)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(text = title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor), maxLines = 1)
                        Text(text = artist, style = TextStyle(fontSize = 13.sp, color = textColor), maxLines = 1)
                    }
                    Spacer(GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(if (isFavorite) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24),
                        contentDescription = "favorite",
                        modifier = GlanceModifier
                            .size(28.dp)
                            .clickable(actionRunCallback<PlayerControlActionCallback>(actionParametersOf(PlayerActions.key to PlayerActions.FAVORITE)))
                            .padding(2.dp),
                        colorFilter = ColorFilter.tint(textColor)
                    )
                    Spacer(GlanceModifier.width(8.dp))
                }
                Spacer(GlanceModifier.height(4.dp))
                // Progress bar commented out as in original code
                Spacer(GlanceModifier.height(10.dp))

                // Control Buttons Row
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val secondaryColor = GlanceTheme.colors.secondaryContainer
                    val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
                    val primaryContainerColor = GlanceTheme.colors.primaryContainer
                    val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
                    val buttonCornerRadius = 60.dp
                    val playButtonCornerRadius = if (isPlaying) 14.dp else 60.dp

                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        cornerRadius = buttonCornerRadius
                    )
                }
            }
        }
    }

    @Composable
    fun ExtraLargeWidgetLayout(
        modifier: GlanceModifier, title: String, artist: String, albumArtBitmapData: ByteArray?,
        isPlaying: Boolean, backgroundColor: ColorProvider, bgCornerRadius: Dp,
        textColor: ColorProvider,
        context: Context,
        queue: List<QueueItem>
    ) {
        val playButtonCornerRadius = if (isPlaying) 12.dp else 60.dp
        // *** FIX: Apply padding to the outer Box for consistency ***
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(bgCornerRadius)
                .padding(16.dp)
        ) {
            // *** FIX: Removed padding from the inner Column ***
            Column(modifier = GlanceModifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                // Top Row: Album Art & Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    AlbumArtImageGlance(
                        bitmapData = albumArtBitmapData,
                        size = 80.dp,
                        context = context,
                        cornerRadius = 16.dp
                    )
                    Spacer(GlanceModifier.width(16.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = title,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor),
                            maxLines = 2
                        )
                        Text(
                            text = artist,
                            style = TextStyle(fontSize = 14.sp, color = textColor),
                            maxLines = 1
                        )
                    }
                }

                // Middle Row: Progress Bar
                Column(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 6.dp)
                ) {
                    Spacer(GlanceModifier.height(2.dp))
                    // Progress bar commented out as in original code
                }

                // Bottom Row: Controls
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val secondaryColor = GlanceTheme.colors.secondaryContainer
                    val onSecondaryColor = GlanceTheme.colors.onSecondaryContainer
                    val primaryContainerColor = GlanceTheme.colors.primaryContainer
                    val onPrimaryContainerColor = GlanceTheme.colors.onPrimaryContainer
                    val buttonCornerRadius = 28.dp

                    PreviousButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        iconSize = 28.dp,
                        cornerRadius = buttonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    PlayPauseButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        isPlaying = isPlaying,
                        iconColor = onPrimaryContainerColor,
                        backgroundColor = primaryContainerColor,
                        iconSize = 30.dp,
                        cornerRadius = playButtonCornerRadius
                    )
                    Spacer(GlanceModifier.width(10.dp))
                    NextButtonGlance(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight(),
                        iconColor = onSecondaryColor,
                        backgroundColor = secondaryColor,
                        iconSize = 28.dp,
                        cornerRadius = buttonCornerRadius
                    )
                }

                Spacer(GlanceModifier.defaultWeight())

                // Queue Row
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(62.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    queue.take(5).forEach { queueItem ->
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Timber.tag("AlbumArtImageGlanceQueue").d("Processing item: $queueItem")
                            AlbumArtImageGlance(
                                modifier = GlanceModifier
                                    .clickable(
                                        actionRunCallback<PlayerControlActionCallback>(
                                            actionParametersOf(
                                                PlayerActions.key to PlayerActions.PLAY_FROM_QUEUE,
                                                PlayerActions.songIdKey to queueItem.id
                                            )
                                        )
                                    ),
                                bitmapData = queueItem.albumArtBitmapData,
                                size = 62.dp,
                                context = context,
                                cornerRadius = 12.dp,
                            )
                        }
                    }
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

        val sizingModifier = if (size != null) modifier.size(size) else modifier.fillMaxSize()

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
                    Timber.tag(TAG_AAIG)
                        .d("Initial bounds: ${options.outWidth}x${options.outHeight}")

                    val imageHeight = options.outHeight
                    val imageWidth = options.outWidth
                    var inSampleSize = 1
                    val targetSizePx = with(context.resources.displayMetrics) { size!!.value * density }.toInt()
                    Timber.tag(TAG_AAIG).d("Target Px size for Dp $size : $targetSizePx")

                    if (imageHeight > targetSizePx || imageWidth > targetSizePx) {
                        val halfHeight: Int = imageHeight / 2
                        val halfWidth: Int = imageWidth / 2
                        while (halfHeight / inSampleSize >= targetSizePx && halfWidth / inSampleSize >= targetSizePx) {
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

                    if (sampledBitmap.width != targetSizePx || sampledBitmap.height != targetSizePx) {
                        Timber.tag(TAG_AAIG)
                            .d("Scaling sampled bitmap from ${sampledBitmap.width}x${sampledBitmap.height} to ${targetSizePx}x${targetSizePx}")
                        val scaledBitmap = sampledBitmap.scale(targetSizePx, targetSizePx)
                        if (scaledBitmap != sampledBitmap) {
                            sampledBitmap.recycle()
                            Timber.tag(TAG_AAIG).d("Recycled intermediate sampledBitmap.")
                        }
                        bitmap = scaledBitmap
                    } else {
                        bitmap = sampledBitmap
                        Timber.tag(TAG_AAIG)
                            .d("No final scaling needed. Using sampled bitmap directly.")
                    }

                    Timber.tag(TAG_AAIG)
                        .d("Final bitmap size: ${bitmap.width}x${bitmap.height}. Putting into cache with key: $cacheKey")
                    bitmap.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }

                } catch (e: Exception) {
                    Timber.tag(TAG_AAIG).e(e, "Error decoding or scaling bitmap: ${e.message}")
                    bitmap = null
                }
            }
            bitmap?.let { ImageProvider(it) }
        } ?: run {
            Timber.tag(TAG_AAIG)
                .d("Using placeholder image because bitmapData was null or processing failed.")
            ImageProvider(R.drawable.rounded_album_24)
        }

        Box(
            modifier = sizingModifier
        ) {
            Image(
                provider = imageProvider,
                contentDescription = "Album Art",
                modifier = GlanceModifier.fillMaxSize().cornerRadius(cornerRadius),
                contentScale = ContentScale.Crop
            )
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
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
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
            modifier = sizeModifier
                .background(ImageProvider(backgroundBitmap))
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
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
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
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
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
}

// Helper para formatear duración en Glance (no puede usar TimeUnit directamente)
private fun formatDurationGlance(millis: Long): String {
    if (millis < 0) return "00:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
