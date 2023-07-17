package de.marmaro.krt.ffupdater.app.entity

import androidx.annotation.Keep

@Keep
enum class DisplayCategory {
    FROM_MOZILLA, // this category alone is good enough
    BASED_ON_FIREFOX, // can be combined with other categories
    GOOD_PRIVACY_BROWSER, // can be combined with other categories
    GOOD_SECURITY_BROWSER, // can be combined with other categories
    BETTER_THAN_GOOGLE_CHROME, // this category alone is good enough
    EOL, // can be combined with other categories
    OTHER
}