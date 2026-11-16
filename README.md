# KotliPod

An emulator of the first-generation iPod nano interface, written in Kotlin with Compose Multiplatform.
See [PROPOSAL.md](PROPOSAL.md) for scope and schedule.

## Run (desktop)

```
./gradlew :desktopApp:run
```

Drag around the wheel with the mouse to scroll, click the center to select, and click the top of the
wheel for Menu. Keyboard: arrow up/down scroll, Enter selects, Escape or Backspace goes back.

## Test

```
./gradlew :shared:jvmTest
```

## Layout

- `shared/src/commonMain/.../core`: UI-free logic (click wheel, menu navigation, library model).
- `shared/src/commonMain/.../ui`: Compose `Canvas` rendering of the pod and its wheel.
