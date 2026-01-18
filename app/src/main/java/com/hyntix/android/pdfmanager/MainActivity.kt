package com.hyntix.android.pdfmanager

import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.padding
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.hyntix.android.pdfmanager.ui.home.HomeScreen
import com.hyntix.android.pdfmanager.ui.navigation.HomeRoute
import com.hyntix.android.pdfmanager.ui.navigation.ViewerRoute
import com.hyntix.android.pdfmanager.ui.navigation.SettingsRoute
import com.hyntix.android.pdfmanager.ui.navigation.LegalRoute
import com.hyntix.android.pdfmanager.ui.theme.PDFManagerTheme
import com.hyntix.android.pdfmanager.ui.viewer.PdfViewerScreen
import com.hyntix.android.pdfmanager.ui.settings.SettingsScreen
import com.hyntix.android.pdfmanager.ui.settings.LegalScreen
import com.hyntix.android.pdfmanager.ui.settings.PolicyContent

class MainActivity : ComponentActivity() {
    // Store incoming intent URI for when opened from external apps
    private var externalPdfUri: Uri? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Handle incoming intent (when opened from other apps like Telegram, Gmail, etc.)
        if (savedInstanceState == null) {
            externalPdfUri = intent?.data
            // Run KotlinPdfium integration test
            kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
                PdfiumTester.runTest(applicationContext)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { com.hyntix.android.pdfmanager.data.repository.SettingsRepository(applicationContext) }
            val appTheme by settingsRepository.appTheme.collectAsState(initial = 0)
            val keepScreenOn by settingsRepository.keepScreenOn.collectAsState(initial = false)

            val grayscaleMode by settingsRepository.grayscaleMode.collectAsState(initial = false)
            val scrollDirection by settingsRepository.scrollDirection.collectAsState(initial = 0)
            val scrollMode by settingsRepository.scrollMode.collectAsState(initial = 0)

            
            // ... (existing code)


            var hasStoragePermission by remember { 
                mutableStateOf(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        true // Fallback for older APIs (assuming legacy storage is handled or not targeted)
                    }
                ) 
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            // Re-check permission on Resume
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            
            val isDarkTheme = when (appTheme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            
            androidx.compose.runtime.DisposableEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {}
            }

            // Persist flag across process death to handle permission return correctly
            // MOVED TO VIEWMODEL: var permissionRequestInProgress = ...
            val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            PDFManagerTheme(darkTheme = isDarkTheme) {
                androidx.compose.material3.Scaffold(
                    snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier.fillMaxSize().then(
                            if (grayscaleMode) {
                                Modifier.drawWithContent {
                                    val saturationMatrix = androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                                    // Use a simple saturation filter layer
                                    this.drawContent()
                                    drawRect(
                                        color = androidx.compose.ui.graphics.Color.Gray,
                                        blendMode = androidx.compose.ui.graphics.BlendMode.Saturation
                                    )
                                }
                            } else Modifier
                        ),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val application = context.applicationContext as android.app.Application
                        val fileRepository = remember { com.hyntix.android.pdfmanager.data.repository.FileRepository(context) }
                        
                        val homeViewModel: com.hyntix.android.pdfmanager.ui.home.HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                            factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                    return com.hyntix.android.pdfmanager.ui.home.HomeViewModel(application, fileRepository, settingsRepository) as T
                                }
                            }
                        )
                        
                        // Determine initial route - external intents go directly to viewer
                        val intentUri = externalPdfUri
                        val initialRoute: Any = when {
                            intentUri != null -> {
                                // Clear the external URI so it's not processed again
                                externalPdfUri = null
                                ViewerRoute(uri = intentUri.toString(), isExternal = true)
                            }
                            hasStoragePermission -> HomeRoute
                            else -> com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute
                        }
                        
