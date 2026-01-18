package com.hyntix.android.pdfmanager.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
object HomeRoute : NavKey

@Serializable
data class ViewerRoute(
    val uri: String,
    val isExternal: Boolean = false
) : NavKey

@Serializable
object SettingsRoute : NavKey

@Serializable
data class LegalRoute(
    val type: String // "privacy", "terms", "licenses"
) : NavKey

@Serializable
object PermissionRoute : NavKey

@Serializable
object DuplicateFinderRoute : NavKey
