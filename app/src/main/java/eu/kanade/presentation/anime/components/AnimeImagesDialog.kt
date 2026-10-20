package eu.kanade.presentation.anime.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.updatePadding
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.size.Size
import eu.kanade.presentation.anime.EditCoverAction
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.data.coil.originalCover
import eu.kanade.tachiyomi.data.coil.useBackground
import eu.kanade.tachiyomi.util.ReaderPageImageView
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.anime.model.Anime
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication

@Composable
fun AnimeImagesDialog(
    anime: Anime,
    previewOriginal: Boolean,
    usingOriginal: Boolean,
    onToggleOriginal: () -> Unit,
    onUseOriginal: (Boolean) -> Unit,
    isCustomCover: Boolean,
    // AY -->
    isCustomBackground: Boolean,
    // <-- AY
    snackbarHostState: SnackbarHostState,
    // AY -->
    pagerState: PagerState,
    // <-- AY
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    // AY -->
    val scope = rememberCoroutineScope()
    val isCover = pagerState.currentPage != 1

    val arrowIcon = if (isCover) {
        MaterialSymbols.Rounded.KeyboardArrowRight
    } else {
        MaterialSymbols.Rounded.KeyboardArrowLeft
    }

    val (editImageStringResource, alternateImageStringResource) = if (isCover) {
        MR.strings.action_edit_cover to AYMR.strings.action_edit_background
    } else {
        AYMR.strings.action_edit_background to MR.strings.action_edit_cover
    }

    val onImageSwitchClicked: () -> Unit = {
        scope.launchUI {
            pagerState.animateScrollToPage(1 - pagerState.currentPage)
        }
    }
    // <-- AY

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = Color.Transparent,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .navigationBarsPadding(),
                ) {
                    ActionsPill {
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = stringResource(MR.strings.action_close),
                            )
                        }
                        // AY -->
                        IconButton(onClick = onImageSwitchClicked) {
                            Icon(
                                imageVector = arrowIcon,
                                contentDescription = stringResource(alternateImageStringResource),
                            )
                        }
                        // <-- AY
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ActionsPill {
                        if (isCover) {
                            Box {
                                var coverMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { coverMenu = true }) {
                                    Icon(
                                        MaterialSymbols.Rounded.MoreVert,
                                        contentDescription = stringResource(AMMR.strings.yume_cover_options),
                                    )
                                }
                                DropdownMenu(expanded = coverMenu, onDismissRequest = { coverMenu = false }) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (previewOriginal) {
                                                        AMMR.strings.yume_cover_show_selected
                                                    } else {
                                                        AMMR.strings.yume_cover_show_original
                                                    },
                                                ),
                                            )
                                        },
                                        onClick = {
                                            onToggleOriginal()
                                            coverMenu = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                stringResource(
                                                    if (usingOriginal) {
                                                        AMMR.strings.yume_cover_use_selected
                                                    } else {
                                                        AMMR.strings.yume_cover_use_original
                                                    },
                                                ),
                                            )
                                        },
                                        onClick = {
                                            onUseOriginal(!usingOriginal)
                                            coverMenu = false
                                        },
                                    )
                                }
                            }
                        }
                        AppBarActions(
                            actions = listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_share),
                                    icon = MaterialSymbols.Rounded.Share,
                                    onClick = onShareClick,
                                ),
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_save),
                                    icon = MaterialSymbols.Rounded.Save,
                                    onClick = onSaveClick,
                                ),
                            ),
                        )
                        if (onEditClick != null) {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        // AY -->
                                        if ((isCover && isCustomCover) || (!isCover && isCustomBackground)) {
                                            // <-- AY
                                            expanded = true
                                        } else {
                                            onEditClick(EditCoverAction.EDIT)
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.Rounded.Edit,
                                        contentDescription = stringResource(editImageStringResource),
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    offset = DpOffset(8.dp, 0.dp),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_edit)) },
                                        onClick = {
                                            onEditClick(EditCoverAction.EDIT)
                                            expanded = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_delete)) },
                                        onClick = {
                                            onEditClick(EditCoverAction.DELETE)
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { contentPadding ->
            val statusBarPaddingPx = with(LocalDensity.current) { contentPadding.calculateTopPadding().roundToPx() }
            val bottomPaddingPx = with(LocalDensity.current) { contentPadding.calculateBottomPadding().roundToPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickableNoIndication(onClick = onDismissRequest),
            ) {
                // AY -->
                HorizontalPager(
                    state = pagerState,
                ) { page ->
                    // <-- AY
                    val model = rememberMetadataCover(
                        ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(anime).useBackground(page == 1).originalCover(page == 0 && previewOriginal).build(),
                    ) as ImageRequest
                    val loading = remember { arrayOfNulls<Disposable>(1) }
                    val loadedModel = remember { arrayOfNulls<ImageRequest>(1) }
                    val loadedView = remember { arrayOfNulls<ReaderPageImageView>(1) }
                    DisposableEffect(Unit) { onDispose { loading[0]?.dispose() } }
                    AndroidView(
                        factory = {
                            ReaderPageImageView(it).apply {
                                onViewClicked = onDismissRequest
                                clipToPadding = false
                                clipChildren = false
                            }
                        },
                        update = { view ->
                            val context = view.context
                            val request = model.newBuilder()
                                .size(Size.ORIGINAL)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .target { image ->
                                    val drawable = image.asDrawable(context.resources)
                                    // Copy bitmap in case it came from memory cache
                                    // Because SSIV needs to thoroughly read the image
                                    val copy = (drawable as? BitmapDrawable)
                                        ?.bitmap
                                        ?.copy(Bitmap.Config.HARDWARE, false)
                                        ?.toDrawable(view.context.resources)
                                        ?: drawable
                                    view.setImage(copy, ReaderPageImageView.Config(zoomDuration = 500))
                                }
                                .build()
                            if (loadedModel[0] != model || loadedView[0] !== view) {
                                loading[0]?.dispose()
                                loading[0] = context.imageLoader.enqueue(request)
                                loadedModel[0] = model
                                loadedView[0] = view
                            }
                            view.updatePadding(top = statusBarPaddingPx, bottom = bottomPaddingPx)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionsPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        content()
    }
}