                        // Custom Saver for navigation backStack to survive config changes
                        val backStackSaver = androidx.compose.runtime.saveable.Saver<androidx.compose.runtime.snapshots.SnapshotStateList<Any>, List<String>>(
                            save = { list ->
                                list.map { route ->
                                    when (route) {
                                        is HomeRoute -> "home"
                                        is ViewerRoute -> "viewer|${route.isExternal}|${route.uri}"
                                        is SettingsRoute -> "settings"
                                        is LegalRoute -> "legal:${route.type}"
                                        is com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute -> "permission"
                                        else -> ""
                                    }
                                }
                            },
                            restore = { list ->
                                val restored = mutableStateListOf<Any>()
                                list.forEach { encoded ->
                                    val route: Any? = when {
                                        encoded == "home" -> HomeRoute
                                        encoded.startsWith("viewer|") -> {
                                            val parts = encoded.removePrefix("viewer|").split("|", limit = 2)
                                            if (parts.size >= 2) {
                                                ViewerRoute(uri = parts[1], isExternal = parts[0] == "true")
                                            } else {
                                                ViewerRoute(uri = parts[0])
                                            }
                                        }
                                        encoded == "settings" -> SettingsRoute
                                        encoded.startsWith("legal:") -> LegalRoute(encoded.removePrefix("legal:"))
                                        encoded == "permission" -> com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute
                                        else -> null
                                    }
                                    route?.let { restored.add(it) }
                                }
                                if (restored.isEmpty()) restored.add(if (hasStoragePermission) HomeRoute else com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute)
                                restored
                            }
                        )
                        
                        val backStack = androidx.compose.runtime.saveable.rememberSaveable(saver = backStackSaver) {
                            mutableStateListOf<Any>(initialRoute)
                        }
                        
                        // Update permission state and navigation on resume
                        androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        val isGranted = android.os.Environment.isExternalStorageManager()
                                        // Always update UI state
                                        hasStoragePermission = isGranted
                                        
