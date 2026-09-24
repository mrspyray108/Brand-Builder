package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.CampaignShotEntity
import com.example.data.db.CampaignWithShots
import com.example.data.model.AdvertisingMedium
import com.example.ui.BrandBuilderViewModel
import com.example.ui.components.BrandImage
import com.example.ui.components.HighResBadge
import com.example.ui.components.ModelBadge
import com.example.ui.components.NoPeopleBadge
import com.example.ui.theme.BananaGold
import com.example.ui.theme.ElectricCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampaignViewerScreen(
    viewModel: BrandBuilderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCampaign by viewModel.activeCampaign.collectAsState()
    var selectedFilterKey by remember { mutableStateOf<String?>("ALL") }

    if (activeCampaign == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No active campaign selected.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Return to Studio")
                }
            }
        }
        return
    }

    val campaign = activeCampaign!!.campaign
    val allShots = activeCampaign!!.shots

    // Find the Master Hero Packshot
    val heroShot = allShots.firstOrNull { it.mediumKey == "HERO_PACKSHOT" }
        ?: allShots.firstOrNull()

    // Medium shots (excluding hero shot if present, or all)
    val mediumShots = allShots.filter { it.mediumKey != "HERO_PACKSHOT" }

    val filteredShots = if (selectedFilterKey == "ALL") {
        mediumShots
    } else {
        mediumShots.filter { it.mediumKey == selectedFilterKey }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Navigation & Campaign Summary Card
        item {
            CampaignHeaderCard(
                campaign = campaign,
                shotCount = allShots.size,
                onBack = onBack,
                onDelete = { viewModel.deleteCampaign(campaign.id) }
            )
        }

        // Master Product Hero Anchor Card
        if (heroShot != null) {
            item {
                MasterHeroCard(
                    heroShot = heroShot,
                    productName = campaign.productName,
                    materials = campaign.materials,
                    colors = campaign.primaryColors,
                    onOpenFullscreen = { viewModel.openPreview(heroShot) }
                )
            }
        }

        // Section Title & Filters
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advertising Medium Shots",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HighResBadge(campaign.resolution)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilterKey == "ALL",
                            onClick = { selectedFilterKey = "ALL" },
                            label = { Text("All Mediums (${mediumShots.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BananaGold.copy(alpha = 0.25f),
                                selectedLabelColor = BananaGold
                            )
                        )
                    }
                    items(AdvertisingMedium.entries) { medium ->
                        val count = mediumShots.count { it.mediumKey == medium.key }
                        if (count > 0) {
                            FilterChip(
                                selected = selectedFilterKey == medium.key,
                                onClick = { selectedFilterKey = medium.key },
                                label = { Text("${medium.title} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }
            }
        }

        // Render each Medium Shot
        items(filteredShots, key = { it.id }) { shot ->
            MediumShotCard(
                shot = shot,
                onOpenFullscreen = { viewModel.openPreview(shot) },
                onCompareConsistency = { viewModel.openConsistencyComparison(shot) },
                onRegenerate = {
                    val medium = AdvertisingMedium.fromKey(shot.mediumKey)
                    viewModel.regenerateShot(medium)
                },
                onDelete = { viewModel.deleteShot(shot.id) }
            )
        }
    }
}

@Composable
private fun CampaignHeaderCard(
    campaign: com.example.data.db.BrandCampaignEntity,
    shotCount: Int,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoPeopleBadge()
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Campaign",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = campaign.productName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (campaign.tagline.isNotBlank()) {
                Text(
                    text = "\"${campaign.tagline}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = BananaGold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${campaign.category} • $shotCount Total Visual Assets",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            ModelBadge()
        }
    }
}

@Composable
private fun MasterHeroCard(
    heroShot: CampaignShotEntity,
    productName: String,
    materials: String,
    colors: String,
    onOpenFullscreen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, BananaGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = BananaGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Master Product Consistency Anchor",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BananaGold
                    )
                }

                IconButton(onClick = onOpenFullscreen) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Source reference image used to maintain uniform geometry and branding across all mediums.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .clickable { onOpenFullscreen() }
                    .testTag("hero_shot_image")
            ) {
                BrandImage(
                    imagePath = heroShot.imagePath,
                    contentDescription = "$productName Hero Shot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Locked Palette:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = colors,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Locked Materials:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = materials,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediumShotCard(
    shot: CampaignShotEntity,
    onOpenFullscreen: () -> Unit,
    onCompareConsistency: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit
) {
    val medium = AdvertisingMedium.fromKey(shot.mediumKey)
    val parsedAspect = parseAspectRatio(shot.aspectRatio)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shot_card_${shot.mediumKey}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = shot.mediumTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = shot.aspectRatio,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = medium.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete shot",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Image Container respecting Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(parsedAspect)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .clickable { onOpenFullscreen() }
                    .testTag("shot_image_${shot.mediumKey}")
            ) {
                BrandImage(
                    imagePath = shot.imagePath,
                    contentDescription = shot.mediumTitle,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // High-res & No People overlay chips in top-right
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NoPeopleBadge()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCompareConsistency,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("compare_consistency_button_${shot.mediumKey}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BananaGold.copy(alpha = 0.2f),
                        contentColor = BananaGold
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Compare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect Consistency", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onRegenerate,
                    modifier = Modifier.testTag("regenerate_button_${shot.mediumKey}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedButton(
                    onClick = onOpenFullscreen,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun parseAspectRatio(ratioStr: String): Float {
    return when (ratioStr) {
        "16:9" -> 16f / 9f
        "9:16" -> 9f / 16f
        "3:4" -> 3f / 4f
        "4:3" -> 4f / 3f
        "2:3" -> 2f / 3f
        "3:2" -> 3f / 2f
        else -> 1f
    }
}
