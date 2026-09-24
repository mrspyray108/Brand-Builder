package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.dialogs.ConsistencyInspectorDialog
import com.example.ui.dialogs.ImageDetailDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.CampaignHistoryScreen
import com.example.ui.screens.CampaignViewerScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.BananaGold

@Composable
fun MainAppScaffold(
    viewModel: BrandBuilderViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeCampaign by viewModel.activeCampaign.collectAsState()
    val previewShot by viewModel.previewShot.collectAsState()
    val comparisonShot by viewModel.comparisonShot.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.CREATOR,
                    onClick = { viewModel.navigateTo(AppScreen.CREATOR) },
                    icon = { Icon(Icons.Default.Create, contentDescription = "Studio") },
                    label = { Text("Studio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BananaGold,
                        selectedTextColor = BananaGold,
                        indicatorColor = BananaGold.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_studio")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.CAMPAIGN_VIEWER,
                    onClick = { viewModel.navigateTo(AppScreen.CAMPAIGN_VIEWER) },
                    icon = { Icon(Icons.Default.ViewCarousel, contentDescription = "Campaign Shots") },
                    label = { Text("Campaign") },
                    enabled = activeCampaign != null,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BananaGold,
                        selectedTextColor = BananaGold,
                        indicatorColor = BananaGold.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_campaign")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.HISTORY,
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Archives") },
                    label = { Text("Archives") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BananaGold,
                        selectedTextColor = BananaGold,
                        indicatorColor = BananaGold.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_history")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.CREATOR -> StudioScreen(
                    viewModel = viewModel,
                    onOpenSettings = { showSettingsDialog = true }
                )
                AppScreen.CAMPAIGN_VIEWER -> CampaignViewerScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.CREATOR) }
                )
                AppScreen.HISTORY -> CampaignHistoryScreen(
                    viewModel = viewModel,
                    onSelectCampaign = { campaignWithShots ->
                        viewModel.selectCampaign(campaignWithShots)
                    }
                )
                AppScreen.SETTINGS -> {
                    // Fallback to Studio
                    StudioScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }
            }
        }

        // Fullscreen image viewer dialog
        if (previewShot != null) {
            ImageDetailDialog(
                shot = previewShot!!,
                onDismiss = { viewModel.openPreview(null) }
            )
        }

        // Consistency comparison dialog
        if (comparisonShot != null && activeCampaign != null) {
            ConsistencyInspectorDialog(
                campaignWithShots = activeCampaign!!,
                comparisonShot = comparisonShot!!,
                onDismiss = { viewModel.openConsistencyComparison(null) }
            )
        }

        // Settings dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentCustomKey = customApiKey,
                onSaveCustomKey = { key -> viewModel.setCustomApiKey(key) },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}
