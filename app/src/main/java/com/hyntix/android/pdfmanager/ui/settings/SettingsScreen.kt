package com.hyntix.android.pdfmanager.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.Trash
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.regular.Scroll
import com.adamglin.phosphoricons.regular.Info
import com.adamglin.phosphoricons.regular.Code
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLegalClick: (String) -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState(viewModel.scrollPosition)
    
    // Sync scroll position back to ViewModel to survive navigation
    LaunchedEffect(scrollState.value) {
        viewModel.scrollPosition = scrollState.value
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(PhosphorIcons.Regular.ArrowLeft, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Theme Setting
            SettingsSectionTitle("Appearance")
            
            ThemeSelector(
                currentTheme = uiState.appTheme,
                onThemeSelected = { viewModel.setAppTheme(it) }
            )
            
            ListItem(
                headlineContent = { Text("Grayscale Mode") },
                supportingContent = { Text("Applies a black & white filter to the entire app") },
                trailingContent = {
                    Switch(
                        checked = uiState.grayscaleMode,
                        onCheckedChange = { viewModel.toggleGrayscaleMode(it) }
                    )
                }
            )

            HorizontalDivider()
            
            // Viewer Settings
            SettingsSectionTitle("Viewer")
            
            ListItem(
                headlineContent = { Text("Keep Screen Awake") },
                supportingContent = { Text("Prevents screen from turning off while reading") },
                trailingContent = {
                    Switch(
                        checked = uiState.keepScreenOn,
                        onCheckedChange = { viewModel.toggleKeepScreenOn(it) }
                    )
                }

            )
            
            ScrollDirectionSelector(
                currentDirection = uiState.scrollDirection,
                onDirectionSelected = { viewModel.setScrollDirection(it) }
            )
            
            ScrollModeSelector(
                currentMode = uiState.scrollMode,
                onModeSelected = { viewModel.setScrollMode(it) }
            )
            
            HorizontalDivider()
            SettingsSectionTitle("Data & Storage")
            
            ListItem(
                headlineContent = { Text("Clear Recent Files") },
                supportingContent = { Text("Removes all files from the Recent list") },
                leadingContent = { Icon(PhosphorIcons.Regular.Trash, contentDescription = null) },
                modifier = Modifier.clickable {
                    viewModel.clearRecentFiles()
                    scope.launch {
                        snackbarHostState.showSnackbar("Recent files cleared")
                    }
                }
            )
            
            ListItem(
                headlineContent = { Text("Clear Cache") },
                supportingContent = { Text("Frees up space by clearing temp files") },
                leadingContent = { Icon(PhosphorIcons.Regular.Trash, contentDescription = null) },
                modifier = Modifier.clickable {
                    viewModel.clearCache()
                    scope.launch {
                        snackbarHostState.showSnackbar("Cache cleared")
                    }
                }
            )

            HorizontalDivider()
            
            // About & Legal
            SettingsSectionTitle("About")
            
            ListItem(
                headlineContent = { Text("Privacy Policy") },
                leadingContent = { Icon(PhosphorIcons.Regular.ShieldCheck, contentDescription = null) },
                modifier = Modifier.clickable { onLegalClick("privacy") }
            )
            
            ListItem(
                headlineContent = { Text("Terms of Service") },
                leadingContent = { Icon(PhosphorIcons.Regular.Scroll, contentDescription = null) },
                modifier = Modifier.clickable { onLegalClick("terms") }
            )

            ListItem(
                headlineContent = { Text("Open Source Licenses") },
                leadingContent = { Icon(PhosphorIcons.Regular.Code, contentDescription = null) },
                modifier = Modifier.clickable { onLegalClick("licenses") }
            )


            
            ListItem(
                headlineContent = { Text("Version") },
                supportingContent = { Text("1.0.0") },
                leadingContent = { Icon(PhosphorIcons.Regular.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ThemeSelector(
    currentTheme: Int,
    onThemeSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = "App Theme",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentTheme == 0,
                onClick = { onThemeSelected(0) }
            )
            Text("System Default", modifier = Modifier.padding(start = 8.dp).clickable { onThemeSelected(0) })
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentTheme == 1,
                onClick = { onThemeSelected(1) }
            )
            Text("Light", modifier = Modifier.padding(start = 8.dp).clickable { onThemeSelected(1) })
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentTheme == 2,
                onClick = { onThemeSelected(2) }
            )
            Text("Dark", modifier = Modifier.padding(start = 8.dp).clickable { onThemeSelected(2) })
        }
    }
}

@Composable
fun ScrollDirectionSelector(
    currentDirection: Int,
    onDirectionSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = "Default Scroll Direction",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentDirection == 0,
                onClick = { onDirectionSelected(0) }
            )
            Text("Vertical", modifier = Modifier.padding(start = 8.dp).clickable { onDirectionSelected(0) })
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentDirection == 1,
                onClick = { onDirectionSelected(1) }
            )
            Text("Horizontal", modifier = Modifier.padding(start = 8.dp).clickable { onDirectionSelected(1) })
        }
    }
}

@Composable
fun ScrollModeSelector(
    currentMode: Int,
    onModeSelected: (Int) -> Unit
) {
    Column {
        Text(
            text = "Scroll Mode",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == 0,
                onClick = { onModeSelected(0) }
            )
            Text("Continuous", modifier = Modifier.padding(start = 8.dp).clickable { onModeSelected(0) })
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = currentMode == 1,
                onClick = { onModeSelected(1) }
            )
            Text("Page by Page", modifier = Modifier.padding(start = 8.dp).clickable { onModeSelected(1) })
        }
    }
}
