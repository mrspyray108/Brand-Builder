package com.example.data.model

data class ProductDraft(
    val name: String = "AURA",
    val category: String = "Luxury Skincare Serum",
    val description: String = "Heavy frosted emerald green glass bottle with gold embossed geometric sun logo, brushed brass precision dropper cap, and pure translucent amber essence.",
    val materials: String = "Frosted emerald glass, brushed brass, matte paper label",
    val primaryColors: String = "Deep Emerald Green, Champagne Gold, Translucent Amber",
    val logoStyle: String = "Minimalist geometric serif logotype 'AURA' with an embossed radiant sun emblem",
    val tagline: String = "The Pure Essence of Radiance",
    val selectedMediums: Set<AdvertisingMedium> = setOf(
        AdvertisingMedium.BILLBOARD,
        AdvertisingMedium.NEWSPAPER,
        AdvertisingMedium.SOCIAL_POST
    ),
    val resolution: String = "2K" // High-resolution output ("2K" or "1K")
) {
    fun buildConsistencyAnchor(): String {
        return "PRODUCT VISUAL ANCHOR (MUST PRESERVE 100% VISUAL IDENTITY): " +
                "Item: $name. Category: $category. " +
                "Design details: $description. " +
                "Exact Materials: $materials. " +
                "Palette: $primaryColors. " +
                "Branding/Logo: $logoStyle. " +
                (if (tagline.isNotBlank()) "Tagline: \"$tagline\". " else "") +
                "CRITICAL: Maintain strict physical and brand consistency of the product packaging, colors, textures, and emblem across all angles."
    }
}

data class PresetProduct(
    val title: String,
    val draft: ProductDraft
)

object ProductPresets {
    val PRESETS = listOf(
        PresetProduct(
            title = "AURA Skincare Serum",
            draft = ProductDraft(
                name = "AURA Botanica",
                category = "Luxury Botanical Serum",
                description = "Cylindrical frosted emerald green glass bottle with gold embossed geometric sun logo, brushed brass dropper pipette, and crystal amber fluid within.",
                materials = "Heavy frosted glass, brushed gold metal, micro-embossed textured label",
                primaryColors = "Deep Emerald Green, Brushed Champagne Gold, Amber",
                logoStyle = "Minimalist geometric serif logotype 'AURA' with an embossed radial sun crest",
                tagline = "The Pure Essence of Radiance",
                selectedMediums = setOf(AdvertisingMedium.BILLBOARD, AdvertisingMedium.NEWSPAPER, AdvertisingMedium.SOCIAL_POST),
                resolution = "2K"
            )
        ),
        PresetProduct(
            title = "VORTEX Titanium Earbuds",
            draft = ProductDraft(
                name = "VORTEX X1",
                category = "Spatial Audio Wireless Earbuds",
                description = "Aerospace grade bead-blasted matte titanium charging case with a flush pulsing sapphire-blue illuminated LED ring indicator and sculpted ergonomic ceramic earbuds.",
                materials = "Bead-blasted titanium, matte ceramic, sapphire glass accents",
                primaryColors = "Dark Space Titanium, Electric Cyan Glow, Matte Obsidian",
                logoStyle = "Laser-etched continuous spiral vector glyph 'VORTEX'",
                tagline = "Silence The World. Hear The Future.",
                selectedMediums = setOf(AdvertisingMedium.BILLBOARD, AdvertisingMedium.NEWSPAPER, AdvertisingMedium.SOCIAL_POST),
                resolution = "2K"
            )
        ),
        PresetProduct(
            title = "LUMINA Artisanal Cold Brew",
            draft = ProductDraft(
                name = "LUMINA Reserve",
                category = "Single-Origin Nitro Cold Brew",
                description = "Vintage-inspired ribbed amber glass apothecary bottle with a sealed black wax neck dip and a letterpress debossed textured parchment label.",
                materials = "Ribbed apothecary amber glass, black sealing wax, heavy cotton parchment paper",
                primaryColors = "Rich Amber Glass, Raw Kraft Cotton White, Matte Charcoal Black",
                logoStyle = "Handcrafted vintage arch serif typography 'LUMINA RESERVE 1928'",
                tagline = "Steeped Slow. Brewed Sacred.",
                selectedMediums = setOf(AdvertisingMedium.BILLBOARD, AdvertisingMedium.NEWSPAPER, AdvertisingMedium.SOCIAL_POST),
                resolution = "2K"
            )
        ),
        PresetProduct(
            title = "CHRONOS Minimalist Watch",
            draft = ProductDraft(
                name = "CHRONOS Horizon",
                category = "Luxury Ceramic Automatic Timepiece",
                description = "Monolithic satin-finish white ceramic watch case with a midnight navy sunburst dial, floating brushed rhodium indices, and an integrated textured fluoroelastomer strap.",
                materials = "Satin white ceramic, sapphire crystal, brushed rhodium, textured strap",
                primaryColors = "Pure White Ceramic, Midnight Sunburst Navy, Brushed Silver",
                logoStyle = "Clean Swiss-style geometric sans-serif emblem 'CHRONOS'",
                tagline = "Time, Stripped to Pure Form.",
                selectedMediums = setOf(AdvertisingMedium.BILLBOARD, AdvertisingMedium.NEWSPAPER, AdvertisingMedium.SOCIAL_POST),
                resolution = "2K"
            )
        )
    )
}
