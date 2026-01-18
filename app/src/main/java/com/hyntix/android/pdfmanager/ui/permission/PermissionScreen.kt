package com.hyntix.android.pdfmanager.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.regular.FolderLock
import com.adamglin.phosphoricons.regular.ShieldCheck
import com.adamglin.phosphoricons.fill.HandPointing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit,
    onPermissionRequested: () -> Unit
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Check loop or life-cycle observation is handled in MainActivity or here.
    // Since MainActivity handles the check onResume, this screen basically just provides the UI and Action.

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.FolderLock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Storage Access Required",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "To browse, manage, and organize your PDF files, this app needs access to your device's storage.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Integrated Animation
                PermissionGuideAnimation()

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Please grant 'All Files Access' in the settings screen that follows.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { 
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                onPermissionRequested()
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                intent.addCategory("android.intent.category.DEFAULT")
                                intent.data = Uri.parse(String.format("package:%s", context.packageName))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent()
                                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                context.startActivity(intent)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp) 
                ) {
                    Text("Grant Access")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ShieldCheck,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your data is safe and processed locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionGuideAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "guide")
    val toggleState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "toggle"
    )

    // Derived states for animation phases
    // 0.0 - 0.5: Move hand to switch
    // 0.5 - 0.6: Toggle On
    // 0.6 - 1.0: Stay On
    
    val switchOn = toggleState > 0.5f
    val handAlpha = if (toggleState > 0.8f) 0f else 1f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Allow access to manage all files",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = switchOn,
                onCheckedChange = null // Read only
            )
        }
        
        // Hand Icon Overlay
        Icon(
            imageVector = PhosphorIcons.Fill.HandPointing, 
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .offset(x = 120.dp + (if (toggleState < 0.5f) (toggleState * 40).dp else 20.dp), y = 20.dp) // Simple move simulation
                .alpha(handAlpha),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
