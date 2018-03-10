#!/usr/bin/env bash

# cd $HOME
git config --global user.email $GHEMAIL
git config --global user.name $GHUSERNAME

# add, commit and push files
git clone https://github.com/Anuken/Mindustry.wiki.git
cd Mindustry.wiki

DESKFILE="mindustry-desktop-bleeding-edge.jar"

if [ -e $DESKFILE ]; then
    rm $DESKFILE
fi

cp ../desktop/build/libs/desktop-release.jar $DESKFILE

FILE1="Home.md"

if [ -e $FILE1 ]; then
    rm $FILE1
fi

touch $FILE1

echo "#### Latest Bleeding Edge Build: "$TRAVIS_BUILD_NUMBER"" >> $FILE1
echo "###### Commit: "$TRAVIS_COMMIT"" >> $FILE1
echo >> $FILE1
echo "[Desktop JAR download.]("$DESKFILE")  " >> $FILE1
echo "*Requires Java to run, as usual.*" >> $FILE1

git add $FILE1
git add $DESKFILE
git commit -m "Added a new bleeding edge build"

git push https://$GHUSERNAME:$GHPASSWORD@github.com/Anuken/Mindustry.wiki.git --all
