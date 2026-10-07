package com.sukisu.ultra.ui.screen.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sukisu.ultra.ui.screen.themeSettings.crop.BackgroundCropActivity
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Adb
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Fence
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.KsuIsValid
import com.sukisu.ultra.ui.component.folk.FolkChoicePreference
import com.sukisu.ultra.ui.component.folk.FolkNavigationPreference
import com.sukisu.ultra.ui.component.folk.FolkScaffold
import com.sukisu.ultra.ui.component.folk.FolkSendLogSheet
import com.sukisu.ultra.ui.component.folk.FolkSettingsSectionGroup
import com.sukisu.ultra.ui.component.folk.FolkSliderPreference
import com.sukisu.ultra.ui.component.folk.FolkSwitchPreference
import com.sukisu.ultra.ui.component.folk.FolkTitleStyle
import com.sukisu.ultra.ui.component.folk.FolkValuePreference
import com.sukisu.ultra.ui.component.uninstalldialog.UninstallDialog
import com.sukisu.ultra.ui.theme.BackgroundConfig
import com.sukisu.ultra.ui.theme.glass.GlassConfig
import com.sukisu.ultra.ui.theme.isInDarkTheme
import com.sukisu.ultra.ui.util.LocaleHelper
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * The settings hub in the FolkPatch design.
 *
 * Every row the old screen offered is here with the same gate: the KSU-only
 * rows stay inside [KsuIsValid], the feature switches keep their
 * supported/managed/unsupported summary and enablement, KPM and SuSFS appear
 * only when the kernel provides them, and soft reboot keeps the jailbreak-mode
 * lock. The UI-mode row is gone with the dual design, and the send-log sheet is
 * the single Folk implementation.
 */
