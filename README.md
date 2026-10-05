# Sound Muter

Mute or turn down single sounds instead of a whole volume category.

Got a trading hall full of villagers going "hmm" all day? A chicken farm under your base? Mute just those and keep every other sound the way it was.

It's client-side only, so it works on any server, vanilla ones included. It only changes what you hear and doesn't add or reveal anything the server didn't send you.

![Searching for chicken sounds, muting one and turning it down](https://raw.githubusercontent.com/SpeedyCoder1192/sound-muter/main/docs/demo.gif)

## How it works

Open the menu with a keybind (or from Mod Menu on Fabric, or the Mods list on NeoForge). You get:

- **Recently heard**: the last 20 sounds that played. Easiest way to find "that noise".
- **Muted / changed**: everything you've already touched.
- **All sounds**: every sound in the game, including ones from other mods and resource packs, grouped by type (ambient, block, entity, music...).

Each sound has a Mute button, a volume slider (0-100%) and a ▶ button to hear it. The search box matches both the subtitle text ("Villager mumbles") and the sound id (`entity.villager.ambient`).

![The Sound Muter menu](https://raw.githubusercontent.com/SpeedyCoder1192/sound-muter/main/docs/screen.png)

## Mute last sound

There's also a "Mute last sound" key. Hear something annoying, press it, done. A small popup tells you what got muted, and pressing the key again within 5 seconds undoes it.

![Popup after pressing the mute key](https://raw.githubusercontent.com/SpeedyCoder1192/sound-muter/main/docs/mute-last.png)

Both keys are unbound by default. Set them in Options > Controls > Key Binds under "Sound Muter".

## Good to know

- Settings are saved in `config/soundmuter.json`.
- A muted sound also loses its subtitle, so think twice before muting something like creeper hissing.
- Menu clicks and other UI sounds don't show up in "Recently heard", otherwise they'd fill the list every time you open it. You can still find them under "All sounds".

## Requirements

- Fabric: [Fabric API](https://modrinth.com/mod/fabric-api). [Mod Menu](https://modrinth.com/mod/modmenu) is optional.
- NeoForge: nothing extra.

Works on 1.20.1, 1.21.1, 1.21.9-1.21.11 and 26.1-26.3 (no NeoForge build for 1.20.1).

Bugs or ideas? [Open an issue](https://github.com/SpeedyCoder1192/sound-muter/issues).

## Building

Needs Java 25 to run Gradle (older Java versions for the older Minecraft builds get downloaded automatically).

```
./gradlew buildAll
```

The jars end up in `build/libs/`. One codebase covers every version through [Stonecutter](https://stonecutter.kikugie.dev/);
version-specific settings are in `versions/<minecraft>-<loader>/gradle.properties`.
To run one version: `./gradlew :1.21.1-fabric:runClient` (or any other folder name in `versions/`).
