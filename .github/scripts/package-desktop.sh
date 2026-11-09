#!/usr/bin/env bash
# Packages the Gradle-built desktop JAR into ready-to-run Windows and Linux ZIPs.
# Run from any directory; expects `desktop/build/libs/Mindustry.jar` to exist.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$root"

jar="desktop/build/libs/Mindustry.jar"
if [ ! -s "$jar" ]; then
    echo "Missing desktop JAR: $jar" >&2
    exit 1
fi

build_version="${BUILD_VERSION:-custom}"
commit="${GITHUB_SHA:-$(git rev-parse HEAD)}"
subject="$(git log -1 --format=%s "$commit" 2>/dev/null || true)"
subject="${subject//\[release\]/}"
bundle="${BUNDLE_JRE:-true}"
repo="${GITHUB_REPOSITORY:-egefefefefef/Mindustry}"

out="$root/build"
stage="$out/release-stage"
rm -rf "$stage" "$out/Mindustry-Windows-x64.zip" "$out/Mindustry-Linux-x64.zip" \
      "$out/SHA256SUMS.txt" "$out/release-notes.md"
mkdir -p "$stage/windows" "$stage/linux"

for platform in windows linux; do
    cp "$jar" "$stage/$platform/Mindustry.jar"
done

# ---------------------------------------------------------------- launchers
cat > "$stage/windows/Mindustry.bat" <<'BAT'
@echo off
setlocal
set "DIR=%~dp0"
if exist "%DIR%jre\bin\javaw.exe" (
    start "" "%DIR%jre\bin\javaw.exe" -jar "%DIR%Mindustry.jar" %*
) else (
    where javaw >nul 2>nul
    if errorlevel 1 (
        echo Java 17 or newer is required to run Mindustry.
        echo Install it from https://adoptium.net/ or use the bundled-jre build.
        pause
        exit /b 1
    )
    start "" javaw -jar "%DIR%Mindustry.jar" %*
)
endlocal
BAT

cat > "$stage/linux/mindustry.sh" <<'SH'
#!/bin/sh
# Launches Mindustry with the bundled runtime, or the system Java if absent.
DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -x "$DIR/jre/bin/java" ]; then
    JAVA="$DIR/jre/bin/java"
else
    JAVA=java
fi
exec "$JAVA" -XX:+ShowCodeDetailsInExceptionMessages -jar "$DIR/Mindustry.jar" "$@"
SH
chmod +x "$stage/linux/mindustry.sh"

# ------------------------------------------------------------- bundled JREs
linux_jre="not bundled"
windows_jre="not bundled"

if [ "$bundle" = "true" ]; then
    echo "Creating minimal Linux runtime with jlink..."
    jlink \
        --add-modules ALL-MODULE-PATH \
        --strip-debug --no-man-pages --no-header-files --compress=2 \
        --output "$stage/linux/jre"
    linux_jre="jlink runtime from $(java -version 2>&1 | head -n 1)"

    echo "Looking up a Temurin 17 Windows x64 JRE..."
    api="https://api.github.com/repos/adoptium/temurin17-binaries/releases?per_page=50"
    auth=()
    if [ -n "${GITHUB_TOKEN:-}" ]; then
        auth=(-H "Authorization: Bearer $GITHUB_TOKEN")
    fi
    releases="$(curl -fsSL "${auth[@]}" -H "Accept: application/vnd.github+json" "$api" || true)"
    url="$(printf '%s' "$releases" | jq -r '
        [ .[]
          | select(.tag_name | startswith("jdk-17"))
          | .assets[]
          | select(.name | test("^OpenJDK17U-jre_x64_windows_hotspot_.*\\.zip$"))
          | .browser_download_url
        ][0] // empty' 2>/dev/null || true)"

    if [ -z "$url" ]; then
        # Some tags ship only the full JDK; that works just as well as a runtime.
        url="$(printf '%s' "$releases" | jq -r '
            [ .[]
              | select(.tag_name | startswith("jdk-17"))
              | .assets[]
              | select(.name | test("^OpenJDK17U-jdk_x64_windows_hotspot_.*\\.zip$"))
              | .browser_download_url
            ][0] // empty' 2>/dev/null || true)"
    fi

    if [ -n "$url" ] && curl -fL --retry 3 -o /tmp/mindustry-win-jre.zip "$url"; then
        rm -rf /tmp/mindustry-win-jre && mkdir -p /tmp/mindustry-win-jre
        unzip -q /tmp/mindustry-win-jre.zip -d /tmp/mindustry-win-jre
        winhome="$(find /tmp/mindustry-win-jre -mindepth 1 -maxdepth 1 -type d -name 'jdk-17*' | head -n 1)"
        if [ -n "$winhome" ]; then
            mkdir -p "$stage/windows/jre"
            cp -a "$winhome/." "$stage/windows/jre/"
            windows_jre="$(basename "$winhome")"
        else
            echo "WARNING: unexpected JRE archive layout, skipping Windows runtime." >&2
        fi
    else
        echo "WARNING: could not fetch a Windows JRE; the Windows ZIP will need system Java 17." >&2
        cat > "$stage/windows/Mindustry.bat" <<'BAT'