                                        // Logic Loop: Only act if we were waiting for a user action (State in ViewModel)
                                        if (homeViewModel.isPermissionRequestInProgress) {
                                            if (isGranted) {
                                                // SCENARIO 1: Permission Granted -> Navigate to Home
                                                // Strict Guard: Check ENTIRE stack for HomeRoute to prevent duplicates
                                                val isHomeAlreadyInStack = backStack.any { it == HomeRoute }
                                                
                                                if (!isHomeAlreadyInStack) {
                                                    backStack.clear() // Clean slate
                                                    backStack.add(HomeRoute)
                                                    // UX: Force shimmer scan on this specific transition
                                                    homeViewModel.refresh(showIndicator = false, forceLoad = true)
                                                }
                                            } else {
                                                // SCENARIO 2: Permission Denied -> Show Warning
                                                // Only show if we were on the Permission screen (context check)
                                                if (backStack.lastOrNull() == com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = "Permission is required to access files",
                                                            duration = androidx.compose.material3.SnackbarDuration.Short
                                                        )
                                                    }
                                                }
                                            }
                                            // Consume the flag so subsequent resumes (minimize/restore) do nothing
                                            homeViewModel.isPermissionRequestInProgress = false
                                        }
                                        // SCENARIO 3: Normal Resume -> Do nothing (no refresh, no nav)
                                    }
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                            }
                        }
                        
                        val entryProvider = entryProvider {
                            entry<com.hyntix.android.pdfmanager.ui.navigation.PermissionRoute> {
                                com.hyntix.android.pdfmanager.ui.permission.PermissionScreen(
                                    onPermissionGranted = {
                                        // Logic handled in onResume
                                    },
                                    onPermissionRequested = {
                                        homeViewModel.isPermissionRequestInProgress = true
                                    }
                                )
                            }

                            entry<HomeRoute> {
                                HomeScreen(
                                    viewModel = homeViewModel,
                                    onSettingsClick = { 
                                        backStack.add(SettingsRoute) 
                                    },
                                    onPdfSelected = { uriString ->
                                        backStack.add(ViewerRoute(uri = uriString))
                                        // Delay clearing search to prevent UI glitch where search bar closes before navigation
                                        scope.launch {
                                            kotlinx.coroutines.delay(500)
                                            homeViewModel.clearSearch()
                                        }
                                    },
                                    onDuplicateFinderClick = {
                                        backStack.add(com.hyntix.android.pdfmanager.ui.navigation.DuplicateFinderRoute)
                                    }
                                )
                            }
                            
                            entry<SettingsRoute> {
                                SettingsScreen(
                                    onBackClick = {
                                        if (backStack.isNotEmpty()) backStack.removeLast()
                                    },
                                    onLegalClick = { type ->
                                        backStack.add(LegalRoute(type))
                                    }
                                )
                            }
                            
                            entry<LegalRoute> { key ->
                                val (title, content) = when (key.type) {
                                    "privacy" -> "Privacy Policy" to PolicyContent.PRIVACY_POLICY
                                    "terms" -> "Terms of Service" to PolicyContent.TERMS_OF_SERVICE
                                    "licenses" -> "Open Source Licenses" to PolicyContent.OPEN_SOURCE_LICENSES
                                    else -> "Legal" to ""
                                }
                                LegalScreen(
                                    title = title,
                                    content = content,
                                    onBackClick = {
                                        if (backStack.isNotEmpty()) backStack.removeLast()
                                    }
                                )
                            }
                            
                            entry<com.hyntix.android.pdfmanager.ui.navigation.DuplicateFinderRoute> {
                                com.hyntix.android.pdfmanager.ui.duplicates.DuplicateFinderScreen(
                                    onNavigateBack = {
                                        if (backStack.isNotEmpty()) backStack.removeLast()
                                    }
                                )
                            }
                            
                            entry<ViewerRoute> { key ->
                                // Check if file is favorite and get page number
                                val isFavorite = remember { mutableStateOf(false) }
                                val initialPage = remember { mutableStateOf<Int?>(null) }
                                val isDataLoaded = remember { mutableStateOf(false) }
                                val viewerScope = androidx.compose.runtime.rememberCoroutineScope()
                                
                                // Load initial data
                                val context = androidx.compose.ui.platform.LocalContext.current
                                androidx.compose.runtime.LaunchedEffect(key.uri) {
                                    val uriObj = Uri.parse(key.uri)
                                    var fileName: String? = null
                                     if (uriObj.scheme == "content") {
                                         try {
                                             context.contentResolver.query(uriObj, null, null, null, null)?.use { cursor ->
                                                 if (cursor.moveToFirst()) {
                                                     val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                                     if (index >= 0) fileName = cursor.getString(index)
                                                 }
                                             }
                                         } catch (e: Exception) {
                                             e.printStackTrace()
                                         }
                                     }
                                     if (fileName == null) {
                                         fileName = uriObj.lastPathSegment
                                     }
                                    
                                    // Only add to recents for internal files (not from external apps like Telegram, WhatsApp)
                                    if (!key.isExternal) {
                                        settingsRepository.addRecentFile(key.uri, fileName ?: "Document")
                                    }

                                    isFavorite.value = settingsRepository.isFavorite(key.uri)
                                    initialPage.value = settingsRepository.getLastPage(key.uri) ?: 0 // Default to page 0
                                    isDataLoaded.value = true
                                }
                                
                                if (!isDataLoaded.value) {
                                    // Show loading indicator while fetching initial page
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator()
                                    }
                                } else {
                                    PdfViewerScreen(
                                        uri = Uri.parse(key.uri),
                                        isExternal = key.isExternal,
                                        isFavoriteInitial = isFavorite.value,
                                        initialPage = initialPage.value!!,
                                        onFavoriteToggle = { uriStr, newState ->
                                            viewerScope.launch {
                                                if (newState) {
                                                    // Add to favorites (preserve current page if file exists in recent)
                                                    val file = java.io.File(if (uriStr.startsWith("file://")) uriStr.substring(7) else uriStr)
                                                    settingsRepository.toggleFavorite(uriStr, file.name, file.length())
                                                } else {
                                                    settingsRepository.removeFavorite(uriStr)
                                                }
                                            }
                                        },
                                        onPageChange = { page ->
                                            // Update page in Recent Files DB
                                            viewerScope.launch {
                                                settingsRepository.updateRecentFilePage(key.uri, page)
                                            }
                                        },
                                        initialVerticalScroll = scrollDirection == 0,
                                        initialScrollMode = scrollMode,
                                        onBackClick = {
                                            if (backStack.size <= 1) {
                                                // External file opened directly - close app on back
                                                (context as? android.app.Activity)?.finish()
                                            } else {
                                                backStack.removeLast()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        NavDisplay(
                            entries = backStack.mapNotNull { key ->
                                try {
                                    val navKey = key as? NavKey ?: return@mapNotNull null
                                    entryProvider.invoke(navKey)
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            onBack = {
                                if (backStack.size > 1) {
                                    backStack.removeLast()
                                } else {
                                    finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}