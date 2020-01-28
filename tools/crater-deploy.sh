#!/usr/bin/env bash

# use the amount of commits behind master as a version number
behind=$(git rev-list --right-only --count master...crater) # https://stackoverflow.com/a/27940027/6056864

# configure some android values
echo "sdk.dir=/Users/quezler/Library/Android/sdk" > local.properties
sed -i'.original' 's/applicationId "io.anuke.mindustry"/applicationId "io.anuke.mindustry.craters"/g' android/build.gradle
sed -i'.original' 's/"io.anuke.mindustry"/"io.anuke.mindustry.craters"/g' android/AndroidManifest.xml
sed -i'.original' 's/Mindustry</Mindustry Craters</g' android/res/values/strings.xml

# delete sed backup files
rm android/build.gradle.original
rm android/AndroidManifest.xml.original
rm android/res/values/strings.xml.original

# build the 3 releases
./gradlew desktop:dist -Pbuildversion=$behind -PversionType=craters
./gradlew server:dist -Pbuildversion=$behind -PversionType=craters
./gradlew android:assembleDebug -Pbuildversion=$behind -PversionType=craters

# reset the android files to default
git checkout -- "android/build.gradle"
git checkout -- "android/AndroidManifest.xml"
git checkout -- "android/res/values/strings.xml"

# rename the output
mkdir ./.gradle/craters
mv desktop/build/libs/Mindustry.jar ./.gradle/craters/Mindustry-Craters-Desktop-${behind}.jar
mv server/build/libs/server-release.jar ./.gradle/craters/Mindustry-Craters-Server-${behind}.jar
mv android/build/outputs/apk/debug/android-debug.apk ./.gradle/craters/Mindustry-Craters-Android-${behind}.apk

# upload to (web)server
scp -r ./.gradle/craters root@mindustry.nydus.app:/var/www/html
scp ./.gradle/craters/Mindustry-Craters-Server-${behind}.jar root@mindustry.nydus.app:/root/crater/Mindustry-Craters-Server.jar
