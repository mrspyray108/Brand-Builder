package com.example.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "brand_campaigns")
data class BrandCampaignEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val category: String,
    val description: String,
    val materials: String,
    val primaryColors: String,
    val logoStyle: String,
    val tagline: String,
    val resolution: String,
    val heroImagePath: String? = null,
    val heroPrompt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "campaign_shots",
    foreignKeys = [
        ForeignKey(
            entity = BrandCampaignEntity::class,
            parentColumns = ["id"],
            childColumns = ["campaignId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["campaignId"])]
)
data class CampaignShotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val campaignId: Long,
    val mediumKey: String,
    val mediumTitle: String,
    val aspectRatio: String,
    val resolution: String,
    val imagePath: String,
    val prompt: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CampaignWithShots(
    @Embedded val campaign: BrandCampaignEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "campaignId"
    )
    val shots: List<CampaignShotEntity>
)
