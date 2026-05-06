package com.example.free2party.data.model

import com.example.free2party.R
import com.example.free2party.util.removeAccents
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class DatePattern(val patternResId: Int, val labelResId: Int) {
    YYYY_MM_DD(
        R.string.date_pattern_yyyy_mm_dd,
        R.string.label_date_yyyy_mm_dd
    ),
    MM_DD_YYYY(
        R.string.date_pattern_mm_dd_yyyy,
        R.string.label_date_mm_dd_yyyy
    ),
    DD_MM_YYYY(
        R.string.date_pattern_dd_mm_yyyy,
        R.string.label_date_dd_mm_yyyy
    )
}

enum class ThemeMode(val labelResId: Int) {
    AUTOMATIC(R.string.label_theme_mode_automatic),
    LIGHT(R.string.label_theme_mode_light),
    DARK(R.string.label_theme_mode_dark)
}

enum class Gender(val labelResId: Int) {
    MAN(R.string.label_gender_man),
    WOMAN(R.string.label_gender_woman),
    OTHER(R.string.label_gender_other);

    /**
     * Maps a base resource ID to its gendered variant without using reflection.
     * Add new gender-sensitive strings here.
     */
    fun getStringRes(baseResId: Int): Int {
        if (this != WOMAN) return baseResId

        return when (baseResId) {
            R.string.label_status_busy -> R.string.label_status_busy_woman
            else -> baseResId
        }
    }
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.AUTOMATIC,
    val gradientBackground: Boolean = true,
    val use24HourFormat: Boolean = true,
    val datePattern: DatePattern = DatePattern.YYYY_MM_DD
)

data class UserSocials(
    val whatsappNumber: String = "",
    val whatsappCountryCode: String = "",
    val whatsappFullNumber: String = "",
    val telegramUsername: String = "",
    val facebookUsername: String = "",
    val instagramUsername: String = "",
    val tiktokUsername: String = "",
    @get:PropertyName("xUsername")
    @field:PropertyName("xUsername")
    val xUsername: String = ""
)

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val profilePicUrl: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val countryCode: String = "",
    val phoneNumber: String = "",
    val birthday: String = "",
    val bio: String = "",
    val gender: Gender = Gender.OTHER,
    val socials: UserSocials = UserSocials(),
    @get:PropertyName("isFreeNow")
    val isFreeNow: Boolean = false,
    @get:PropertyName("isStatusFromPlan")
    val isStatusFromPlan: Boolean = false,
    val settings: UserSettings = UserSettings(),
    @ServerTimestamp
    val createdAt: Date? = null
) {
    @get:PropertyName("firstNameLowercase")
    @Suppress("unused")
    val firstNameLowercase: String get() = firstName.lowercase().removeAccents()

    @get:PropertyName("lastNameLowercase")
    @Suppress("unused")
    val lastNameLowercase: String get() = lastName.lowercase().removeAccents()

    @get:PropertyName("emailLowercase")
    @Suppress("unused")
    val emailLowercase: String get() = email.lowercase().removeAccents()

    @get:PropertyName("searchKeywords")
    @Suppress("unused")
    val searchKeywords: List<String> get() {
        val keywords = mutableSetOf<String>()
        val normalizedFirstName = firstName.lowercase().removeAccents()
        val normalizedLastName = lastName.lowercase().removeAccents()
        val normalizedEmail = email.lowercase().removeAccents()

        val allWords = (normalizedFirstName + " " + normalizedLastName + " " + normalizedEmail.split("@").firstOrNull().orEmpty())
            .split(Regex("[\\s.\\-_@]+"))
            .filter { it.isNotBlank() }

        // Individual word prefixes
        for (word in allWords) {
            for (i in 1..word.length) {
                keywords.add(word.take(i))
            }
        }

        // Full name prefixes (FirstName LastName)
        val fullName = ("$normalizedFirstName $normalizedLastName").trim()
        if (fullName.isNotEmpty()) {
            for (i in 1..fullName.length) {
                keywords.add(fullName.take(i))
            }
        }

        // Full name prefixes (LastName FirstName)
        val reversedFullName = ("$normalizedLastName $normalizedFirstName").trim()
        if (reversedFullName.isNotEmpty()) {
            for (i in 1..reversedFullName.length) {
                keywords.add(reversedFullName.take(i))
            }
        }

        return keywords.toList()
    }

    @get:Exclude
    val fullName: String get() = "$firstName $lastName".trim()
}
