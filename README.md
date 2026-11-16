# Material Down

Android gesture-navigation companion with a compact Material 3-inspired horizontal pill.

## Gestures
- Swipe up on the pill: Home.
- Swipe up and hold before releasing: Recents.
- Back: use Android's native edge-back gesture. Enable Gesture navigation in Android system settings.

## Setup
1. Install the app.
2. Open it and tap Enable pill.
3. In Android Accessibility settings, enable Material Down navigation gestures.
4. If Android is configured for three-button navigation, switch to Gesture navigation in system settings to use edge-swipe Back.

Android requires an explicitly enabled Accessibility Service for an app to show an accessibility overlay and invoke global navigation actions. The service does not read screen content.

## Build
Open the project in Android Studio or run gradle :app:assembleDebug with JDK 17 and Gradle 8.9.
