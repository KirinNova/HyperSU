package com.sukisu.ultra.ui.screen.install

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import androidx.annotation.StringRes
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.sukisu.ultra.R

@Parcelize
sealed class InstallMethod : Parcelable {
    data class SelectFile(
        val uri: Uri? = null,
        @get:StringRes override val label: Int = R.string.select_file,
        override val summary: String?
    ) : InstallMethod()

    data class DownloadFile(
        val url: String? = null,
        val partition: String? = null,
        @get:StringRes override val label: Int = R.string.download_file,
        override val summary: String?
    ) : InstallMethod()

    data object DirectInstall : InstallMethod() {
        override val label: Int
            get() = R.string.direct_install
    }

    data object DirectInstallToInactiveSlot : InstallMethod() {
        override val label: Int
            get() = R.string.install_inactive_slot
    }

    /**
     * An AnyKernel3 archive this app fetched, and one the user picked, are the same flow over
     * the same kind of file. They stay separate classes so the install-method list can tell
     * which row was chosen; [isKernelArchive] is how the shared steps accept either.
     */
    data class HorizonKernel(
        val uri: Uri? = null,
        val slot: String? = null,
        @get:StringRes override val label: Int = R.string.horizon_kernel,
        override val summary: String? = null
    ) : InstallMethod()

    data class AnyKernel3(
        val uri: Uri? = null,
        val slot: String? = null,
        @get:StringRes override val label: Int = R.string.anykernel3_flash,
        override val summary: String? = null
    ) : InstallMethod()

    abstract val label: Int

    @IgnoredOnParcel
    open val summary: String? = null
}

/*
 * These three are declared on the nullable receiver so a call site holding an
 * `InstallMethod?` needs no `?.` or `!!`. A null method simply is not an archive.
 */

/** The uri of an archive row, or null for the methods that carry no archive. */
val InstallMethod?.archiveUri: Uri?
    get() = when (this) {
        is InstallMethod.HorizonKernel -> uri
        is InstallMethod.AnyKernel3 -> uri
        else -> null
    }

/** The slot of an archive row, or null when none is recorded or not applicable. */
val InstallMethod?.archiveSlot: String?
    get() = when (this) {
        is InstallMethod.HorizonKernel -> slot
        is InstallMethod.AnyKernel3 -> slot
        else -> null
    }

/** True for the two archive rows, which share the slot, KPM and confirmation steps. */
val InstallMethod?.isKernelArchive: Boolean
    get() = this is InstallMethod.HorizonKernel || this is InstallMethod.AnyKernel3

/**
 * The same archive with a slot recorded, keeping its own type.
 *
 * The slot dialog hands back only a slot string, so the archive has to be rebuilt. Rebuilding
 * it as a fixed type is what made the install list lose its selection mark: the row the user
 * picked was an AnyKernel3, and the rebuilt value was a HorizonKernel, so the two no longer
 * matched. Copying keeps whichever type it was.
 */
fun InstallMethod.withArchiveSlot(slot: String?): InstallMethod = when (this) {
    is InstallMethod.HorizonKernel -> copy(slot = slot)
    is InstallMethod.AnyKernel3 -> copy(slot = slot)
    else -> this
}

fun isKoFile(context: Context, uri: Uri): Boolean {
    val seg = uri.lastPathSegment ?: ""
    if (seg.endsWith(".ko", ignoreCase = true)) return true

    return try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                val name = cursor.getString(idx)
                name?.endsWith(".ko", ignoreCase = true) == true
            } else {
                false
            }
        } ?: false
    } catch (_: Throwable) {
        false
    }
}
