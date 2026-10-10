#!/bin/sh -l

# ubuntu dockerfile is very minimal (only 122 packages are installed)
# need to install updated git (from official git ppa)
apt-get -q update
apt-get -yq install software-properties-common
add-apt-repository ppa:git-core/ppa -y
# install dependencies expected by other steps
apt-get -q update
apt-get -yq install git \
curl \
ca-certificates \
wget \
bzip2 \
zip \
unzip \
xz-utils \
sudo gnupg locales

# set Locale to en_US.UTF-8 (avoids hang during compilation)
locale-gen en_US.UTF-8
export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8
export LC_ALL=en_US.UTF-8

# add zulu apt repository - https://docs.azul.com/core/install/debian
curl -s https://repos.azul.com/azul-repo.key | gpg --dearmor -o /usr/share/keyrings/azul.gpg
echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | tee /etc/apt/sources.list.d/zulu.list
apt-get -q update
# install zulu JDK and Java build tools
apt-get -yq install zulu17-jdk-headless

# Install cross-compilation toolchains
apt-get -yq --force-yes install gcc g++
apt-get -yq --force-yes install gcc-aarch64-linux-gnu g++-aarch64-linux-gnu libc6-dev-arm64-cross
apt-get -yq --force-yes install gcc-arm-linux-gnueabihf g++-arm-linux-gnueabihf libc6-dev-armhf-cross
apt-get -yq --force-yes install gcc-riscv64-linux-gnu g++-riscv64-linux-gnu libc6-dev-riscv64-cross

# Build Linux natives
cd arc
./gradlew arc-core:jnigenBuildAllLinux --no-daemon