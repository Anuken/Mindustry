git config --global user.name "Wiki Updater"
git clone --depth=1 --branch=master https://github.com/MindustryGame/wiki ../wiki
git clone --depth=1 --branch=master https://github.com/Anuken/Mindustry-Wiki-Generator ../Mindustry-Wiki-Generator
cd ../Mindustry-Wiki-Generator
./gradlew run
cd ../wiki
git add .
git commit -m "Update to match build ${TRAVIS_TAG}"
git push https://Anuken:${GH_PUSH_TOKEN}@github.com/MindustryGame/wiki
