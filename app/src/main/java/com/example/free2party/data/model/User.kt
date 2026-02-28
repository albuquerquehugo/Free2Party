package com.example.free2party.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class DatePattern(val pattern: String, val label: String) {
    YYYY_MM_DD("yyyy-MM-dd", "YYYY/MM/DD"),
    MM_DD_YYYY("MM-dd-yyyy", "MM/DD/YYYY"),
    DD_MM_YYYY("dd-MM-yyyy", "DD/MM/YYYY")
}

enum class ThemeMode(val label: String) {
    AUTOMATIC("Automatic"),
    LIGHT("Light"),
    DARK("Dark")
}

data class UserSettings(
    val use24HourFormat: Boolean = true,
    val datePattern: DatePattern = DatePattern.YYYY_MM_DD,
    val themeMode: ThemeMode = ThemeMode.AUTOMATIC
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
    val fullName: String get() = "$firstName $lastName".trim()
}
