# JIMMY – Meet The World (Android Random Video Chat App)

## 1. Architecture & Source Code Overview
- **UI & Presentation (`app/src/main/java/com/example/ui/`)**:
  - `screens/SplashScreen.kt`: Animated Jimmy logo with video-chat speech bubble, golden crown, glowing rotating Earth globe, and landmark skyline silhouette.
  - `screens/WelcomeScreen.kt`: Auto-detected **Your Country** picker, **Connect With** destination picker (`All Countries (Random)`, `Thailand`, `Myanmar`, `Japan`, `South Korea`, `United States`, + 30+ countries worldwide with search), **Popular Countries** quick chips, **18+ Age Verification**, **Google Sign-In via Credential Manager**, and non-intrusive **AdMob Banner Slot**.
  - `screens/VideoChatScreen.kt`: Full-screen 50/50 split video layout (Stranger live feed on upper half, User live CameraX preview on lower half), **In-Video Real-Time Text Chat** overlay (floating bubbles + keyboard-safe input bar), **Next** matchmaking button, **Camera Switch**, **Microphone Mute**, and **Report / Block** safety sheet.
  - `video/CameraAndRtcEngine.kt`: Hardware `CameraX` (`PreviewView` + `ImageAnalysis` compact frame encoder), `AudioManager` communication routing, and WebRTC SDP session signaling.
  - `viewmodel/JimmyViewModel.kt`: Reactive state machine managing matchmaking, real-time room & chat listeners, rate limiting, and moderation.
- **Backend & Data Layer (`app/src/main/java/com/example/data/`)**:
  - `model/JimmyModels.kt`: Type-safe Firestore models (`UserProfile`, `CallRoom`, `ChatMessage`, `ModerationAction`) and `CountryCatalog`.
  - `repository/JimmyRepository.kt`: Matchmaking engine, WebRTC signaling updates, real-time chat stream, and safety moderation actions.
  - `remote/FirestoreErrorHandler.kt`: Structured JSON error telemetry (`handleFirestoreError`).

## 2. Firestore Database Structure (`firebase-blueprint.json` & `firestore.rules`)
All operations use the provisioned enterprise database ID (`R.string.firestore_database_id`) and enforce Zero-Trust security rules:
1. `/users/{userId}` (`UserProfile`):
   - Fields: `userId`, `displayName`, `countryCode`, `countryName`, `targetCountryCode`, `onlineStatus` (`online` | `searching` | `in_call` | `offline`), `ageVerified`, `createdAt`, `updatedAt`.
   - Isolation: Strictly readable and writable only by `request.auth.uid == userId`.
2. `/call_rooms/{roomId}` (`CallRoom`):
   - Fields: `hostId`, `hostName`, `hostCountryCode`, `hostCountryName`, `targetCountryCode`, `guestId`, `guestName`, `guestCountryCode`, `guestCountryName`, `status` (`waiting` | `connected` | `ended`), `rtcOfferSdp`, `rtcAnswerSdp`, `hostFrameData`, `guestFrameData`, `hostAudioMuted`, `guestAudioMuted`, `hostCameraFacing`, `guestCameraFacing`, `createdAt`, `updatedAt`.
   - Security Rules:
     - Self-matching is blocked at the database rule level (`existing().hostId != request.auth.uid` on join).
     - Terminal state lock prevents modifying rooms once `status == 'ended'`.
3. `/call_rooms/{roomId}/messages/{messageId}` (`ChatMessage`):
   - Fields: `roomId`, `senderId`, `senderName`, `text`, `participants` (2-element array of `[hostId, guestId]`), `createdAt`.
   - Isolation: Readable/queryable only when `request.auth.uid in resource.data.participants`. Append-only (`allow update, delete: if false`).
4. `/users/{userId}/moderation_actions/{actionId}` (`ModerationAction`):
   - Fields: `userId`, `targetUserId`, `targetCountryCode`, `actionType` (`report` | `block`), `reason`, `roomId`, `createdAt`.

## 3. Firebase Setup Instructions
1. Firebase Auth (Google Sign-In) and Enterprise Firestore are pre-provisioned in `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`.
2. Security rules are defined in `/firestore.rules` and verified against `/firestore.test.js` and `JimmyRepositoryRuleTest.kt`.

## 4. Video SDK & WebRTC / Agora Configuration
Configure custom TURN servers or Agora credentials in the **Secrets panel in AI Studio** (mapped via `.env.example` -> `BuildConfig`):
- `WEBRTC_STUN_URL`: Defaults to `stun:stun.l.google.com:19302`.
- `WEBRTC_TURN_URL`, `WEBRTC_TURN_USERNAME`, `WEBRTC_TURN_CREDENTIAL`: Optional TURN relay credentials for symmetric NAT traversal.
- `AGORA_APP_ID`: Optional Agora RTC Engine App ID if switching the media transport layer to Agora.

## 5. Google AdMob Integration Instructions
- Development builds use Google's official test Ad Unit IDs defined in `.env.example`:
  - `ADMOB_APP_ID=ca-app-pub-3940256099942544~3347511713`
  - `ADMOB_BANNER_AD_UNIT_ID=ca-app-pub-3940256099942544/6300978111`
  - `ADMOB_REWARDED_AD_UNIT_ID=ca-app-pub-3940256099942544/5224354917`
- Ads are restricted to non-call screens (Welcome screen bottom banner) and never interrupt active video calls.
- Before production release, replace the test IDs via the **Secrets panel in AI Studio** with your approved AdMob production IDs.

## 6. Android APK Build & Testing Instructions
- **Run JavaScript Security Rules Tests**:
  `FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js`
- **Run Android Robolectric Repository & Rules Tests**:
  `gradle :app:testDebugUnitTest --tests '*Rule*'`
- **Build Debug APK**:
  `gradle assembleDebug` (or export APK/AAB directly from the AI Studio Settings menu).
