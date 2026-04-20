package com.example.free2party.data.model

import com.example.free2party.R
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class DatePattern(val patternResId: Int, val labelResId: Int) {
    YYYY_MM_DD(
        R.string.date_pattern_yyyy_mm_dd,
        R.string.date_label_yyyy_mm_dd
    ),
    MM_DD_YYYY(
        R.string.date_pattern_mm_dd_yyyy,
        R.string.date_label_mm_dd_yyyy
    ),
    DD_MM_YYYY(
        R.string.date_pattern_dd_mm_yyyy,
        R.string.date_label_dd_mm_yyyy
    )
}

enum class ThemeMode(val labelResId: Int) {
    AUTOMATIC(R.string.theme_mode_automatic),
    LIGHT(R.string.theme_mode_light),
    DARK(R.string.theme_mode_dark)
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
    val socials: UserSocials = UserSocials(),
    @get:PropertyName("isFreeNow")
    val isFreeNow: Boolean = false,
    val settings: UserSettings = UserSettings(),
    @ServerTimestamp
    val createdAt: Date? = null
) {
    @get:Exclude
    val fullName: String get() = "$firstName $lastName".trim()
}
