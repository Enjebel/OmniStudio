package com.example.model

import com.example.R

object AdStudioRepository {

    val demoProducts = listOf(
        DemoProduct(
            id = "prod_watch_1",
            name = "Chronos Royal Diamond",
            price = "$1,250",
            category = "Luxury Watches",
            description = "Handcrafted automatic chronograph with diamond bezel and carbon titanium finish.",
            drawableRes = R.drawable.prod_watch,
            defaultBadge = "EXCLUSIVE"
        ),
        DemoProduct(
            id = "prod_shoes_1",
            name = "AeroSprint Carbon Pro",
            price = "$189",
            category = "Athletic Footwear",
            description = "Ultra-responsive dynamic marathon sneaker with dual-density neon energy foam.",
            drawableRes = R.drawable.prod_shoes,
            defaultBadge = "NEW DROP"
        ),
        DemoProduct(
            id = "prod_perfume_1",
            name = "L'Élixir Noir Aeterna",
            price = "$115",
            category = "Haute Fragrance",
            description = "Faceted amber crystal extrait de parfum with golden oud and midnight vanilla notes.",
            drawableRes = R.drawable.prod_perfume,
            defaultBadge = "BEST SELLER"
        ),
        DemoProduct(
            id = "prod_serum_1",
            name = "HydraGlow Peptide Elixir",
            price = "$48",
            category = "Clean Skincare",
            description = "Triple hyaluronic acid bio-infusion with brightening 24k colloidal gold flakes.",
            drawableRes = R.drawable.prod_perfume,
            defaultBadge = "VIRAL TIKTOK"
        ),
        DemoProduct(
            id = "prod_earbuds_1",
            name = "SonicPods Spatial ANC",
            price = "$149",
            category = "Acoustic Tech",
            description = "Active noise cancelling with 48-hour battery, ceramic drivers, and spatial audio.",
            drawableRes = R.drawable.prod_watch,
            defaultBadge = "50% OFF"
        ),
        DemoProduct(
            id = "prod_speaker_1",
            name = "AuraSphere 360 Studio",
            price = "$89",
            category = "Smart Audio",
            description = "High-fidelity cylindrical acoustic room speaker with reactive ambient RGB ring.",
            drawableRes = R.drawable.prod_shoes,
            defaultBadge = "LIMITED"
        )
    )

    val templates = listOf(
        AdTemplate(
            id = "tpl_tiktok_punch",
            title = "TikTok Viral Punch",
            subtitle = "High-converting punch zooms & neon badges",
            platformTag = "TikTok / Reels",
            defaultRatio = AspectRatioOption.RATIO_9_16,
            motionPreset = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
            bgPreset = BackgroundPreset.CYBERPUNK_NEON,
            defaultHeadline = "STOP SCROLLING!",
            defaultOffer = "50% OFF TODAY ONLY",
            defaultCta = "SHOP NOW BEFORE IT'S GONE"
        ),
        AdTemplate(
            id = "tpl_insta_luxury",
            title = "Haute Luxury Showcase",
            subtitle = "Smooth floating camera with marble reflections",
            platformTag = "Instagram Reels",
            defaultRatio = AspectRatioOption.RATIO_9_16,
            motionPreset = MotionTemplate.FLOATING_LUXURY,
            bgPreset = BackgroundPreset.LUXURY_MARBLE,
            defaultHeadline = "Redefine Perfection",
            defaultOffer = "Complimentary Worldwide Express",
            defaultCta = "EXPLORE COLLECTION"
        ),
        AdTemplate(
            id = "tpl_youtube_cinematic",
            title = "Cinematic Product Promo",
            subtitle = "Dramatic lighting reveal with widescreen ratio",
            platformTag = "YouTube 16:9",
            defaultRatio = AspectRatioOption.RATIO_16_9,
            motionPreset = MotionTemplate.CINEMATIC_REVEAL,
            bgPreset = BackgroundPreset.TECH_GRID,
            defaultHeadline = "The Next Generation Is Here",
            defaultOffer = "Engineered Without Compromise",
            defaultCta = "ORDER YOURS TODAY"
        ),
        AdTemplate(
            id = "tpl_flash_glitch",
            title = "Flash Sale Glitch",
            subtitle = "Strobe flashes with high urgency CTAs",
            platformTag = "Feed / Story",
            defaultRatio = AspectRatioOption.RATIO_1_1,
            motionPreset = MotionTemplate.FLASH_SALE_GLITCH,
            bgPreset = BackgroundPreset.FASHION_RUNWAY,
            defaultHeadline = "MIDNIGHT FLASH DROP",
            defaultOffer = "FIRST 100 ORDERS GET FREE GIFT",
            defaultCta = "CLAIM YOUR DISCOUNT"
        ),
        AdTemplate(
            id = "tpl_prestige_gold",
            title = "Prestige Black & Gold",
            subtitle = "Refined obsidian with gold metallic rim",
            platformTag = "Instagram Feed",
            defaultRatio = AspectRatioOption.RATIO_1_1,
            motionPreset = MotionTemplate.ORBIT_3D,
            bgPreset = BackgroundPreset.LUXURY_BLACK_GOLD,
            defaultHeadline = "Unrivaled Craftsmanship",
            defaultOffer = "Numbered Limited Edition",
            defaultCta = "RESERVE NOW"
        ),
        AdTemplate(
            id = "tpl_beauty_sunbeam",
            title = "Organic Beauty Glow",
            subtitle = "Natural warm sunbeams and soft lighting",
            platformTag = "Portrait Ad",
            defaultRatio = AspectRatioOption.RATIO_4_5,
            motionPreset = MotionTemplate.FLOATING_LUXURY,
            bgPreset = BackgroundPreset.NATURE_SUNBEAMS,
            defaultHeadline = "Unlock Your Natural Radiance",
            defaultOffer = "100% Organic & Clinically Tested",
            defaultCta = "GET GLOWING TODAY"
        )
    )

