#!/usr/bin/env bash

behind=$(git rev-list --right-only --count master...crater) # https://stackoverflow.com/a/27940027/6056864

echo "sdk.dir=/Users/quezler/Library/Android/sdk" > local.properties
sed -i'.original' 's/applicationId "io.anuke.mindustry"/applicationId "io.anuke.mindustry.craters"/g' android/build.gradle
sed -i'.original' 's/"io.anuke.mindustry"/"io.anuke.mindustry.craters"/g' android/AndroidManifest.xml
sed -i'.original' 's/Mindustry</Mindustry Craters</g' android/res/values/strings.xml

rm android/build.gradle.original
rm android/AndroidManifest.xml.original
rm android/res/values/strings.xml.original

./gradlew desktop:dist -Pbuildversion=$behind -PversionType=craters
./gradlew server:dist -Pbuildversion=$behind -PversionType=craters
./gradlew android:assembleDebug -Pbuildversion=$behind -PversionType=craters


git checkout -- "android/build.gradle"
git checkout -- "android/AndroidManifest.xml"
git checkout -- "android/res/values/strings.xml"

mkdir ./.gradle/craters

mv desktop/build/libs/Mindustry.jar ./.gradle/craters/Mindustry-Craters-Desktop-${behind}.jar
mv server/build/libs/server-release.jar ./.gradle/craters/Mindustry-Craters-Server-${behind}.jar
mv android/build/outputs/apk/debug/android-debug.apk ./.gradle/craters/Mindustry-Craters-Android-${behind}.apk

scp -r ./.gradle/craters root@mindustry.nydus.app:/var/www/html
scp ./.gradle/craters/Mindustry-Craters-Server-${behind}.jar root@mindustry.nydus.app:/root/crater/Mindustry-Craters-Server.jar
