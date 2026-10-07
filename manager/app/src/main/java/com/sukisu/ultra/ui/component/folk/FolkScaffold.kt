package com.sukisu.ultra.ui.component.folk

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.rememberTopAppBarState
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.navigation.LocalBottomBarVisible
import com.sukisu.ultra.ui.navigation.LocalIsFloatingNavMode
import com.sukisu.ultra.ui.theme.backgroundWallpaper
import com.sukisu.ultra.ui.theme.glass.glassAmbient
import com.sukisu.ultra.ui.theme.tokens.FolkTheme
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment

/**
 * How a [FolkScaffold] presents its title.
 *
 * [Inline] is the default on purpose: a collapsible [Large] title reads wrong on
 * a tab page (that combination caused a rework once already), so making the
 * large title an explicit opt-in keeps the mistake from happening by accident.
 *
 * [Flexible] is the Material 3 Expressive bar for content pages: it starts tall,
 * carries an optional subtitle, and collapses to the inline height on scroll.
 */
enum class FolkTitleStyle { Large, Flexible, Inline, None }

/**
 * The shared chrome for a screen: title, back button, actions, snackbar and the
 * insets that keep content clear of the status bar and the floating bottom bar.
 *
 * The content slot receives the padding to apply; it stays a plain lambda rather
 * than a `LazyListScope` because the screens are not all lists - a settings page
 * is a `LazyColumn`, the theme picker is a staggered grid and the audit log puts
 * a `TabRow` between the bar and the list. A shared "padding + chrome" contract
 * covers all of them without reshaping any of them.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FolkScaffold(
    title: String = "",
    titleStyle: FolkTitleStyle = FolkTitleStyle.Inline,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    /** Optional supporting line under a [FolkTitleStyle.Flexible] title. */
    subtitle: String? = null,
    floatingActionButton: @Composable () -> Unit = {},
    /**
     * A persistent bottom bar, such as a command input. Callers that use this
     * should usually pass `addBottomClearance = false`, because the bar already
     * contributes to the content padding.
     */
    bottomBar: (@Composable () -> Unit)? = null,
    /**
     * A fully custom bar. Screens whose bar is not a plain title (an animated
     * search field, a selection bar) supply it here so they still get the shared
     * insets and bottom clearance without reshaping their bar.
     */
    topBar: (@Composable () -> Unit)? = null,
    /**
     * Set false when the content already keeps its own bottom clearance (e.g. a
     * list using `fabNavBottomClearance`), so the space is not reserved twice.
     */
    addBottomClearance: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior(rememberTopAppBarState())
    // The tint flips between clear and the elevated tone as the content scrolls
    // under the bar. Both ends keep one RGB so the animation never interpolates
    // through black and leaves a grey scrim over the wallpaper.
    val elevatedBarColor = folkGroupColor()
    val scrolled by remember(scrollBehavior) {
        derivedStateOf { scrollBehavior.state.contentOffset < -4f }
    }
    val barColor by animateColorAsState(
        targetValue = if (FolkTheme.palette.onCustomBackground) {
            Color.Transparent
        } else if (scrolled) {
            elevatedBarColor
        } else {
            elevatedBarColor.copy(alpha = 0f)
        },
        label = "folkBarColor",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // The page background, full bleed: behind the bar (transparent over a picture,
        // so the wallpaper reaches the status bar) and above the still-composed entry
        // underneath that a clear container would otherwise show through. Outside
        // wallpaper mode this wash is an opaque background of its own, so the
        // container below can stay clear in both modes.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .backgroundWallpaper()
                .glassAmbient(),
        )
        Scaffold(
        // Only the collapsible built-in bar consumes scroll; the others need no
        // connection.
        modifier = if (topBar == null && (titleStyle == FolkTitleStyle.Large || titleStyle == FolkTitleStyle.Flexible)) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        topBar = {
            val custom = topBar
            if (custom != null) {
                custom()
            } else {
                // Miuix replaces the Material bars: the Material large/flexible
                // bar paints its own default surface over the title region, which
                // read as a white band across the wallpaper on every content page.
                when (titleStyle) {
                    FolkTitleStyle.Large, FolkTitleStyle.Flexible -> TopAppBar(
                        title = title,
                        largeTitle = title,
                        subtitle = subtitle.orEmpty(),
                        color = barColor,
                        titleColor = MaterialTheme.colorScheme.onBackground,
                        largeTitleColor = MaterialTheme.colorScheme.onBackground,
                        subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        navigationIcon = { FolkBackButton(onBack) },
                        actions = actions,
                        scrollBehavior = scrollBehavior,
                    )

                    FolkTitleStyle.Inline -> SmallTopAppBar(
                        title = title,
                        color = barColor,
                        titleColor = MaterialTheme.colorScheme.onBackground,
                        navigationIcon = { FolkBackButton(onBack) },
                        actions = actions,
                    )

                    FolkTitleStyle.None -> Unit
                }
            }
        },
        // The full-bleed box above paints the page - wallpaper or wash - and covers
        // the still-composed entry underneath, so the container stays clear and lets
        // that box show. contentColor stays pinned: a transparent container derives
        // Unspecified, which would drop uncoloured text to black in dark mode.
        containerColor = Color.Transparent,
        // Deriving the content colour from a transparent container yields
        // Unspecified, which drops any text that does not set its own colour to
        // black - unreadable in dark mode. Pin it to the background's content
        // colour.
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = {
            if (snackbarHostState != null) {
                SnackbarHost(snackbarHostState)
            }
        },
        floatingActionButton = floatingActionButton,
        bottomBar = {
            val custom = bottomBar
            if (custom != null) {
                custom()
            }
        },
    ) { inner ->
        val layoutDirection = LocalLayoutDirection.current
        val clearance = if (addBottomClearance) folkBottomClearance() else 0.dp
        // Keep the content viewport below the bar instead of only offsetting the
        // first item. With a translucent bar in wallpaper mode, content that
        // scrolls underneath would otherwise show through the title.
        // The backdrop is recorded once at the app root, so plates here sample a
        // layer they are not drawn into - the rule that stops a plate reading back
        // its own output - without every screen recording the same picture again.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = inner.calculateTopPadding()),
        ) {
            // Cap the content column on large screens so rows do not stretch
            // across a tablet or desktop window; on a phone this is a no-op.
            Box(
                modifier = Modifier
                    .widthIn(max = FolkSettingsDimens.ContentMaxWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
            ) {
                content(
                    PaddingValues(
                        start = inner.calculateStartPadding(layoutDirection),
                        end = inner.calculateEndPadding(layoutDirection),
                        top = 0.dp,
                        bottom = inner.calculateBottomPadding() + clearance,
                    )
                )
            }
        }
    }
    }
}

@Composable
private fun FolkBackButton(onBack: (() -> Unit)?) {
    if (onBack != null) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
        }
    }
}

/**
 * Clearance under the last item so it can be scrolled above the floating bottom
 * bar.
 *
 * The overlay only exists on main-tab routes; on a detail page the app hides
 * the bar in both floating and docked mode, and the docked shell pads the nav
 * host only on its main-tab routes. In those cases a small gap under the
 * content is all that is needed, and adding the floating height there would
 * just be dead scroll space.
 */
@Composable
private fun folkBottomClearance(): Dp {
    val floating = LocalIsFloatingNavMode.current
    val barVisible = LocalBottomBarVisible.current.value
    return animateDpAsState(
        targetValue = if (floating && barVisible) FloatingBarClearance else StaticBarClearance,
        label = "folkBottomClearance",
    ).value
}

private val FloatingBarClearance = 80.dp
private val StaticBarClearance = 16.dp