    fun generateCopyForProduct(productName: String, category: AiCategory): GeneratedAdCopy {
        return when (category) {
            AiCategory.ECOMMERCE -> GeneratedAdCopy(
                hooks = listOf(
                    "Stop scrolling! This $productName is breaking the internet.",
                    "The #1 product everyone is talking about in 2026.",
                    "Why didn't anyone tell me about this sooner?",
                    "If you only buy one thing this month, make it this.",
                    "Warning: This is selling out faster than anything we've ever seen."
                ),
                headlines = listOf(
                    "Upgrade Your Daily Routine",
                    "The Ultimate Game Changer",
                    "Engineered For Everyday Excellence",
                    "Quality You Can Truly Feel",
                    "Over 50,000 Happy Customers Worldwide"
                ),
                offers = listOf(
                    "FLASH SALE: 50% OFF TODAY ONLY",
                    "Buy 1 Get 1 Free for the first 100 buyers",
                    "Limited Stock Available - Ships in 24 Hours",
                    "Free Express Delivery + 30-Day Money Back Guarantee",
                    "Special Launch Price: Save $50 Right Now"
                ),
                scripts = listOf(
                    "Are you still doing things the hard way? Meet the $productName. Designed with ultra-premium materials, built to last, and engineered to turn heads. Don't wait until it's sold out—tap below to get 50% off right now!",
                    "Here's why thousands of shoppers are obsessing over the $productName. It's sleek, ultra-reliable, and delivers unbeatable value right out of the box. Tap the link and claim your launch discount before stock runs out!",
                    "Tired of low-quality alternatives that let you down? We redesigned the $productName from scratch. Premium finish, guaranteed durability, and instant compliments. Tap shop now!",
                    "This one simple upgrade changed everything. The $productName is lightweight, lightning-fast, and unconditionally backed by our 30-day trial. Grab yours today!",
                    "Attention: Last call for our seasonal inventory drop. The $productName is currently 50% off, but only while supplies last. Tap to claim yours today!"
                ),
                ctas = listOf(
                    "SHOP NOW BEFORE IT'S GONE",
                    "CLAIM 50% OFF NOW",
                    "GET YOURS TODAY",
                    "ORDER NOW - LIMITED STOCK",
                    "UNLOCK EXCLUSIVE OFFER"
                )
            )
            AiCategory.LUXURY -> GeneratedAdCopy(
                hooks = listOf(
                    "True luxury whispers. Introducing the $productName.",
                    "For those who refuse to settle for ordinary.",
                    "Craftsmanship that outlasts generations.",
                    "The pinnacle of design, now within reach.",
                    "Elevate your signature presence with $productName."
                ),
                headlines = listOf(
                    "Mastery In Every Detail",
                    "A Heritage Of Uncompromising Elegance",
                    "Precision Engineered Horology & Luxury",
                    "Crafted For The Discerning Eye",
                    "Timeless Sophistication Redefined"
                ),
                offers = listOf(
                    "Numbered Limited Edition of 500 Pieces",
                    "Complimentary Bespoke Monogramming Included",
                    "White Glove Insured Courier Delivery Worldwide",
                    "Certificate of Authenticity & 5-Year Guarantee",
                    "Private Client Invitation Pricing"
                ),
                scripts = listOf(
                    "True craftsmanship cannot be hurried. The $productName is sculpted from titanium and sapphire, capturing light with breathtaking brilliance. Reserve your numbered piece today.",
                    "When perfection is your standard, only the $productName will suffice. Impeccable finishes, bespoke balance, and timeless pedigree. Discover the private atelier collection now.",
                    "Elevate your lifestyle with an icon of modern distinction. The $productName represents centuries of refined aesthetic excellence. Order your bespoke piece today.",
                    "Some possessions transcend mere utility to become art. Experience the weight, precision, and beauty of the $productName. Inquire today.",
                    "Step into a world of curated elegance. The $productName commands effortless admiration wherever you arrive. Available in strictly limited quantities."
                ),
                ctas = listOf(
                    "EXPLORE THE COLLECTION",
                    "RESERVE YOUR ALLOCATION",
                    "INQUIRE PRIVATELY",
                    "DISCOVER CRAFTSMANSHIP",
                    "ACQUIRE NOW"
                )
            )
            AiCategory.TECH -> GeneratedAdCopy(
                hooks = listOf(
                    "Next-gen audio tech just changed forever.",
                    "Ditch wires and latency for good with $productName.",
                    "Future hardware engineered for 2026 and beyond.",
                    "Is this the smartest device ever built?",
                    "Experience pure audio fidelity like never before."
                ),
                headlines = listOf(
                    "Spatial Audio Redefined",
                    "Zero Latency. Infinite Immersion.",
                    "Next-Gen Silicon Architecture",
                    "Hyper-Intuitive Touch & Voice Control",
                    "Unmatched 48-Hour Battery Endurance"
                ),
                offers = listOf(
                    "Save $40 On Early Access Pre-Orders",
                    "Includes Custom Wireless Fast-Charging Case",
                    "Free 3-Year Extended Tech Protection Plan",
                    "Try Risk-Free With 60-Day In-Home Return Policy",
                    "Trade In Any Old Device For An Instant $30 Credit"
                ),
                scripts = listOf(
                    "Immerse yourself into pure acoustic bliss. The $productName features spatial binaural drivers and adaptive active noise cancellation that silences the outside world. Upgrade your listening experience today!",
                    "Engineered with cutting-edge microchips, the $productName bridges seamless connectivity and studio-grade reproduction. One tap pairs with all your devices. Tap below to save $40 today!",
                    "Say goodbye to battery anxiety and tinny sound. The $productName packs 48 hours of uninterrupted playtime inside an aerodynamic featherweight shell. Order now!",
                    "Experience the future on your terms. Ultra-low latency, crystal voice pickups, and tactile haptics make the $productName an essential daily companion. Tap shop now!",
                    "Why compromise on your gear? The $productName delivers high-res lossless streaming with studio acoustics anywhere you roam. Grab the limited introductory bundle today!"
                ),
                ctas = listOf(
                    "GET NEXT-GEN TECH",
                    "ORDER WITH $40 OFF",
                    "UPGRADE YOUR GEAR",
                    "EXPERIENCE SPATIAL AUDIO",
                    "SHOP TECH LAUNCH"
                )
            )
            AiCategory.BEAUTY -> GeneratedAdCopy(
                hooks = listOf(
                    "The secret to glass skin in just 7 days.",
                    "Dermatologists are raving about this $productName.",
                    "Get that radiant golden-hour glow 24/7.",
                    "Say goodbye to dull skin with one single dropper.",
                    "Your skin barrier will thank you forever for this."
                ),
                headlines = listOf(
                    "Clinical Peptides & Pure Botanical Gold",
                    "Deep Cellular Hydration That Lasts",
                    "Clean, Vegan, & Dermatologist Tested",
                    "Wake Up To Youthful Radiant Texture",
                    "Gentle Power For Every Skin Type"
                ),
                offers = listOf(
                    "Buy 1 Serum, Get Dewy Mist 50% Off",
                    "Free Luxury Silk Cosmetic Pouch With Every Order",
                    "Subscribe & Save 25% + Free Shipping Forever",
                    "Results Guaranteed in 14 Days Or Full Refund",
                    "Limited Batch Release - Clean Small-Batch Crafted"
                ),
                scripts = listOf(
                    "Reveal your most luminous complexion yet with the $productName. Formulated with peptide complexes and clean botanicals, it melts deep into your skin to lock in moisture for 72 hours. Tap to start your glow journey!",
                    "No heavy filters needed when your skin glows from within. The $productName delivers immediate plumping and restorative hydration that leaves skin glass-smooth. Claim your bottle today!",
                    "Give your skincare routine the holy grail it has been missing. 100% clean, cruelty-free, and formulated to restore youthfulness and firmness overnight. Tap below for special launch pricing!",
                    "Meet the serum that sold out three times this year. The $productName is lightweight, non-comedogenic, and infused with antioxidant gold peptides. Order now before stock depletes!",
                    "Real transformation without harsh chemicals. Experience soft, radiant, rejuvenated skin from day one with the $productName. Tap shop now and receive a free gift!"
                ),
                ctas = listOf(
                    "UNLOCK YOUR GLOW",
                    "CLAIM YOUR BOTTLE",
                    "START 7-DAY TRIAL",
                    "SHOP CLEAN BEAUTY",
                    "GET 25% OFF NOW"
                )
            )
            AiCategory.FITNESS -> GeneratedAdCopy(
                hooks = listOf(
                    "Break your personal marathon record this weekend.",
                    "These shoes literally bounce you forward with every stride.",
                    "Engineered for athletes who refuse to slow down.",
                    "The most comfortable high-performance runner on earth.",
                    "Carbon fiber energy return you have to feel to believe."
                ),
                headlines = listOf(
                    "Propulsion Re-Engineered",
                    "Hyper-Responsive Carbon Plate Midsole",
                    "Featherlight Breathable Fly-Weave",
                    "Traction That Grips Any Surface",
                    "Endurance Tested Over 1,000 Miles"
                ),
                offers = listOf(
                    "Introductory Drop: Save $30 + Free Race Socks",
                    "Free 30-Day Road Trial - Run In Them Or Return",
                    "Includes Pro Training App 6-Month Premium Pass",
                    "Free Express Tracked Delivery",
                    "Bundle With Carbon Insoles For 40% Off"
                ),
                scripts = listOf(
                    "Ready to shatter your boundaries? The $productName combines a featherlight carbon plate with explosive dual-density foam, converting every footstrike into pure forward propulsion. Lace up today!",
                    "Don't let heavy sneakers hold your stamina back. The $productName weighs under 7 ounces yet delivers maximum impact dampening for all-day running comfort. Tap to claim your pair!",
                    "Engineered alongside elite marathon racers, the $productName provides unmatched stability through every curve and stride. Tap shop now and experience lightning speed!",
                    "Feel the difference of true biomechanical propulsion. The $productName gives you energy return that keeps your legs fresh through mile 26. Order now before your size sells out!",
                    "Elevate your workout game with the most futuristic sneaker of 2026. Breathable, responsive, and insanely durable. Tap below to get free express shipping!"
                ),
                ctas = listOf(
                    "LACE UP FOR SPEED",
                    "GET YOUR PAIR TODAY",
                    "CLAIM $30 OFF",
                    "START ROAD TRIAL",
                    "SHOP PERFORMANCE DROP"
                )
            )
        }
    }
}
