![Imgur](https://i.imgur.com/w4N0yhv.png)

[![Build Status](https://travis-ci.org/Anuken/Mindustry.svg?branch=master)](https://travis-ci.org/Anuken/Mindustry) 
[![Discord](https://img.shields.io/discord/391020510269669376.svg)](https://discord.gg/mindustry)

A sandbox tower defense game written in Java.

_[Trello Board](https://trello.com/b/aE2tcUwF/mindustry-40-plans)_  
_[Wiki](http://mindustry.wikia.com/wiki/Mindustry_Wiki)_ 

### Building

Bleeding-edge live builds are generated automatically for every commit. You can see them [here](https://jenkins.hellomouse.net/job/mindustry/).

If you'd rather compile on your own, follow these instructions.
First, make sure you have Java 8 and JDK 8 installed. Open a terminal in the root directory, `cd` to the Mindustry folder and run the following commands:

#### Windows

_Running:_ `gradlew desktop:run`  
_Building:_ `gradlew desktop:dist`

#### Linux/Mac OS

_Running:_ `./gradlew desktop:run`  
_Building:_ `./gradlew desktop:dist`

#### Server

Server builds are bundled with each released build (in Releases). If you'd rather compile on your own, replace 'desktop' with 'server', e.g. `gradlew server:dist`.

##### Troubleshooting

If the terminal returns `Permission denied` or `Command not found` on Mac/Linux, run `chmod +x ./gradlew` before running `./gradlew`. *This is a one-time procedure.*

---

Gradle may take up to several minutes to download files. Be patient. <br>
After building, the output .JAR file should be in `/desktop/build/libs/desktop-release.jar` for desktop builds, and in `/server/build/libs/server-release.jar` for server builds.

### Downloads

<a href="https://anuke.itch.io/mindustry"><img src="https://i.imgur.com/sk26hTV.png" width="auto" height="75"></a>

<a href="https://play.google.com/store/apps/details?id=io.anuke.mindustry&hl=en"><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Get_it_on_Google_play.svg/1280px-Get_it_on_Google_play.svg.png" width="auto" height="75"></a>
