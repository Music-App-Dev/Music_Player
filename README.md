# Music Player App 🎶

An Android-based music player app built using Java, offering users the ability to browse, play, and control their favorite music tracks with a modern UI. This app showcases the usage of Android services, notifications, and media handling capabilities.

## 📜 Table of Contents
- [Features](#features)
- [Screenshots](#screenshots)
- [Installation](#installation)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

---

## 📌 Features

- **Music Library**: Browse all audio files stored on your device.
- **Media Controls**: Play, pause, skip, and repeat tracks with ease.
- **Background Playback**: Listen to music while using other apps.
- **Shuffle & Repeat**: Toggle shuffle mode or loop a single track.
- **Notifications**: Interactive media notification for controlling playback.
- **Album Art**: Displays album artwork for each track.

---

## 📷 Screenshots
![image](https://github.com/user-attachments/assets/247364b1-d944-4ba7-83d3-101e64748c22)
![image](https://github.com/user-attachments/assets/c0848c69-4e6a-4f40-b09a-0023f74c81de)
![image](https://github.com/user-attachments/assets/758e7c5f-54d1-44bb-a687-7f8928e26f90)
![image](https://github.com/user-attachments/assets/82e465a0-3b9c-4ff1-bd54-3d3d082e1dd3)


---

## 🚀 Installation

### Prerequisites
- [Android Studio](https://developer.android.com/studio) installed.
- An Android device or emulator with API level 34 or higher.
- On the emulator, Spotify currently must be installed for the program to function.
- Make sure to use the latest version of Android Studio or else the gradle will not sync properly.

### Steps

1. **Clone the Repository**:
   - Open a terminal and run:
     - `git clone https://github.com/your-username/music-player-app.git`
     - `cd music-player-app`
   - You may also you Github Desktop for an easier time to set it up

2. **Open in Android Studio**:
   - Launch **Android Studio**.
   - Click on **Open an existing project**.
   - Navigate to the `music-player-app` directory and open it.

3. **Build the Project**:
   - Allow Android Studio to download necessary dependencies and build the project.

4. **Run on Device/Emulator**:
   - Connect your Android device or start an emulator.
   - Click the **Run** button or press `Shift + F10` to install and launch the app.

---

## 🎮 Usage

1. Launch the app on your device.
2. Sign in to your Spotify account and grant access to the application
3. Either browse through your liked tracks, or albums/playlists.
4. Tap on a track to start playback.
5. Use media controls to play, pause, skip, or shuffle tracks.
6. Click the + button to add the song to your liked tracks. 
7. Control playback from the notification bar.
8. Click the refresh button to refresh the tracks or albums/playlists to update in real-time.
9. Use the search feature to search for songs, and add them to your liked songs or into your playlists/albums.  

---

## 🗂️ Project Structure

- `app/src/main/java/com/yourpackage/musicplayer/`
  - `activities`: Activity classes
  - `adapters`: RecyclerView Adapters
  - `models`: Data models
  - `services`: Spotify API service
- `app/src/main/res/`: Layouts, drawables, etc.
- `AndroidManifest.xml`
- `build.gradle`

---

## 🛠️ Technologies Used

- **Java**: Core language for app development.
- **Android SDK**: For building the app.
- **Spotify API**: To handle audio playback.
- **Notifications**: For interactive media notifications.
- **RecyclerView**: For displaying lists of music tracks.
- **SharedPreferences**: To save user preferences.

---

## 🤝 Contributing

Contributions are welcome! Follow these steps:

1. **Fork the project**.
2. **Create a new branch**:
   - `git checkout -b feature/YourFeature`
3. **Commit your changes**:
   - `git commit -m 'Add some feature'`
4. **Push to the branch**:
   - `git push origin feature/YourFeature`
5. **Open a pull request**.

---

## 🙏 Acknowledgements

- [Android Developers Documentation](https://developer.android.com/docs)
- Spotify for [API Documentations](https://developer.spotify.com/documentation/web-api)
- Alex
