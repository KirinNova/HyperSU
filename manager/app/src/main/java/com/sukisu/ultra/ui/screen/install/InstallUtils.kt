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
    /**
     * An AnyKernel3 archive, whatever its origin.
     *
     * [HorizonKernel] and [AnyKernel3] are the same flow over the same kind of file, and the
     * slot and KPM steps have to accept either. They stay separate types so the install-method
     * list can tell which row the user actually picked - collapsing one into the other made the
     * chosen row lose its selection mark.
     */
    interface KernelArchive {
        val uri: Uri?
        val slot: String?
    }

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

    data class HorizonKernel(
        override val uri: Uri? = null,
        override val slot: String? = null,
        @get:StringRes override val label: Int = R.string.horizon_kernel,
        override val summary: String? = null
    ) : InstallMethod(), KernelArchive

    /**
     * Flash an AnyKernel3 archive the user picked themselves.
     *
     * HorizonKernel is the same flow for a kernel this app downloads; this one exists so a
     * locally held AnyKernel3 zip can be flashed without going through the download path.
     * It carries the same slot and KPM-patch steps.
     */
    data class AnyKernel3(
        override val uri: Uri? = null,
        override val slot: String? = null,
        @get:StringRes override val label: Int = R.string.anykernel3_flash,
        override val summary: String? = null
    ) : InstallMethod(), KernelArchive

    abstract val label: Int

    @IgnoredOnParcel
    open val summary: String? = null
}

/**
 * The same archive with a slot recorded, keeping its own type.
 *
 * The slot dialog hands back only a slot string, so the archive has to be rebuilt. Rebuilding
 * it as a fixed type is what made the install list lose its selection mark: the row the user
 * picked was an AnyKernel3, and the rebuilt value was a HorizonKernel, so the two no longer
 * matched. Copying keeps whichever type it was.
 */
fun InstallMethod.KernelArchive.withSlot(slot: String?): InstallMethod = when (this) {
    is InstallMethod.HorizonKernel -> copy(slot = slot)
    is InstallMethod.AnyKernel3 -> copy(slot = slot)
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
