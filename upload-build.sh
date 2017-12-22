#!/usr/bin/env bash

# cd $HOME
git config --global user.email $GHEMAIL
git config --global user.name $GHUSERNAME

# add, commit and push files
git clone https://github.com/Anuken/Mindustry.wiki.git
cd Mindustry.wiki

DESKFILE=$TRAVIS_COMMIT"-desktop-bleeding-edge.jar"
cp ../desktop/build/libs/desktop-release.jar $DESKFILE

FILE1="$TRAVIS_BUILD_NUMBER-Bleeding-Edge-Build-$TRAVIS_COMMIT.md"

if [ ! -e $FILE1 ]; then
    touch $FILE1
fi

NEWLINE=$'\n'
echo "### Travis Build #"$TRAVIS_BUILD_NUMBER".${NEWLINE}####Desktop JAR download: ["$DESKFILE"]("$DESKFILE")" >> $FILE1

git add $FILE1
git add $DESKFILE
git commit -m "Added a new bleeding edge build"
git push https://$GHUSERNAME:$GHPASSWORD@github.com/Anuken/Mindustry.wiki.git --all
