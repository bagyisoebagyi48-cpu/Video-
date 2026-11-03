package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.model.CountryCatalog
import com.example.data.model.CountryOption
import com.example.ui.auth.GuestSessionManager
import com.example.ui.components.AdMobBannerSlot
import com.example.ui.components.GlowingGlobeVisual
import com.example.ui.components.JimmyLogoHeader
import com.example.ui.components.WorldSkylineSilhouette
import com.example.ui.theme.JimmyBlueAccent
import com.example.ui.theme.JimmyGoldCrown
import com.example.ui.theme.JimmyNavyBorder
import com.example.ui.theme.JimmyNavyCard
import com.example.ui.theme.JimmyNavyDarkest
import com.example.ui.theme.JimmyNavyDeep
import com.example.ui.theme.JimmyNavySurface
import com.example.ui.theme.JimmyOnlineGreen
import com.example.ui.theme.JimmyOrangeBright
import com.example.ui.theme.JimmyOrangeDeep
import com.example.ui.theme.JimmyOrangeLight
import com.example.ui.theme.JimmyTextMuted
import com.example.ui.theme.JimmyTextPrimary
import com.example.ui.theme.JimmyTextSecondary

private enum class CountryPickerTarget {
    NONE,
    YOUR_COUNTRY,
    CONNECT_WITH
}

