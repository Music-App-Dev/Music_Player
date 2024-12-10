# Music Player App 🎶

An Android-based music player app built using Java, offering users the ability to browse, play, and control their favorite music tracks with a modern UI. This app showcases the usage of Android services, notifications, and media handling capabilities.

## 📜 Table of Contents
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Usage](#-usage)
- [Project Structure](#-project-structure)
- [Contributing](#-contributing)
  
---

## 📌 Features

- **Music Library**: Browse all audio files from the Spotify API.
- **Media Controls**: Play, pause, skip, shuffle, and repeat tracks with ease.
- **Background Playback**: Listen to music while using other apps.
- **Queue and Recently Played**: Add songs to a queue and also view your recently played songs.
- **Album Art**: Displays album artwork for each track.

---

## 📷 Screenshots
![image](https://github.com/user-attachments/assets/f14f46aa-20d4-4cf2-b1c3-b9ad131a2ad6)
![image](https://github.com/user-attachments/assets/a9dc0c8c-0701-49d2-a015-84dce312d529)
![image](https://github.com/user-attachments/assets/f3d11ff7-2437-4bbb-9f73-ec95a5ec3541)
![image](https://github.com/user-attachments/assets/c88b94a6-2b15-4788-935f-3c0ecca10b2c)
![image](https://github.com/user-attachments/assets/b66d77e5-4fbf-475e-b130-dc65e7d60722)
![image](https://github.com/user-attachments/assets/ca5ab0be-b3b3-45b6-85d2-f855863ae3c0)

---

## 🚀 Installation

### Prerequisites
- [Android Studio](https://developer.android.com/studio) installed.
- An Android device or emulator with API level 34 or higher.
- On the emulator, Spotify currently must be installed for the program to function.
- User must have Spotify Premium in order for the playback to function correctly

### Steps

1. **Clone the Repository**:
   - Open a terminal and run:
     - `git clone https://github.com/your-username/music-player-app.git`
     - `cd music-player-app`
   - You may also you Github Desktop for an easier set up by cloning

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
5. Use media controls to play, pause, skip, repeat, or shuffle tracks.
6. Click the + button to add the song to your liked tracks. 
7. Click the refresh button to refresh the tracks or albums/playlists to update in real-time.
8. Use the search feature to search for songs, and add them to your liked songs or into your playlists/albums.  

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
