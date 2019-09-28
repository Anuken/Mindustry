#!/usr/bin/bash

cd $1

#convert ogg to .mp3 files for iOS
for i in *.ogg; do
  echo $i
  ffmpeg -i "$i" "OUT_${i%.*}.mp3"
done

find . -type f ! -name "OUT_*" -delete

for file in OUT_*; do mv "$file" "${file#OUT_}"; done;

cd ../../