@Composable
fun SettingPagerFolk(
    uiState: SettingsUiState,
    actions: SettingsScreenActions,
    bottomInnerPadding: Dp,
    isKpmAvailable: Boolean,
    isSusfsSupported: Boolean,
) {
    val snackBarHost = remember { SnackbarHostState() }
    val showUninstallDialog = rememberSaveable { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    UninstallDialog(
        show = showUninstallDialog.value,
        onDismissRequest = { showUninstallDialog.value = false },
    )

    val languageSystemLabel = stringResource(R.string.settings_language_system)
    val languageTags = remember { listOf(LocaleHelper.SYSTEM) + LocaleHelper.SUPPORTED_TAGS }
    val languageNames = remember(languageSystemLabel) {
        languageTags.map { if (it.isEmpty()) languageSystemLabel else LocaleHelper.displayName(it) }
    }

    FolkScaffold(
        title = stringResource(R.string.settings),
        titleStyle = FolkTitleStyle.Flexible,
        snackbarHostState = snackBarHost,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                bottom = bottomInnerPadding + innerPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            // Update checks (KSU only).
            item {
                KsuIsValid {
                    FolkSettingsSectionGroup(title = stringResource(R.string.settings_check_update)) {
                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_check_update),
                                summary = stringResource(R.string.settings_check_update_summary),
                                icon = Icons.Filled.SystemUpdate,
                                checked = uiState.checkUpdate,
                                onCheckedChange = actions.onSetCheckUpdate,
                            )
                        }
                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_module_check_update),
                                summary = stringResource(R.string.settings_check_update_summary),
                                icon = Icons.Filled.SystemUpdateAlt,
                                checked = uiState.checkModuleUpdate,
                                onCheckedChange = actions.onSetCheckModuleUpdate,
                            )
                        }
                    }
                }
            }

            // Appearance.
            item {
                FolkSettingsSectionGroup(title = stringResource(R.string.settings_theme)) {
                    item {
                        val languageIndex = languageTags.indexOf(uiState.appLanguage)
                            .coerceAtLeast(0)
                        // The choice stays inside the app: a Miuix window-level
                        // dropdown instead of the system's per-app language page.
                        // The switch itself is masked by the FolkLanguageSwitch
                        // cover, so nothing hands the task away any more.
                        MiuixTheme(
                            colors = if (isInDarkTheme()) darkColorScheme() else lightColorScheme(),
                        ) {
                            WindowDropdownPreference(
                                title = stringResource(R.string.settings_language),
                                summary = stringResource(R.string.settings_language_summary),
                                items = languageNames,
                                selectedIndex = languageIndex,
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Rounded.Language,
                                        contentDescription = null,
                                    )
                                },
                                onSelectedIndexChange = { index ->
                                    actions.onSetLanguage(languageTags[index])
                                },
                            )
                        }
                    }
                    item {
                        FolkNavigationPreference(
                            title = stringResource(R.string.settings_theme),
                            summary = stringResource(R.string.settings_theme_summary),
                            icon = Icons.Filled.Palette,
                            onClick = actions.onOpenTheme,
                        )
                    }

                    // Custom background. Driven straight off BackgroundConfig rather than
                    // through the view model: the value is already Compose state, and the
                    // wallpaper on the screen behind this page reads the same object, so a
                    // change here is visible before the row finishes its press animation.
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.settings_background),
                            summary = stringResource(R.string.settings_background_summary),
                            icon = Icons.Rounded.Wallpaper,
                            checked = BackgroundConfig.enabled,
                            onCheckedChange = { BackgroundConfig.setEnabled(it) },
                        )
                    }
                    item {
                        val context = LocalContext.current
                        val cropFailed = stringResource(R.string.background_crop_failed)
                        // Every picture is cropped to the screen before it is stored,
                        // ReSukiSU's adaptation: what the wallpaper draws is already the
                        // shape of the display, so nothing has to guess at fit modes.
                        val cropLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult(),
                        ) { result ->
                            if (result.resultCode == Activity.RESULT_OK) {
                                val output = result.data?.let { data -> UCrop.getOutput(data) }
                                if (output != null) {
                                    val saved = runCatching {
                                        // Internal storage, not the crop cache: a wallpaper
                                        // that vanishes when the cache is evicted reads as
                                        // the setting having forgotten itself.
                                        val file = File(context.filesDir, "custom_background.jpg")
                                        context.contentResolver.openInputStream(output)?.use { input ->
                                            FileOutputStream(file).use { out ->
                                                input.copyTo(out)
                                            }
                                        }
                                        BackgroundConfig.setUri(Uri.fromFile(file).toString())
                                        BackgroundConfig.setEnabled(true)
                                    }.isSuccess
                                    if (!saved) {
                                        BackgroundConfig.setUri(output.toString())
                                        BackgroundConfig.setEnabled(true)
                                    }
                                }
                            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                                Toast.makeText(context, cropFailed, Toast.LENGTH_SHORT).show()
                            }
                        }
                        val picker = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument(),
                        ) { uri ->
                            if (uri != null) {
                                // Persistable: the crop step reads the document once, but
                                // picking the same image again after a process death would
                                // otherwise hit a dead grant.
                                runCatching {
                                    context.contentResolver.takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                    )
                                }
                                val dm = context.resources.displayMetrics
                                val outputUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(
                                        context.cacheDir,
                                        "background_crop_${System.currentTimeMillis()}.jpg",
                                    ),
                                )
                                cropLauncher.launch(
                                    Intent(context, BackgroundCropActivity::class.java).apply {
                                        putExtra(UCrop.EXTRA_INPUT_URI, uri)
                                        putExtra(UCrop.EXTRA_OUTPUT_URI, outputUri)
                                        putExtra(
                                            UCrop.EXTRA_ASPECT_RATIO_X,
                                            dm.widthPixels.toFloat(),
                                        )
                                        putExtra(
                                            UCrop.EXTRA_ASPECT_RATIO_Y,
                                            dm.heightPixels.toFloat(),
                                        )
                                        putExtra(UCrop.EXTRA_MAX_SIZE_X, dm.widthPixels)
                                        putExtra(UCrop.EXTRA_MAX_SIZE_Y, dm.heightPixels)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    },
                                )
                            }
                        }
                        FolkValuePreference(
                            title = stringResource(R.string.settings_background_pick),
                            summary = if (BackgroundConfig.uri.isEmpty()) {
                                stringResource(R.string.settings_background_no_image)
                            } else {
                                null
                            },
                            icon = Icons.Rounded.Image,
                            onClick = { picker.launch(arrayOf("image/*")) },
                        )
                    }
                    item {
                        FolkSliderPreference(
                            title = stringResource(R.string.settings_background_blur),
                            icon = Icons.Rounded.BlurOn,
                            value = BackgroundConfig.blur,
                            onValueChange = { BackgroundConfig.setBlur(it) },
                            valueRange = 0f..40f,
                            steps = 39,
                            valueFormat = { "${it.toInt()} dp" },
                            enabled = BackgroundConfig.isActive,
                        )
                    }
                    item {
                        FolkSliderPreference(
                            title = stringResource(R.string.settings_background_dim),
                            icon = Icons.Rounded.DarkMode,
                            value = BackgroundConfig.dim,
                            onValueChange = { BackgroundConfig.setDim(it) },
                            valueRange = 0f..1f,
                            steps = 19,
                            valueFormat = { "${(it * 100).toInt()}%" },
                            enabled = BackgroundConfig.isActive,
                        )
                    }
                    // No fit-mode row: the crop screen is the adaptation now, same as
                    // ReSukiSU. BackgroundConfig.cover stays at fill for drawing.
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.settings_enable_glass),
                            summary = stringResource(R.string.settings_enable_glass_summary),
                            icon = Icons.Rounded.Tune,
                            checked = GlassConfig.enabled,
                            onCheckedChange = { GlassConfig.setEnabled(it) },
                        )
                    }
                    item {
                        FolkSliderPreference(
                            title = stringResource(R.string.settings_glass_intensity),
                            icon = Icons.Rounded.Tonality,
                            value = GlassConfig.intensity,
                            onValueChange = { GlassConfig.setIntensity(it) },
                            valueRange = 0.4f..1.6f,
                            steps = 24,
                            valueFormat = { "x" + it },
                            enabled = GlassConfig.enabled,
                        )
                    }
                    item {
                        FolkSliderPreference(
                            title = stringResource(R.string.settings_glass_blur),
                            icon = Icons.Rounded.BlurOn,
                            value = GlassConfig.blur,
                            onValueChange = { GlassConfig.setBlur(it) },
                            valueRange = 0f..60f,
                            steps = 60,
                            valueFormat = { "${it.toInt()} dp" },
                            enabled = GlassConfig.enabled && GlassConfig.blurEnabled,
                        )
                    }
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.settings_glass_rim),
                            icon = Icons.Rounded.Brush,
                            checked = GlassConfig.rim,
                            onCheckedChange = { GlassConfig.setRim(it) },
                            enabled = GlassConfig.enabled,
                        )
                    }
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.settings_glass_specular),
                            icon = Icons.Rounded.Flare,
                            checked = GlassConfig.specular,
                            onCheckedChange = { GlassConfig.setSpecular(it) },
                            enabled = GlassConfig.enabled,
                        )
                    }
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.settings_glass_sheen),
                            icon = Icons.Rounded.Lightbulb,
                            checked = GlassConfig.sheen,
                            onCheckedChange = { GlassConfig.setSheen(it) },
                            enabled = GlassConfig.enabled,
                        )
                    }
                    item {
                        FolkSwitchPreference(
                            title = stringResource(R.string.icon_switch_title),
                            summary = stringResource(R.string.icon_switch_summary),
                            icon = Icons.Rounded.Android,
                            checked = uiState.alternativeIcon,
                            onCheckedChange = actions.onSetAlternativeIcon,
                        )
                    }
                }
            }

            // Templates and tools (KSU only).
            item {
                KsuIsValid {
                    FolkSettingsSectionGroup(title = stringResource(R.string.settings_profile_template)) {
                        item {
                            FolkNavigationPreference(
                                title = stringResource(R.string.settings_profile_template),
                                summary = stringResource(R.string.settings_profile_template_summary),
                                icon = Icons.Filled.Description,
                                onClick = actions.onOpenProfileTemplate,
                            )
                        }
                        item {
                            FolkNavigationPreference(
                                title = stringResource(R.string.settings_tools),
                                summary = stringResource(R.string.settings_tools_summary),
                                icon = Icons.Filled.Fence,
                                onClick = actions.onOpenTools,
                            )
                        }
                    }
                }
            }

            // KPM and SuSFS, when the kernel provides them.
            if (isKpmAvailable) {
                item {
                    FolkSettingsSectionGroup(title = stringResource(R.string.kpm_title)) {
                        item {
                            FolkNavigationPreference(
                                title = stringResource(R.string.kpm_title),
                                summary = stringResource(R.string.settings_kpm_summary),
                                icon = Icons.Filled.Fence,
                                onClick = actions.onOpenKpm,
                            )
                        }
                    }
                }
            }

            // SuSFS config is its own section and reaches the kernel through ksud, so it
            // must not inherit the KPM gate above it - a device without KPM was losing the
            // SuSFS entry entirely.
            if (isSusfsSupported) {
                item {
                    FolkSettingsSectionGroup(title = stringResource(R.string.susfs_config_title)) {
                        item {
                            FolkNavigationPreference(
                                title = stringResource(R.string.susfs_config_title),
                                summary = stringResource(R.string.susfs_config_summary),
                                icon = Icons.Filled.Fence,
                                onClick = actions.onOpenSusfsConfig,
                            )
                        }
                    }
                }
            }

            // Kernel features (KSU only).
            item {
                KsuIsValid {
                    FolkSettingsSectionGroup(title = stringResource(R.string.settings_sucompat)) {
                        item {
                            val suSummary = when (uiState.suCompatStatus) {
                                "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(R.string.feature_status_managed_summary)
                                else -> stringResource(R.string.settings_sucompat_summary)
                            }
                            val suModes = listOf(
                                stringResource(R.string.settings_mode_enable_by_default),
                                stringResource(R.string.settings_mode_disable_until_reboot),
                                stringResource(R.string.settings_mode_disable_always),
                            )
                            FolkChoicePreference(
                                title = stringResource(R.string.settings_sucompat),
                                summary = suSummary,
                                icon = Icons.Filled.AdminPanelSettings,
                                options = suModes,
                                selectedIndex = uiState.suCompatMode,
                                enabled = uiState.suCompatStatus == "supported",
                                onSelect = actions.onSetSuCompatMode,
                            )
                        }

                        item {
                            val umountSummary = when (uiState.kernelUmountStatus) {
                                "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(R.string.feature_status_managed_summary)
                                else -> stringResource(R.string.settings_kernel_umount_summary)
                            }
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_kernel_umount),
                                summary = umountSummary,
                                icon = Icons.Filled.LayersClear,
                                enabled = uiState.kernelUmountStatus == "supported",
                                checked = uiState.isKernelUmountEnabled,
                                onCheckedChange = actions.onSetKernelUmountEnabled,
                            )
                        }

                        item {
                            val selinuxHideSummary = when (uiState.selinuxHideStatus) {
                                "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(R.string.feature_status_managed_summary)
                                else -> stringResource(R.string.settings_selinux_hide_summary)
                            }
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_selinux_hide),
                                summary = selinuxHideSummary,
                                icon = Icons.Filled.Security,
                                enabled = uiState.selinuxHideStatus == "supported",
                                checked = uiState.isSelinuxHideEnabled,
                                onCheckedChange = actions.onSetSelinuxHideEnabled,
                            )
                        }

                        item {
                            val sulogSummary = when (uiState.sulogStatus) {
                                "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(R.string.feature_status_managed_summary)
                                else -> stringResource(R.string.settings_sulog_summary)
                            }
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_sulog),
                                summary = sulogSummary,
                                icon = Icons.AutoMirrored.Filled.Article,
                                enabled = uiState.sulogStatus == "supported",
                                checked = uiState.isSulogEnabled,
                                onCheckedChange = actions.onSetSulogEnabled,
                            )
                        }

                        item {
                            val adbRootSummary = when (uiState.adbRootStatus) {
                                "unsupported" -> stringResource(R.string.feature_status_unsupported_summary)
                                "managed" -> stringResource(R.string.feature_status_managed_summary)
                                else -> stringResource(R.string.settings_adb_root_summary)
                            }
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_adb_root),
                                summary = adbRootSummary,
                                icon = Icons.Filled.Adb,
                                enabled = uiState.adbRootStatus == "supported",
                                checked = uiState.isAdbRootEnabled,
                                onCheckedChange = actions.onSetAdbRootEnabled,
                            )
                        }

                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_soft_reboot),
                                summary = stringResource(R.string.settings_soft_reboot_summary),
                                icon = Icons.Filled.RestartAlt,
                                enabled = !uiState.isLateLoadMode,
                                checked = uiState.isLateLoadMode || uiState.useSoftReboot,
                                onCheckedChange = actions.onSetUseSoftReboot,
                            )
                        }
                    }
                }
            }

            // Behaviour (KSU only).
            item {
                KsuIsValid {
                    FolkSettingsSectionGroup(title = stringResource(R.string.settings_umount_modules_default)) {
                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_umount_modules_default),
                                summary = stringResource(R.string.settings_umount_modules_default_summary),
                                icon = Icons.AutoMirrored.Filled.Rule,
                                checked = uiState.isDefaultUmountModules,
                                onCheckedChange = actions.onSetDefaultUmountModules,
                            )
                        }
                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.enable_web_debugging),
                                summary = stringResource(R.string.enable_web_debugging_summary),
                                icon = Icons.Filled.DeveloperMode,
                                checked = uiState.enableWebDebugging,
                                onCheckedChange = actions.onSetEnableWebDebugging,
                            )
                        }
                        item {
                            FolkSwitchPreference(
                                title = stringResource(R.string.settings_auto_jailbreak),
                                summary = stringResource(R.string.settings_auto_jailbreak_summary),
                                icon = Icons.Filled.FlashOn,
                                enabled = uiState.isLateLoadMode,
                                checked = uiState.autoJailbreak,
                                onCheckedChange = actions.onSetAutoJailbreak,
                            )
                        }
                    }
                }
            }

            // Uninstall, only meaningful in LKM mode.
            if (uiState.isLkmMode) {
                item {
                    FolkSettingsSectionGroup(title = stringResource(R.string.settings_uninstall)) {
                        item {
                            FolkNavigationPreference(
                                title = stringResource(R.string.settings_uninstall),
                                icon = Icons.Filled.Delete,
                                enabled = !uiState.isLateLoadMode,
                                onClick = { showUninstallDialog.value = true },
                            )
                        }
                    }
                }
            }

            // Diagnostics and about.
            item {
                FolkSettingsSectionGroup(title = stringResource(R.string.about)) {
                    item {
                        FolkNavigationPreference(
                            title = stringResource(R.string.send_log),
                            icon = Icons.Filled.BugReport,
                            onClick = { showBottomSheet = true },
                        )
                    }
                    item {
                        FolkNavigationPreference(
                            title = stringResource(R.string.about),
                            icon = Icons.Filled.Info,
                            onClick = actions.onOpenAbout,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        if (showBottomSheet) {
            FolkSendLogSheet(
                onDismiss = { showBottomSheet = false },
                snackbarHostState = snackBarHost,
            )
        }
    }
}