private enum class LegalSheetType {
    NONE,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY,
    SETUP_GUIDE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    isAuthenticated: Boolean,
    userDisplayName: String,
    yourCountry: CountryOption,
    connectWithCountry: CountryOption,
    ageVerified18Plus: Boolean,
    toastMessage: String?,
    onSelectYourCountry: (CountryOption) -> Unit,
    onSelectConnectWith: (CountryOption) -> Unit,
    onToggleAgeVerified: (Boolean) -> Unit,
    onStartVideoChat: () -> Unit,
    onAuthSuccess: () -> Unit,
    onSignOut: () -> Unit,
    onClearToast: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pickerTarget by remember { mutableStateOf(CountryPickerTarget.NONE) }
    var legalSheet by remember { mutableStateOf(LegalSheetType.NONE) }
    var countrySearchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(toastMessage) {
        if (!toastMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMessage)
            onClearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = JimmyNavyDarkest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JimmyNavyDarkest,
                            JimmyNavyDeep,
                            Color(0xFF0C153A),
                            JimmyNavyDarkest
                        )
                    )
                )
                .testTag("welcome_screen")
        ) {
            WorldSkylineSilhouette(
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 520.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header: Jimmy Logo + "Meet People Around The World ❤️"
                    JimmyLogoHeader(
                        showVideoBubbleIcon = false,
                        subtitle = stringResource(R.string.subtitle_meet_people)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Center: Glowing Earth Globe with Orange Orbital Rings & Global Avatars
                    GlowingGlobeVisual(
                        globeSize = 210.dp,
                        showOrbitingCallers = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card A: Your Country
                    CountrySelectionCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Your Country",
                                tint = JimmyOrangeBright,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = stringResource(R.string.your_country_label),
                        selectedCountry = yourCountry,
                        onClick = {
                            countrySearchQuery = ""
                            pickerTarget = CountryPickerTarget.YOUR_COUNTRY
                        },
                        testTag = "your_country_selector"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Card B: Connect With
                    CountrySelectionCard(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = "Connect With",
                                tint = JimmyOrangeLight,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = stringResource(R.string.connect_with_label),
                        selectedCountry = connectWithCountry,
                        onClick = {
                            countrySearchQuery = ""
                            pickerTarget = CountryPickerTarget.CONNECT_WITH
                        },
                        testTag = "connect_with_selector"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Popular Countries Quick Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Popular Countries",
                                tint = JimmyOrangeBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.popular_countries_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = JimmyTextPrimary
                            )
                        }
                        if (connectWithCountry.code != "ALL") {
                            Text(
                                text = "Reset to All",
                                style = MaterialTheme.typography.labelMedium,
                                color = JimmyOrangeLight,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectConnectWith(CountryCatalog.ALL_COUNTRIES) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("reset_all_countries_button")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(
                            items = CountryCatalog.POPULAR_COUNTRIES,
                            key = { it.code }
                        ) { country ->
                            val isSelected = connectWithCountry.code == country.code
                            PopularCountryChip(
                                country = country,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        onSelectConnectWith(CountryCatalog.ALL_COUNTRIES)
                                    } else {
                                        onSelectConnectWith(country)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary CTA: Start Video Chat Button (Zero sign-in barrier)
                    Button(
                        onClick = onStartVideoChat,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(JimmyOrangeLight, JimmyOrangeBright, JimmyOrangeDeep)
                                )
                            )
                            .testTag("start_video_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.start_video_chat),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 18+ Age Verification Notice
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onToggleAgeVerified(!ageVerified18Plus) }
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("age_verification_toggle")
                    ) {
                        Icon(
                            imageVector = if (ageVerified18Plus) {
                                Icons.Default.VerifiedUser
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = "18+ Age Verification",
                            tint = if (ageVerified18Plus) JimmyGoldCrown else JimmyTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.age_restriction_notice),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ageVerified18Plus) JimmyTextSecondary else JimmyOrangeLight
                        )
                    }

                    // Non-Call Screen AdMob Banner
                    Spacer(modifier = Modifier.height(8.dp))
                    AdMobBannerSlot(
                        modifier = Modifier.testTag("welcome_admob_banner")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Legal Links & Session Bar (Terms, Privacy, Setup Guide, Sign Out if signed in)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { legalSheet = LegalSheetType.TERMS_OF_SERVICE },
                            modifier = Modifier.testTag("terms_of_service_button")
                        ) {
                            Text(
                                text = stringResource(R.string.terms_of_service),
                                style = MaterialTheme.typography.labelMedium,
                                color = JimmyTextMuted
                            )
                        }
                        Text(text = "•", color = JimmyTextMuted, fontSize = 12.sp)
                        TextButton(
                            onClick = { legalSheet = LegalSheetType.PRIVACY_POLICY },
                            modifier = Modifier.testTag("privacy_policy_button")
                        ) {
                            Text(
                                text = stringResource(R.string.privacy_policy),
                                style = MaterialTheme.typography.labelMedium,
                                color = JimmyTextMuted
                            )
                        }
                        Text(text = "•", color = JimmyTextMuted, fontSize = 12.sp)
                        TextButton(
                            onClick = { legalSheet = LegalSheetType.SETUP_GUIDE },
                            modifier = Modifier.testTag("setup_guide_button")
                        ) {
                            Text(
                                text = "SDK & Docs",
                                style = MaterialTheme.typography.labelMedium,
                                color = JimmyTextMuted
                            )
                        }
                        if (userDisplayName.isNotBlank()) {
                            Text(text = "•", color = JimmyTextMuted, fontSize = 12.sp)
                            TextButton(
                                onClick = {
                                    GuestSessionManager.resetSession(context)
                                    onSignOut()
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Text(
                                    text = "Reset ID ($userDisplayName)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = JimmyTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Searchable Country Picker Modal Sheet
    if (pickerTarget != CountryPickerTarget.NONE) {
        val isConnectWith = pickerTarget == CountryPickerTarget.CONNECT_WITH
        val options = remember(isConnectWith, countrySearchQuery) {
            val baseList = if (isConnectWith) {
                CountryCatalog.CONNECT_WITH_OPTIONS
            } else {
                CountryCatalog.COUNTRIES
            }
            if (countrySearchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter {
                    it.name.contains(countrySearchQuery, ignoreCase = true) ||
                        it.code.contains(countrySearchQuery, ignoreCase = true) ||
                        it.region.contains(countrySearchQuery, ignoreCase = true)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { pickerTarget = CountryPickerTarget.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = JimmyNavySurface,
            contentColor = JimmyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isConnectWith) "Connect With Country" else "Select Your Country",
                    style = MaterialTheme.typography.headlineMedium,
                    color = JimmyTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = countrySearchQuery,
                    onValueChange = { countrySearchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_country_hint),
                            color = JimmyTextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Country",
                            tint = JimmyOrangeLight
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JimmyOrangeBright,
                        unfocusedBorderColor = JimmyNavyBorder,
                        focusedContainerColor = JimmyNavyDeep,
                        unfocusedContainerColor = JimmyNavyDeep,
                        focusedTextColor = JimmyTextPrimary,
                        unfocusedTextColor = JimmyTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("country_search_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items = options, key = { it.code }) { option ->
                        val isSelected = if (isConnectWith) {
                            connectWithCountry.code == option.code
                        } else {
                            yourCountry.code == option.code
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) JimmyOrangeBright.copy(alpha = 0.18f)
                                    else JimmyNavyDeep
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) JimmyOrangeBright else JimmyNavyBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (isConnectWith) {
                                        onSelectConnectWith(option)
                                    } else {
                                        onSelectYourCountry(option)
                                    }
                                    pickerTarget = CountryPickerTarget.NONE
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("country_option_${option.code}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = option.flagEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = option.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = JimmyTextPrimary
                                    )
                                    Text(
                                        text = option.region,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = JimmyTextMuted
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = JimmyOrangeBright
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Legal & Architecture Documentation Modal Sheet
    if (legalSheet != LegalSheetType.NONE) {
        ModalBottomSheet(
            onDismissRequest = { legalSheet = LegalSheetType.NONE },
            containerColor = JimmyNavySurface,
            contentColor = JimmyTextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                when (legalSheet) {
                    LegalSheetType.TERMS_OF_SERVICE -> {
                        Text(
                            text = "JIMMY – Terms of Service (18+)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = JimmyOrangeLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. Age Requirement (18+): You must be at least 18 years old to access JIMMY Random Video Chat.\n\n" +
                                "2. Community Conduct: Harassment, nudity, hate speech, violence, or illegal behavior is strictly prohibited. Violators are immediately disconnected and blocked.\n\n" +
                                "3. Instant Moderation: Tapping Report or Block during any call immediately terminates the connection and logs the incident for safety enforcement.\n\n" +
                                "4. Ephemeral Calls: Video streams are live peer-to-peer / real-time sessions. Respect the privacy of everyone you meet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JimmyTextSecondary
                        )
                    }
                    LegalSheetType.PRIVACY_POLICY -> {
                        Text(
                            text = "JIMMY – Privacy Policy",
                            style = MaterialTheme.typography.headlineMedium,
                            color = JimmyOrangeLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "1. Minimal Data Collection: JIMMY stores only local matchmaking state (anonymous session ID, country selection, online status, and active call room ID) on your device.\n\n" +
                                "2. Camera & Microphone Privacy: Hardware camera and microphone access are used exclusively while you are actively inside the Video Chat Screen and released immediately when you leave or background the app.\n\n" +
                                "3. Privacy First: No personal accounts, passwords, or emails are collected.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JimmyTextSecondary
                        )
                    }
                    LegalSheetType.SETUP_GUIDE -> {
                        Text(
                            text = "SDK, Backend & Build Guide",
                            style = MaterialTheme.typography.headlineMedium,
                            color = JimmyOrangeLight
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• Standalone Architecture: Powered by a clean, lightweight matchmaking engine and WebRTC SDP signaling without any external Firebase services.\n\n" +
                                "• Real-Time Video & Audio: Powered by CameraX hardware preview/frame analyzer + WebRTC SDP signaling. Configure custom TURN servers or Agora App ID via `.env` / AI Studio Secrets (`WEBRTC_TURN_URL`, `AGORA_APP_ID`).\n\n" +
                                "• AdMob Monetization: Configured with official Google Test Ad Unit IDs (`ADMOB_BANNER_AD_UNIT_ID`, `ADMOB_REWARDED_AD_UNIT_ID`) on non-call screens.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = JimmyTextSecondary
                        )
                    }
                    LegalSheetType.NONE -> Unit
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CountrySelectionCard(
    icon: @Composable () -> Unit,
    label: String,
    selectedCountry: CountryOption,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(JimmyNavySurface.copy(alpha = 0.88f))
            .border(1.dp, JimmyNavyBorder, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = JimmyTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(JimmyNavyCard)
                .border(1.dp, JimmyNavyBorder.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(
                            if (selectedCountry.code == "ALL") CircleShape
                            else RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (selectedCountry.code == "ALL") JimmyBlueAccent
                            else JimmyNavyDeep
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = selectedCountry.flagEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = selectedCountry.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = JimmyTextPrimary
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Change Country",
                tint = JimmyTextSecondary
            )
        }
    }
}

@Composable
private fun PopularCountryChip(
    country: CountryOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shortName = if (country.code == "KR") "S. Korea" else country.name
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) JimmyOrangeBright.copy(alpha = 0.22f)
                else JimmyNavySurface.copy(alpha = 0.9f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) JimmyOrangeBright else JimmyNavyBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag("popular_country_${country.code}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(JimmyNavyCard),
            contentAlignment = Alignment.Center
        ) {
            Text(text = country.flagEmoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = shortName,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) JimmyOrangeLight else JimmyTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}
