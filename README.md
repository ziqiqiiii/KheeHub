# KheeHub
Android Mobile App - Public Toilet Finder 

## Setup Instructions

Follow these steps to get the project running on your local machine.

### 1. Prerequisites
- **Android Studio** (Hedgehog or newer recommended)
- **Android SDK** (API Level 34 or higher)
- **JDK 11**

### 2. Clone the Repository
```bash
git clone https://github.com/your-username/KheeHub.git
```

### 3. SDK and API Key Configuration (`local.properties`)
Create a file named `local.properties` in the root directory of the project. This file is ignored by Git and is used to store local configuration and secrets.

1.  **Set the Android SDK path:**
    If Android Studio doesn't detect it automatically, add the `sdk.dir` property:
    ```properties
    sdk.dir=/path/to/your/android/sdk
    ```
    *(Note: On Windows, use double backslashes, e.g., `sdk.dir=C\:\\Users\\Name\\AppData\\Local\\Android\\Sdk`)*

2.  **Add the Google Maps API Key:**
    - Go to the [Google Cloud Console](https://console.cloud.google.com/).
    - Create a new project or select an existing one.
    - Enable the **Maps SDK for Android**.
    - Go to **Credentials** and create an **API Key**.
    - Add it to `local.properties`:
      ```properties
      MAPS_API_KEY=YOUR_API_KEY
      ```

### 4. Firebase Setup
The app uses Firebase for data storage (Firestore) and analytics.

1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Create a new Firebase project.
3.  Add an Android app to the project using the package name `com.example.kheehub`.
4.  Download the `google-services.json` file.
5.  Place the `google-services.json` file into the `app/` directory of the project.
6.  (Optional) Enable **Firestore Database** in the Firebase console if you want to use your own database.

### 5. Build and Run
1.  Open the project in Android Studio.
2.  Wait for the Gradle sync to complete.
3.  Select an emulator or connect a physical device.
4.  Click the **Run** button (green play icon).

## Features
- Find public toilets near your location.
- View toilet details and ratings.
- Integrated Google Maps for navigation.
- Real-time data sync with Firebase Firestore.
