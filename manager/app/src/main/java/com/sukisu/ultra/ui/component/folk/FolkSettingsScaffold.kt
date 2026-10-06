package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sukisu.ultra.ui.theme.backgroundWallpaper
import com.sukisu.ultra.ui.theme.glass.ProvideGlassBackdrop
import com.sukisu.ultra.ui.theme.glass.glassAmbient
import com.sukisu.ultra.ui.theme.glass.layerBackdropIf
import com.sukisu.ultra.ui.theme.glass.rememberGlassBackdrop
import com.sukisu.ultra.ui.theme.tokens.FolkTheme
import com.sukisu.ultra.ui.util.NavigationBarsSpacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box

/**
 * Colours for the settings bar.
 *
 * With a custom background the bar is fully transparent so the image reads
 * straight through it on every page.
 *
 * Otherwise the bar fades from fully transparent to the elevated panel tone.
 * Both ends use the *same* RGB with only the alpha changing - using
 * [Color.Transparent] instead would make Material interpolate the colour from
 * black, which showed up as a grey scrim washing over the title and the
 * content while scrolling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun folkTopAppBarColors(): TopAppBarColors {
    if (FolkTheme.palette.onCustomBackground) {
        return TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    }
    val elevated = folkGroupColor()
    return TopAppBarDefaults.largeTopAppBarColors(
        containerColor = elevated.copy(alpha = 0f),
        scrolledContainerColor = elevated,
    )
}

/**
 * Colours for the screens that build their own bar (home, modules, search):
 * the Material defaults, except that a custom background reads straight
 * through the bar on every page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun folkDefaultAppBarColors(): TopAppBarColors =
    if (FolkTheme.palette.onCustomBackground) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        )
    } else {
        TopAppBarDefaults.topAppBarColors()
    }

/**
 * Shared chrome for every settings sub-screen.
 *
 * Uses a compact [TopAppBar] with the title beside the back button. The bar
 * stays the same height while scrolling and gains the shared elevated tone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkSettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = folkTopAppBarColors(),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        // Opaque, so the entry below this screen (kept composed by the nav
        // host for its card transition and swipe-back gesture) cannot show
        // through the page.
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
    ) { innerPadding ->
        // Keep the list viewport below the bar instead of only offsetting the
        // first item: with a translucent bar in wallpaper mode, content that
        // scrolls underneath would show through the title.
        // Cap the list width on large screens so rows stay readable.
        //
        // Recorded exactly the way [FolkScaffold] records it: the wash lives on a
        // layer of its own and the plates are siblings of it, never drawn into it -
        // a plate sampling a layer it renders into would read back its own output
        // rather than the page behind it. Without this the settings glass had no
        // backdrop to bend and could only tint, which is the card this plate is
        // trying not to be.
        val backdrop = rememberGlassBackdrop()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdropIf(backdrop)
                    // The picture first, the ambient wash over it - and glassAmbient stands
                    // down on its own once the page background goes transparent, so this box
                    // is either the wallpaper or the wash and never both.
                    .backgroundWallpaper()
                    .glassAmbient(),
            )
            ProvideGlassBackdrop(backdrop) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = FolkSettingsDimens.ContentMaxWidth)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            bottom = innerPadding.calculateBottomPadding() + FolkSettingsDimens.ScreenPadding,
                        ),
                    ) {
                        content()
                        item(key = "folk_bottom") {
                            NavigationBarsSpacer()
                        }
                    }
                }
            }
        }
    }
}
