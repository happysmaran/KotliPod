# KotliPod

An emulator of the first-generation iPod nano interface, written in Kotlin with Compose Multiplatform.
See [PROPOSAL.md](PROPOSAL.md) for scope and schedule.

## Run (desktop)

```
./gradlew :desktopApp:run
```

Drag around the wheel with the mouse to scroll, click the center to select, and click the top of the
wheel for Menu. Keyboard: arrow up/down scroll, Enter selects, Escape or Backspace goes back.

## Music library and playback

The library starts empty. Choose **Settings → Select Music Folder** (first row) to pick a folder; it is scanned recursively for `.mp3`, `.m4a`, `.wav` and `.aif/.aiff` files, and the songs appear under **Music**. Selecting a song plays it; Play/Pause (Space) toggles playback.

Music files are only ever read. Decoding happens in memory, using the first decoder that works: Java Sound (with the MP3/AAC plugins), macOS `afconvert` (writes to a temporary directory that is deleted afterwards), then `ffmpeg` (streams raw PCM over a pipe).

### Android and iOS

Android plays through `MediaPlayer` and reads the folder granted by the system folder picker (a document-tree URI). iOS plays through `AVAudioPlayer`, reads the folder returned by the document picker, and reads tags with AVFoundation, which has no genre. Both read the music in place and never write to it. Run them with the `androidApp` run configuration and by opening `iosApp/iosApp.xcodeproj` in Xcode. Playback stops when the app is closed or sent to the background; there is no background-audio service yet.

## Test

```
./gradlew :shared:jvmTest
```

## Layout

- `shared/src/commonMain/.../core`: UI-free logic (click wheel, menu navigation, library model).
- `shared/src/commonMain/.../ui`: Compose `Canvas` rendering of the pod and its wheel.
