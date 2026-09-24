package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BrandDao {

    @Transaction
    @Query("SELECT * FROM brand_campaigns ORDER BY timestamp DESC")
    fun getAllCampaignsWithShots(): Flow<List<CampaignWithShots>>

    @Transaction
    @Query("SELECT * FROM brand_campaigns WHERE id = :campaignId")
    suspend fun getCampaignWithShotsById(campaignId: Long): CampaignWithShots?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: BrandCampaignEntity): Long

    @Update
    suspend fun updateCampaign(campaign: BrandCampaignEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: CampaignShotEntity): Long

    @Query("DELETE FROM campaign_shots WHERE id = :shotId")
    suspend fun deleteShotById(shotId: Long)

    @Query("DELETE FROM brand_campaigns WHERE id = :campaignId")
    suspend fun deleteCampaignById(campaignId: Long)
}
