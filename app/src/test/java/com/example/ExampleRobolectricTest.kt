package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AdvertisingMedium
import com.example.data.model.ProductDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Brand Builder", appName)
    }

    @Test
    fun `test product consistency anchor contains visual identity specs`() {
        val draft = ProductDraft(
            name = "AURA",
            category = "Luxury Skincare Serum",
            description = "Heavy frosted emerald green glass bottle with gold embossed geometric sun logo",
            materials = "Frosted glass, brushed brass",
            primaryColors = "Emerald Green, Gold",
            logoStyle = "Minimalist geometric serif logotype 'AURA'",
            tagline = "The Pure Essence of Radiance",
            selectedMediums = setOf(AdvertisingMedium.BILLBOARD, AdvertisingMedium.NEWSPAPER, AdvertisingMedium.SOCIAL_POST),
            resolution = "2K"
        )

        val anchor = draft.buildConsistencyAnchor()
        assertTrue(anchor.contains("AURA"))
        assertTrue(anchor.contains("Frosted glass"))
        assertTrue(anchor.contains("Emerald Green"))
        assertTrue(anchor.contains("The Pure Essence of Radiance"))
    }

    @Test
    fun `test advertising medium templates enforce no humans`() {
        AdvertisingMedium.entries.forEach { medium ->
            val template = medium.promptTemplate
            assertTrue(
                "Medium ${medium.name} prompt must enforce NO PEOPLE/HUMANS",
                template.contains("NO PEOPLE") || template.contains("NO HUMANS")
            )
        }
    }
}
