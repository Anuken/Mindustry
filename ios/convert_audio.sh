#!/usr/bin/bash

#convert ogg to .caf files for iOS
for i in $1/*.ogg; do
  echo $i
  ffmpeg -i "$i" "${i%.*}.caf"
done