package com.example.data.repository

import android.content.Context
import com.example.data.api.GeminiImageService
import com.example.data.api.GeneratedImageResult
import com.example.data.db.AppDatabase
import com.example.data.db.BrandCampaignEntity
import com.example.data.db.CampaignShotEntity
import com.example.data.db.CampaignWithShots
import com.example.data.model.AdvertisingMedium
import com.example.data.model.ProductDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BrandRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val brandDao = db.brandDao()
    private val geminiService = GeminiImageService(context)

    val allCampaigns: Flow<List<CampaignWithShots>> = brandDao.getAllCampaignsWithShots()

    fun getService(): GeminiImageService = geminiService

    suspend fun getCampaignWithShots(campaignId: Long): CampaignWithShots? = withContext(Dispatchers.IO) {
        brandDao.getCampaignWithShotsById(campaignId)
    }

    suspend fun createCampaign(
        product: ProductDraft,
        heroResult: GeneratedImageResult
    ): Long = withContext(Dispatchers.IO) {
        val campaign = BrandCampaignEntity(
            productName = product.name,
            category = product.category,
            description = product.description,
            materials = product.materials,
            primaryColors = product.primaryColors,
            logoStyle = product.logoStyle,
            tagline = product.tagline,
            resolution = product.resolution,
            heroImagePath = heroResult.filePath,
            heroPrompt = heroResult.promptUsed,
            timestamp = System.currentTimeMillis()
        )
        val campaignId = brandDao.insertCampaign(campaign)

        // Also save the Master Hero Shot as the first shot
        val heroShot = CampaignShotEntity(
            campaignId = campaignId,
            mediumKey = "HERO_PACKSHOT",
            mediumTitle = "Master Product Hero Shot",
            aspectRatio = "1:1",
            resolution = product.resolution,
            imagePath = heroResult.filePath,
            prompt = heroResult.promptUsed
        )
        brandDao.insertShot(heroShot)

        campaignId
    }

    suspend fun addShot(
        campaignId: Long,
        medium: AdvertisingMedium,
        resolution: String,
        result: GeneratedImageResult
    ): Long = withContext(Dispatchers.IO) {
        val shot = CampaignShotEntity(
            campaignId = campaignId,
            mediumKey = medium.key,
            mediumTitle = medium.title,
            aspectRatio = medium.aspectRatio,
            resolution = resolution,
            imagePath = result.filePath,
            prompt = result.promptUsed
        )
        brandDao.insertShot(shot)
    }

    suspend fun deleteCampaign(campaignId: Long) = withContext(Dispatchers.IO) {
        brandDao.deleteCampaignById(campaignId)
    }

    suspend fun deleteShot(shotId: Long) = withContext(Dispatchers.IO) {
        brandDao.deleteShotById(shotId)
    }
}
