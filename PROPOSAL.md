# KotliPod: An iPod Nano Emulator in Kotlin

## Project description

KotliPod is intended to be a software emulation of the user interface of the first-generation iPod nano, featuring the 176×132 color display, the menu hierarchy, the Now Playing screen, and the click wheel with its center button and Menu, Play/Pause, and skip buttons. It will be written in Kotlin on the Compose Multiplatform system suggested by Paul, and primary platforms are the mobile and desktop.

Yes I know I like iPods too much.

How does this project apply to DSA? Well, the click wheel is the first problem: a pointer or finger position on a circular touch surface must become a stream of discrete scroll detents. That would probably require angle computation (`atan2`), wraparound-safe delta accumulation, and velocity-based acceleration so that fast circular motion skips through long lists. Navigation is the second: the menu system is a tree traversed with an explicit stack, with per-screen scroll state saved and restored on back-navigation. The music library is the third: songs must be grouped and ordered by Artist, Album, Genre. There are other things I would like to add, but I have not fully thought about yet in terms of architecture or features, but I would like to also add playlists, shuffling, et. al.

The interface logic lives in a UI-free core module in `shared/commonMain`, with no Compose dependency. It takes input events (wheel delta, button presses) and produces a screen model. A thin Compose `Canvas` layer draws that model at 176×132 and scales it by an integer factor inside an iPod body skin. This split lets the core be unit tested and benchmarked on its own, and it lets the same code run on the Android and iOS targets. Benchmarks will compare my merge sort against the standard library sort on realistic library sizes, and my shuffling against a real iPod and another project I have, ClassicTunes, to connect the work back to runtime analysis. It will also be compared with a real 1st gen iPod Nano I have to make everything implemented behaves realistically.

## Team

This is a solo project.

## Learning goals

- Apply sorting, hashing, trees, stacks, and shuffling to a real interactive system, and see where the standard versions need adjusting,
- Design an input pipeline that turns continuous analog-style input into discrete events, including numerical edge cases such as angle wraparound.
- Practice a strict separation between pure logic and rendering in Kotlin Multiplatform (some previous projects stare back at me in disappointment).
- Write benchmarks and report what they show about asymptotic versus real performance and day-to-day usage.

## Deliverables

| Deliverable | C | B | A |
|---|---|---|---|
| 1. Navigation engine | Menu tree and back-stack for the main menu, Music, and Settings. Select and Menu buttons work. | Full menu tree for every top-level item, with scroll position restored on back. | B, plus transition animations (slide left/right) and a text marquee for overlong titles. |
| 2. Click wheel model | Angle to detent conversion with wraparound handled; works with mouse drag. | B-level C, plus velocity-based acceleration and press zones (Menu, Play/Pause, skip, center). | Tuned feel against reference videos, plus keyboard and touch support, and unit tests for edge cases (crossing 0°/360°, reversal mid-stroke). |
| 3. Library, sort, and shuffle | Sample library of about 200 songs; browse by Artist, Album, Songs using the standard library sort. | Own stable multi-key merge sort; grouping by hash maps; Genres and Playlists; Fisher–Yates shuffle and Shuffle Songs. | B, plus benchmarks against `sortedWith` and shuffle-by-sort across library sizes, with a short write-up of results. |
| 4. Now Playing and playback | Now Playing screen with progress bar, play/pause, next/previous, and real WAV playback on the desktop target through JavaSound. | MP3 playback through a decoder library (such as JLayer) in addition to WAV; repeat and shuffle state machine; previous restarts the track after a few seconds, as on the original. | Library loaded from a music folder with ID3 tags read into the metadata model, plus seeking by turning the wheel on the progress bar. |
| 5. Extras and Photos | Clock and Stopwatch. | Calendar (month grid via a day-of-week algorithm), Notes viewer, Photos with box-filter thumbnails. | Contacts, Screen Lock, bilinear scaling comparison, and one game (Brick). |
| 6. Look and feel | Flat-colored 176×132 screen with the classic menu list layout. | Faithful typography, selection highlight, title bar with battery and play icons, and the iPod body with wheel. | Backlight and contrast settings that change rendering, and the Android target running the same core. |
| 7. Documentation and tests | READMEs with build and run steps; core unit tests. | README plus architecture overview and annotated tour of the algorithms. | B, plus a classmate-oriented walkthrough: "add a new menu screen in 10 minutes" tutorial. |

The other-deliverables requirement is met by the repository (code and documentation), a short summary of what was built, and the walkthrough in deliverable 7. The five-minute presentation will be a live demo of the emulator plus a benchmark.

## Schedule

**By 11/19**

- Core module set up in `shared/commonMain` with unit-test tooling working.
- Navigation engine: menu tree, back-stack, Select and Menu handling.
- Click wheel model with angle accumulation and wraparound.
- Compose Canvas screen showing the real main menu and a scrollable song list from a sample library, driven by mouse on the wheel.
- Audio: WAV playback through JavaSound(?) behind a playback interface in the core (play, pause, position), tested with sample files. Now Playing screen wired to it.

**By 12/7**

- Deliverables 1, 2, 3, 4, and 5 at B: full menu tree, wheel acceleration and press zones, own merge sort with grouping, Genres and Playlists, shuffle, repeat/shuffle state machine, real MP3 and WAV playback, Clock, Stopwatch, Calendar, Photos with thumbnails.
- Benchmarks for sort and shuffle with a short written analysis.
- Documentation, READMEs, etc.
- Library loaded from a music folder with ID3 tags.
- Brick/Breakout game, Contacts, Notes editing.

**By eventually, maybe**

- Other iPod games (song guessing, that one game where you shoot down targets parachuting from planes????)
- Android target running the shared core.
