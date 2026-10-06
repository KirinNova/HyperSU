package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkSettingsGroup
import com.sukisu.ultra.ui.screen.susfs.component.BottomActionButtons
import com.sukisu.ultra.ui.screen.susfs.component.FeatureStatusCard
import com.sukisu.ultra.ui.screen.susfs.component.FolkSusfsEmptyState
import com.sukisu.ultra.ui.screen.susfs.util.EnabledFeature
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The Enabled Features tab, in the FolkPatch design: an explanatory row, one
 * status row per feature and a refresh action.
 */
@Composable
fun EnabledFeaturesContent(
    enabledFeatures: List<EnabledFeature>,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FolkSettingsGroup {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.susfs_enabled_features_description),
                        style = FolkType.Summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (enabledFeatures.isEmpty()) {
            FolkSusfsEmptyState(message = stringResource(R.string.susfs_no_features_found))
        } else {
            FolkSettingsGroup {
                enabledFeatures.forEach { feature ->
                    item(key = "feature_${feature.name}") {
                        FeatureStatusCard(
                            feature = feature,
                            onRefresh = onRefresh,
                        )
                    }
                }
            }
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.refresh),
        onPrimaryClick = onRefresh,
    )
}
