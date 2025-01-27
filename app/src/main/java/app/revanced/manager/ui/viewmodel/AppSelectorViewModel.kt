package app.revanced.manager.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.revanced.manager.Variables
import app.revanced.manager.Variables.patches
import app.revanced.manager.Variables.selectedAppPackage
import app.revanced.manager.ui.Resource
import app.revanced.manager.util.tag
import app.revanced.patcher.extensions.PatchExtensions.compatiblePackages
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

class AppSelectorViewModel(
    val app: Application,
) : ViewModel() {

    val filteredApps = mutableListOf<ApplicationInfo>()

    init {
        viewModelScope.launch { filterApps() }
    }

    private fun filterApps() {
        try {
            val (patches) = patches.value as Resource.Success
            patches.forEach patch@{ patch ->
                patch.compatiblePackages?.forEach { pkg ->
                    try {
                        if (!(filteredApps.any { it.packageName == pkg.name })) {
                            val appInfo = app.packageManager.getApplicationInfo(pkg.name, 0)
                            filteredApps.add(appInfo)
                            return@forEach
                        }
                    } catch (e: Exception) {
                        return@forEach
                    }
                }
            }
            Log.d(tag, "Filtered apps.")
        } catch (e: Exception) {
            Log.e(tag, "An error occurred while filtering", e)
        }
    }

    fun applicationLabel(info: ApplicationInfo): String {
        return app.packageManager.getApplicationLabel(info).toString()
    }

    fun loadIcon(info: ApplicationInfo): Drawable? {
        return info.loadIcon(app.packageManager)
    }

    fun setSelectedAppPackage(appId: ApplicationInfo) {
        selectedAppPackage.value.ifPresent { s ->
            if (s != appId) Variables.selectedPatches.clear()
        }
        selectedAppPackage.value = Optional.of(appId)
    }

    fun setSelectedAppPackageFromFile(file: Uri?) {
        val apkDir = app.filesDir.resolve("input.apk").toPath()
        Files.copy(
            app.contentResolver.openInputStream(file!!),
            apkDir,
            StandardCopyOption.REPLACE_EXISTING
        )
        setSelectedAppPackage(
            app.packageManager.getPackageArchiveInfo(
                apkDir.toString(),
                PackageManager.GET_META_DATA
            )!!.applicationInfo
        )
    }
}