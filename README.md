# GameBot - Gamer's Reward Assistant

GameBot is an Android application designed to help gamers maximize their rewards by automating the tedious process of watching ads. It uses the **Accessibility Service API** to "watch" the screen, detect when an ad's progress is finished, and automatically click the close or claim buttons, allowing gamers to focus on playing rather than dismissing ads.

## Core Purpose

Many mobile games require players to watch the same ads repeatedly to earn rewards (coins, energy, extra lives, etc.). GameBot removes the frustration by:
1. **Automating the Wait**: Detecting the progress bar/timer within the ad.
2. **Auto-Dismissing**: Clicking "Close", "Next", or "Skip" buttons the moment they become available.
3. **Seamless Experience**: Once the player enters the ad, the bot handles the rest, returning them to the game with their reward ready.

## Features

- **Progress Detection**: Automatically detects `ProgressBar` elements (timers) used in common ad networks.
- **Reward Recognition**: Specifically looks for buttons labeled "Claim", "Collect", "Reward", or "Finish" to ensure rewards are secured.
- **Intelligent Waiting**: Waits for timers to hit 100% or for loading spinners to disappear before taking action.
- **Human-like Interaction**: Includes a randomized delay (1-2 seconds) before clicking to simulate natural human behavior and avoid bot detection.
- **Broad Compatibility**: Optimized for major ad activities like `com.facebook.ads.AudienceNetworkActivity` and handles various button types (Text, Icons, and Containers).
- **Persistent Service**: Runs in a separate process (`:bot_process`) to remain active even if the main GameBot app is closed.

## How It Works

The app implements a `GameBotService` which extends `AccessibilityService`. 

1. **Screen Monitoring**: Listens for `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED` events.
2. **Layout Inspection**: Scans the view hierarchy (UI Tree) of the active window.
3. **Logic**:
   - Checks if a `ProgressBar` is visible.
   - If no progress is ongoing, it searches for target clickable elements.
   - Specifically targets Facebook Ad close buttons by identifying clickable containers holding an `ImageView`.
4. **Action**: Performs `ACTION_CLICK` on the identified target after a randomized delay.

## Getting Started

### 1. Build and Run
Clone the project into Android Studio and run it on an Android device (Android 7.0+ recommended).

### 2. Enable the Service
1. Open the **GameBot** app.
2. Click **"1. Enable Bot Service"**.
3. In the System Accessibility settings, find **"Game Bot Assistant"**.
4. Toggle it **ON** and grant the requested permissions.

### 3. Testing
1. Return to the GameBot app.
2. Click **"Start Mock Progress"**.
3. A progress bar will appear. Once finished, the bot will automatically click the resulting "Next" or "Close Ad" button after a short delay.
4. Monitor logs in **Logcat** by filtering for the tag `GameBotService`.

## Important Note on Accessibility Services
This app is for educational/personal automation purposes. Google Play has strict policies regarding the use of Accessibility Services. Ensure your use case complies with the [Android Accessibility API Policy](https://support.google.com/googleplay/android-developer/answer/10964491).