@echo off
setlocal
set "DIR=%~dp0"
where javaw >nul 2>nul
if errorlevel 1 (
    echo Java 17 or newer is required to run Mindustry.
    echo Install it from https://adoptium.net/
    pause
    exit /b 1
)
start "" javaw -jar "%DIR%Mindustry.jar" %*
endlocal
BAT
    fi
fi

# ------------------------------------------------------------------ readmes
cat > "$stage/windows/README.txt" <<TXT
Mindustry 8 - Windows x64 build
===============================
Build:    $build_version
Commit:   $commit
Source:   https://github.com/$repo

HOW TO RUN
  Double-click "Mindustry.bat" (or run: java -jar Mindustry.jar).
  $windows_jre

Saves, mods and maps are stored in %APPDATA%\Mindustry.
Это сборка форка, а не официальный релиз Anuken/Mindustry.
TXT

cat > "$stage/linux/README.txt" <<TXT
Mindustry 8 - Linux x64 build
=============================
Build:    $build_version
Commit:   $commit
Source:   https://github.com/$repo

HOW TO RUN
  ./mindustry.sh          (or: java -jar Mindustry.jar)
  chmod +x mindustry.sh   if the executable bit was lost while unpacking
  $linux_jre

Saves, mods and maps are stored in ~/.local/share/Mindustry (or the XDG data dir).
Это сборка форка, а не официальный релиз Anuken/Mindustry.
TXT

# --------------------------------------------------------------------- zips
(cd "$stage/windows" && zip -q -r "$out/Mindustry-Windows-x64.zip" .)
(cd "$stage/linux" && zip -q -r "$out/Mindustry-Linux-x64.zip" .)
cp "$jar" "$out/Mindustry.jar"

(cd "$out" && sha256sum Mindustry-Windows-x64.zip Mindustry-Linux-x64.zip Mindustry.jar > SHA256SUMS.txt)

cat > "$out/release-notes.md" <<TXT
## Mindustry 8 — desktop builds for Windows and Linux

Built from [\`$commit\`](https://github.com/$repo/commit/$commit), in-game build string \`$build_version\`.
Latest change: $subject

These are builds of this fork, **not** official Anuken/Mindustry releases.

| Asset | Contents |
| --- | --- |
| \`Mindustry-Windows-x64.zip\` | \`Mindustry.jar\` + \`Mindustry.bat\` + bundled Java 17 runtime ($windows_jre) |
| \`Mindustry-Linux-x64.zip\` | \`Mindustry.jar\` + \`mindustry.sh\` + bundled Java 17 runtime ($linux_jre) |
| \`Mindustry.jar\` | universal cross-platform JAR (needs Java 17+ installed) |
| \`SHA256SUMS.txt\` | checksums for the three artifacts above |

Windows: unpack and run \`Mindustry.bat\`.
Linux: unpack and run \`./mindustry.sh\`.

The game JAR itself is platform-independent; the platform ZIPs differ only by the launcher
script and the bundled runtime. Requires OpenGL 2+/3+ capable drivers and the native
libraries included in the JAR. If the bundled runtime is absent, install JDK/JRE 17.

Flow-field pathfinding and the AI unit-avoidance mask are implemented in Rust and bundled
as JNI natives for Windows and Linux (\`libmindustry_native64.so\` / \`mindustry_native64.dll\`).
If a native library cannot be loaded, the game transparently falls back to the original
Java implementations of both hot paths.
TXT

echo "Packaged:"
ls -lh "$out/Mindustry-Windows-x64.zip" "$out/Mindustry-Linux-x64.zip" "$out/Mindustry.jar" "$out/SHA256SUMS.txt"
