package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeneratedImageResult
import com.example.data.db.CampaignShotEntity
import com.example.data.db.CampaignWithShots
import com.example.data.model.AdvertisingMedium
import com.example.data.model.PresetProduct
import com.example.data.model.ProductDraft
import com.example.data.model.ProductPresets
import com.example.data.repository.BrandRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    CREATOR,
    CAMPAIGN_VIEWER,
    HISTORY,
    SETTINGS
}

data class GenerationState(
    val isGenerating: Boolean = false,
    val stageMessage: String = "",
    val progress: Float = 0f,
    val totalSteps: Int = 0,
    val currentStep: Int = 0,
    val error: String? = null
)

class BrandBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BrandRepository(application)
    private val prefs = application.getSharedPreferences("brand_builder_prefs", Context.MODE_PRIVATE)

    val savedCampaigns: StateFlow<List<CampaignWithShots>> = repository.allCampaigns
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(AppScreen.CREATOR)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _productDraft = MutableStateFlow(ProductPresets.PRESETS.first().draft)
    val productDraft: StateFlow<ProductDraft> = _productDraft.asStateFlow()

    private val _activeCampaign = MutableStateFlow<CampaignWithShots?>(null)
    val activeCampaign: StateFlow<CampaignWithShots?> = _activeCampaign.asStateFlow()

    private var activeHeroBase64: String? = null

    private val _generationState = MutableStateFlow(GenerationState())
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Inspecting / comparing shot state
    private val _previewShot = MutableStateFlow<CampaignShotEntity?>(null)
    val previewShot: StateFlow<CampaignShotEntity?> = _previewShot.asStateFlow()

    private val _comparisonShot = MutableStateFlow<CampaignShotEntity?>(null)
    val comparisonShot: StateFlow<CampaignShotEntity?> = _comparisonShot.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateDraft(update: (ProductDraft) -> ProductDraft) {
        _productDraft.value = update(_productDraft.value)
    }

    fun applyPreset(preset: PresetProduct) {
        _productDraft.value = preset.draft
    }

    fun toggleMedium(medium: AdvertisingMedium) {
        val current = _productDraft.value.selectedMediums
        val updated = if (current.contains(medium)) {
            if (current.size > 1) current - medium else current // Keep at least one
        } else {
            current + medium
        }
        _productDraft.value = _productDraft.value.copy(selectedMediums = updated)
    }

    fun setResolution(res: String) {
        _productDraft.value = _productDraft.value.copy(resolution = res)
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun openPreview(shot: CampaignShotEntity?) {
        _previewShot.value = shot
    }

    fun openConsistencyComparison(shot: CampaignShotEntity?) {
        _comparisonShot.value = shot
    }

    fun selectCampaign(campaignWithShots: CampaignWithShots) {
        _activeCampaign.value = campaignWithShots
        // Load hero image base64 for consistency chaining if needed
        val heroPath = campaignWithShots.campaign.heroImagePath
        if (!heroPath.isNullOrBlank()) {
            activeHeroBase64 = repository.getService().convertFileToBase64(heroPath)
        }
        _currentScreen.value = AppScreen.CAMPAIGN_VIEWER
    }

    fun deleteCampaign(campaignId: Long) {
        viewModelScope.launch {
            repository.deleteCampaign(campaignId)
            if (_activeCampaign.value?.campaign?.id == campaignId) {
                _activeCampaign.value = null
                _currentScreen.value = AppScreen.CREATOR
            }
        }
    }

    fun deleteShot(shotId: Long) {
        viewModelScope.launch {
            repository.deleteShot(shotId)
            _activeCampaign.value?.campaign?.id?.let { campaignId ->
                val refreshed = repository.getCampaignWithShots(campaignId)
                _activeCampaign.value = refreshed
            }
        }
    }

    /**
     * Executes the Campaign Generation flow:
     * 1. Generates Master Product Hero Shot with Nano-Banana (gemini-2.5-flash-image)
     * 2. Uses Hero Shot as reference image + consistency anchor to generate each selected medium
     *    (Billboard, Newspaper, Social Post, etc.)
     * 3. Strictly enforces NO HUMANS / NO PEOPLE across all shots
     * 4. Saves everything to Room Database
     */
    fun startCampaignGeneration() {
        val draft = _productDraft.value
        val mediums = draft.selectedMediums.toList()
        val totalSteps = 1 + mediums.size // 1 hero shot + N medium shots

        viewModelScope.launch {
            _generationState.value = GenerationState(
                isGenerating = true,
                stageMessage = "Initializing Nano-Banana engine (${repository.getService().modelName})...",
                progress = 0.05f,
                totalSteps = totalSteps,
                currentStep = 0,
                error = null
            )

            val effectiveKey = _customApiKey.value

            // Step 1: Master Hero Packshot
            _generationState.value = _generationState.value.copy(
                stageMessage = "Generating Master Product Hero Shot for '${draft.name}'...",
                progress = 1f / totalSteps * 0.5f,
                currentStep = 1
            )

            val heroResult = repository.getService().generateHeroShot(draft, effectiveKey)
            if (heroResult.isFailure) {
                val errorMsg = heroResult.exceptionOrNull()?.localizedMessage ?: "Failed to generate Hero Shot"
                _generationState.value = _generationState.value.copy(
                    isGenerating = false,
                    error = errorMsg
                )
                return@launch
            }

            val hero = heroResult.getOrThrow()
            activeHeroBase64 = hero.base64

            // Persist campaign in Room
            val campaignId = repository.createCampaign(draft, hero)

            // Step 2..N: Generate each medium using the Hero shot as consistency anchor
            for ((index, medium) in mediums.withIndex()) {
                val stepNum = index + 2
                _generationState.value = _generationState.value.copy(
                    stageMessage = "Generating ${medium.title} (No people • Maintaining product consistency)...",
                    progress = stepNum.toFloat() / totalSteps,
                    currentStep = stepNum
                )

                val mediumResult = repository.getService().generateMediumShot(
                    medium = medium,
                    product = draft,
                    heroReferenceBase64 = activeHeroBase64,
                    apiKey = effectiveKey
                )

                if (mediumResult.isSuccess) {
                    val result = mediumResult.getOrThrow()
                    repository.addShot(campaignId, medium, draft.resolution, result)
                } else {
                    // Log error but continue with other mediums if possible
                    val err = mediumResult.exceptionOrNull()?.localizedMessage
                    android.util.Log.e("BrandBuilderVM", "Medium ${medium.title} failed: $err")
                }
            }

            // Load newly created campaign into active state
            val freshCampaign = repository.getCampaignWithShots(campaignId)
            _activeCampaign.value = freshCampaign

            _generationState.value = GenerationState(isGenerating = false, stageMessage = "Complete!")
            _currentScreen.value = AppScreen.CAMPAIGN_VIEWER
        }
    }

    /**
     * Re-generates a single shot in an existing campaign while preserving product consistency.
     */
    fun regenerateShot(medium: AdvertisingMedium) {
        val currentCamp = _activeCampaign.value ?: return
        val draft = ProductDraft(
            name = currentCamp.campaign.productName,
            category = currentCamp.campaign.category,
            description = currentCamp.campaign.description,
            materials = currentCamp.campaign.materials,
            primaryColors = currentCamp.campaign.primaryColors,
            logoStyle = currentCamp.campaign.logoStyle,
            tagline = currentCamp.campaign.tagline,
            resolution = currentCamp.campaign.resolution
        )

        viewModelScope.launch {
            _generationState.value = GenerationState(
                isGenerating = true,
                stageMessage = "Re-rendering ${medium.title} with Nano-Banana...",
                progress = 0.5f,
                totalSteps = 1,
                currentStep = 1
            )

            val res = repository.getService().generateMediumShot(
                medium = medium,
                product = draft,
                heroReferenceBase64 = activeHeroBase64,
                apiKey = _customApiKey.value
            )

            if (res.isSuccess) {
                repository.addShot(currentCamp.campaign.id, medium, draft.resolution, res.getOrThrow())
                _activeCampaign.value = repository.getCampaignWithShots(currentCamp.campaign.id)
                _generationState.value = GenerationState(isGenerating = false)
            } else {
                _generationState.value = GenerationState(
                    isGenerating = false,
                    error = res.exceptionOrNull()?.localizedMessage ?: "Failed to regenerate shot"
                )
            }
        }
    }

    fun dismissError() {
        _generationState.value = _generationState.value.copy(error = null)
    }
}
