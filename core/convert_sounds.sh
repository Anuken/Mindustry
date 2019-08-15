#convert from stereo to mono
for i in assets/sounds/*.ogg; do
  echo $i
  ffmpeg -i "$i" -y -ac 1 "$i"
done
