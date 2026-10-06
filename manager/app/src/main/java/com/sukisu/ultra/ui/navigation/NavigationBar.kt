package com.sukisu.ultra.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukisu.ultra.ui.theme.glass.GlassStrength
import com.sukisu.ultra.ui.theme.glass.liquidGlass
import com.sukisu.ultra.ui.theme.tokens.ContinuousCornerShape
import com.sukisu.ultra.ui.theme.tokens.FolkShape

/** Height of the docked bar, excluding the system navigation-bar inset. */
private val DockedBarHeight = 68.dp

/** Height of the floating capsule. */
private val FloatingBarHeight = 62.dp

/**
 * The FolkPatch bottom navigation bar.
 *
 * A visual port of FolkPatch's `NavigationBarContent` (`BottomBarContent` in its non-drawer
 * shape): a surface holding one selectable item per [BottomBarDestination], where the selected
 * item carries a pill behind its icon and both the icon and the label spring into the accent
 * colour. The floating variant shrinks the surface into a capsule inset from the screen edges;
 * the docked variant spans the full width with only its top corners rounded.
 *
 * Selection is driven purely by [selectedIndex] and reported back through
 * [onSelectedIndexChange] - there is no NavHostController, no compose-destinations route and no
 * repository behind this composable, so the caller keeps owning navigation.
 *
 * @param selectedIndex index of the active tab, `0..BottomBarDestination.PAGE_COUNT - 1`.
 * @param onSelectedIndexChange invoked with the newly tapped tab index.
 * @param badge per-tab badge counts; a zero count renders no badge.
 * @param isFloating `true` for the floating capsule, `false` for the docked bar.
 */
@Composable
fun FolkBottomBar(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    badge: NavigationBadgeState,
    isFloating: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val destinations = BottomBarDestination.entries
    val density = LocalDensity.current

    // Shared shape switch: the capsule is reused for the floating mode, while the docked bar keeps
    // the same corner family on its top edge only.
    val barShape = if (isFloating) {
        FolkShape.CornerFull
    } else {
        ContinuousCornerShape(topStart = 24.dp, topEnd = 24.dp)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isFloating) {
                    Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        // The capsule sits above the system bar; the inset is added below.
                        bottom = 8.dp,
                    )
                } else {
                    Modifier
                }
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            // The bar is chrome floating over whatever screen is showing, so it cannot
            // refract the page - that layer is recorded in the content window, not here.
            // It still has to read as glass: the tint, the specular and the hairline rim
            // come from the shared plate, which is the same glass minus the refraction.
            .liquidGlass(
                shape = barShape,
                strength = GlassStrength.Subtle,
                refract = false,
            ),
        shape = barShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // Elevation is pointless over a transparent body - it only tints a fill that is
        // no longer there - so the floating variant keeps its drop shadow and drops the
        // tonal lift.
        tonalElevation = 0.dp,
        shadowElevation = if (isFloating) 8.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFloating) FloatingBarHeight else DockedBarHeight)
                .padding(horizontal = if (isFloating) 6.dp else 4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex

                // One spring drives the pill, the icon scale and the colour, so the whole item
                // settles as a single movement instead of three.
                val selection by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "FolkBottomBarSelection$index",
                )

                val iconColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "FolkBottomBarIconColor$index",
                )

                val labelColor by animateColorAsState(
                    targetValue = if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "FolkBottomBarLabelColor$index",
                )

                val pillColor = MaterialTheme.colorScheme.secondaryContainer
                val navBadge = badgeFor(index, badge)
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(FolkShape.CornerFull)
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelectedIndexChange(index) },
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BadgedBox(
                        badge = {
                            if (navBadge != null) {
                                Badge(
                                    containerColor = when (navBadge.tone) {
                                        BadgeTone.Alert -> MaterialTheme.colorScheme.error
                                        BadgeTone.Accent -> MaterialTheme.colorScheme.primary
                                    },
                                    contentColor = when (navBadge.tone) {
                                        BadgeTone.Alert -> MaterialTheme.colorScheme.onError
                                        BadgeTone.Accent -> MaterialTheme.colorScheme.onPrimary
                                    },
                                ) {
                                    Text(
                                        text = navBadge.count.coerceAtMost(99).toString(),
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 28.dp)
                                .scale(0.9f + 0.1f * selection)
                                .background(
                                    color = pillColor.copy(alpha = selection),
                                    shape = FolkShape.CornerFull,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selected) {
                                    destination.iconSelected
                                } else {
                                    destination.iconNotSelected
                                },
                                contentDescription = stringResource(destination.label),
                                tint = iconColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Text(
                        text = stringResource(destination.label),
                        color = labelColor,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
