# Desktop releases (Windows / Linux)

`.github/workflows/release-desktop.yml` builds the game with JDK 17 and the Arc commit
pinned in `gradle.properties`, then runs `.github/scripts/package-desktop.sh` to produce
two ready-to-run archives and publishes them as a GitHub Release.

## Trigger a new release

```sh
gh workflow run release-desktop.yml --ref arena/3268cd66-mindustry \
  -f tag=v8-arena-2 -f build=arena-2 -f bundle_jre=true -f publish=true
```

Alternatively, push a commit to `arena/3268cd66-mindustry` with `[release]` in its message.
If the tag already exists, its assets are replaced (`gh release upload --clobber`);
otherwise a new release is created at the triggering commit. Set `publish=false` to only
upload the ZIPs as workflow artifacts.

| Input | Meaning |
| --- | --- |
| `tag` | release tag, e.g. `v8-arena-2` |
| `build` | in-game build string written to `version.properties` |
| `bundle_jre` | bundle a Java 17 runtime into each ZIP (Linux: `jlink`, Windows: Temurin JRE) |
| `publish` | create/update the GitHub Release |

## Release assets

- `Mindustry-Windows-x64.zip` — `Mindustry.jar`, `Mindustry.bat`, bundled Windows JRE
- `Mindustry-Linux-x64.zip` — `Mindustry.jar`, `mindustry.sh`, bundled Linux runtime
- `Mindustry.jar` — universal cross-platform JAR (requires Java 17+ installed)
- `SHA256SUMS.txt` — checksums

The JAR is platform-independent; the ZIPs differ only by the launcher script and the
bundled runtime. The workflow needs `contents: write` for publishing. These are builds of
this fork, not official `Anuken/Mindustry` releases.

Для запуска: на Windows — `Mindustry.bat`, на Linux — `./mindustry.sh`.
