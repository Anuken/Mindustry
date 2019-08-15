#convert from stereo to mono
cd assets/sounds/
for i in *.ogg; do
  echo $i
  ffmpeg -i "$i" -ac 1 "OUT_$i"
done

find . -type f ! -name "OUT_*" -delete

for file in OUT_*; do mv "$file" "${file#OUT_}"; done;

cd ../../
