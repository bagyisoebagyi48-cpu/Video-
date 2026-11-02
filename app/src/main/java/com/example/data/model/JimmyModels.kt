package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

data class CountryOption(
    val code: String,
    val name: String,
    val flagEmoji: String,
    val region: String,
    val isPopular: Boolean = false
)

object CountryCatalog {
    val ALL_COUNTRIES = CountryOption(
        code = "ALL",
        name = "All Countries (Random)",
        flagEmoji = "🌍",
        region = "Worldwide",
        isPopular = false
    )

    val COUNTRIES: List<CountryOption> = listOf(
        CountryOption("TH", "Thailand", "🇹🇭", "Southeast Asia", isPopular = true),
        CountryOption("MM", "Myanmar", "🇲🇲", "Southeast Asia", isPopular = true),
        CountryOption("JP", "Japan", "🇯🇵", "East Asia", isPopular = true),
        CountryOption("KR", "South Korea", "🇰🇷", "East Asia", isPopular = true),
        CountryOption("US", "United States", "🇺🇸", "North America", isPopular = true),
        CountryOption("VN", "Vietnam", "🇻🇳", "Southeast Asia"),
        CountryOption("SG", "Singapore", "🇸🇬", "Southeast Asia"),
        CountryOption("MY", "Malaysia", "🇲🇾", "Southeast Asia"),
        CountryOption("PH", "Philippines", "🇵🇭", "Southeast Asia"),
        CountryOption("ID", "Indonesia", "🇮🇩", "Southeast Asia"),
        CountryOption("IN", "India", "🇮🇳", "South Asia"),
        CountryOption("TW", "Taiwan", "🇹🇼", "East Asia"),
        CountryOption("HK", "Hong Kong", "🇭🇰", "East Asia"),
        CountryOption("GB", "United Kingdom", "🇬🇧", "Europe"),
        CountryOption("CA", "Canada", "🇨🇦", "North America"),
        CountryOption("AU", "Australia", "🇦🇺", "Oceania"),
        CountryOption("DE", "Germany", "🇩🇪", "Europe"),
        CountryOption("FR", "France", "🇫🇷", "Europe"),
        CountryOption("ES", "Spain", "🇪🇸", "Europe"),
        CountryOption("IT", "Italy", "🇮🇹", "Europe"),
        CountryOption("BR", "Brazil", "🇧🇷", "South America"),
        CountryOption("MX", "Mexico", "🇲🇽", "North America"),
        CountryOption("AE", "United Arab Emirates", "🇦🇪", "Middle East"),
        CountryOption("TR", "Turkey", "🇹🇷", "Middle East / Europe"),
        CountryOption("NL", "Netherlands", "🇳🇱", "Europe"),
        CountryOption("SE", "Sweden", "🇸🇪", "Europe"),
        CountryOption("NO", "Norway", "🇳🇴", "Europe"),
        CountryOption("CH", "Switzerland", "🇨🇭", "Europe"),
        CountryOption("PL", "Poland", "🇵🇱", "Europe"),
        CountryOption("AR", "Argentina", "🇦🇷", "South America"),
        CountryOption("CO", "Colombia", "🇨🇴", "South America"),
        CountryOption("CL", "Chile", "🇨🇱", "South America"),
        CountryOption("ZA", "South Africa", "🇿🇦", "Africa"),
        CountryOption("NG", "Nigeria", "🇳🇬", "Africa"),
        CountryOption("EG", "Egypt", "🇪🇬", "Africa"),
        CountryOption("NZ", "New Zealand", "🇳🇿", "Oceania")
    )

    val POPULAR_COUNTRIES: List<CountryOption> = COUNTRIES.filter { it.isPopular }

    val CONNECT_WITH_OPTIONS: List<CountryOption> = listOf(ALL_COUNTRIES) + COUNTRIES

    fun findByCode(code: String): CountryOption {
        if (code.equals("ALL", ignoreCase = true)) return ALL_COUNTRIES
        return COUNTRIES.find { it.code.equals(code, ignoreCase = true) }
            ?: COUNTRIES.first { it.code == "MM" }
    }

    fun detectDeviceCountry(): CountryOption {
        val localeCountry = Locale.getDefault().country.uppercase(Locale.ROOT)
        return COUNTRIES.find { it.code == localeCountry }
            ?: COUNTRIES.first { it.code == "MM" }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val countryCode: String = "MM",
    val countryName: String = "Myanmar",
    val targetCountryCode: String = "ALL",
    val onlineStatus: String = "online",
    val ageVerified: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CallRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostCountryCode: String = "MM",
    val hostCountryName: String = "Myanmar",
    val targetCountryCode: String = "ALL",
    val guestId: String = "",
    val guestName: String = "",
    val guestCountryCode: String = "",
    val guestCountryName: String = "",
    val status: String = "waiting",
    val rtcOfferSdp: String = "",
    val rtcAnswerSdp: String = "",
    val hostFrameData: String = "",
    val guestFrameData: String = "",
    val hostAudioMuted: Boolean = false,
    val guestAudioMuted: Boolean = false,
    val hostCameraFacing: String = "front",
    val guestCameraFacing: String = "front",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isHost(uid: String): Boolean = hostId == uid

    fun partnerUid(myUid: String): String = if (hostId == myUid) guestId else hostId

    fun partnerName(myUid: String): String =
        if (hostId == myUid) guestName.ifBlank { "Stranger" } else hostName.ifBlank { "Stranger" }

    fun partnerCountryCode(myUid: String): String =
        if (hostId == myUid) guestCountryCode.ifBlank { "ALL" } else hostCountryCode.ifBlank { "ALL" }

    fun partnerCountryName(myUid: String): String =
        if (hostId == myUid) guestCountryName.ifBlank { "Global" } else hostCountryName.ifBlank { "Global" }

    fun partnerFrameData(myUid: String): String =
        if (hostId == myUid) guestFrameData else hostFrameData

    fun partnerAudioMuted(myUid: String): Boolean =
        if (hostId == myUid) guestAudioMuted else hostAudioMuted

    fun partnerCameraFacing(myUid: String): String =
        if (hostId == myUid) guestCameraFacing else hostCameraFacing
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participants: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ModerationAction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val targetCountryCode: String = "ALL",
    val actionType: String = "report",
    val reason: String = "",
    val roomId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
