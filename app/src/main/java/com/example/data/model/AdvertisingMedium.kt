package com.example.data.model

enum class AdvertisingMedium(
    val key: String,
    val title: String,
    val subtitle: String,
    val aspectRatio: String,
    val iconName: String,
    val promptTemplate: String
) {
    BILLBOARD(
        key = "BILLBOARD",
        title = "Billboard Ad",
        subtitle = "Massive high-impact outdoor roadside display",
        aspectRatio = "16:9",
        iconName = "view_carousel",
        promptTemplate = "An epic, ultra-realistic premium outdoor highway advertising billboard mockup showcasing [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. The billboard is set in a striking modern architectural landscape under clean cinematic golden hour lighting. The massive billboard canvas prominently highlights the product with razor-sharp commercial photography. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO HANDS, NO FACES, NO DRIVERS. The scene must be entirely devoid of human beings. Pure high-end outdoor advertising photography."
    ),
    NEWSPAPER(
        key = "NEWSPAPER",
        title = "Newspaper Ad",
        subtitle = "Authentic tactile editorial print broadsheet",
        aspectRatio = "3:4",
        iconName = "menu_book",
        promptTemplate = "A realistic editorial printed newspaper advertisement featuring [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. Rich authentic tactile newsprint paper texture, crisp black ink and duotone printing, elegant typographic margins and headline layout. The printed newspaper page is resting on a clean studio table with soft natural window lighting. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO HANDS, NO FACES. Nobody holding or reading the paper. Pure editorial still life photography."
    ),
    SOCIAL_POST(
        key = "SOCIAL_POST",
        title = "Social Media Post",
        subtitle = "Aesthetic studio flat-lay & podium feed shot",
        aspectRatio = "1:1",
        iconName = "share",
        promptTemplate = "A trendsetting modern social media product campaign photograph of [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. Displayed on a minimalist geometric sculpted pedestal with artistic organic shadows, subtle studio props matching the product theme, and clean editorial lighting. Square composition ready for a viral brand campaign. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO HANDS, NO FACES. Pure product aesthetic still life."
    ),
    SUBWAY_POSTER(
        key = "SUBWAY_POSTER",
        title = "Subway Transit Poster",
        subtitle = "Backlit glowing transit ad frame",
        aspectRatio = "9:16",
        iconName = "subway",
        promptTemplate = "A large vertical backlit advertising display poster in a sleek, ultra-clean modern transit terminal showcasing [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. The illuminated display case casts a soft ambient glow onto polished architectural terrazzo floors. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO COMMUTERS, NO SILHOUETTES. Completely empty serene architectural transit station."
    ),
    MAGAZINE_SPREAD(
        key = "MAGAZINE_SPREAD",
        title = "Magazine Spread",
        subtitle = "Glossy double-page luxury editorial",
        aspectRatio = "4:3",
        iconName = "auto_stories",
        promptTemplate = "A double-page spread inside an open luxury design magazine displaying [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. Realistic paper curve, subtle glossy print sheen, high-fashion layout with minimalist typography. Resting on a dark stone studio surface. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO HANDS, NO BODIES. Pure editorial publication still life."
    ),
    STOREFRONT_WINDOW(
        key = "STOREFRONT_WINDOW",
        title = "Storefront Display",
        subtitle = "Luxury boutique window vitrine",
        aspectRatio = "1:1",
        iconName = "storefront",
        promptTemplate = "A high-end luxury retail boutique storefront window display featuring [PRODUCT_NAME] ([CATEGORY]). [ANCHOR]. Artistically curated podium pedestal inside a pristine glass vitrine with bespoke designer accent lighting and subtle reflections. STRICT RESTRICTION: ABSOLUTELY NO PEOPLE, NO HUMANS, NO PEDESTRIANS, NO FACES. Clean serene luxury store installation."
    );

    fun formatPrompt(productName: String, category: String, anchor: String): String {
        return promptTemplate
            .replace("[PRODUCT_NAME]", productName)
            .replace("[CATEGORY]", category)
            .replace("[ANCHOR]", anchor)
    }

    companion object {
        fun fromKey(key: String): AdvertisingMedium {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: BILLBOARD
        }
    }
}
