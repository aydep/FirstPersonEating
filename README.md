# First Person Eating

A small client-side NeoForge 1.21.1 mod that bridges **Shoulder Surfing Reloaded** and **Kaleidoscope: ImmersiveEating**.

Immersive Eating's eating animations only work in first person, and refuse to start in any other view. With this mod you can stay in over-the-shoulder view all game: when you start eating a configured food the camera switches to first person, and when you stop it returns to the view you had before.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.x
- [Shoulder Surfing Reloaded](https://modrinth.com/mod/shoulder-surfing-reloaded) 5.2.0
- Kaleidoscope: ImmersiveEating 1.5.2 (mod id `food`)

The mod does nothing if either of the two is missing. Client-side only; not needed on servers.

## Config

`config/firstpersoneating-client.toml`

```toml
# Item id or 'namespace:*'
items = ["kaleidoscopecookery:*"]
```

Only items that Immersive Eating itself animates are affected; this list narrows that further.

## Building

Put the two required jars (names as in `build.gradle`) into a `libs/` folder, then:

```
./gradlew build
```

The jar is written to `build/libs/`.

## Known limits

- No smooth camera transition yet; the switch is instant.
- Trigger conditions mirror Immersive Eating 1.5.2's internals (use key held, no screen open, not aiming at a block) and may need updating for newer versions.

## License

MIT
