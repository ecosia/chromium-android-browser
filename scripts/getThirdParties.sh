#!/bin/bash
PROJECT_ROOT=`cd "$1"; pwd`
cp -f $PROJECT_ROOT/src/scripts/.gclient $PROJECT_ROOT/.gclient
cp -f $PROJECT_ROOT/src/scripts/.gclient_entries $PROJECT_ROOT/.gclient_entries
cd $PROJECT_ROOT/src

#make sure gclient runs with python2
export GCLIENT_PY3=0

#sync 3rd parties without history and deleting old ones
gclient sync --no-history -D

#install build dependencies
RELEASE_ID=`awk -F= '$1=="ID" { print $2 ;}' /etc/os-release`

echo "Detected Linux Release: $RELEASE_ID"
echo "Installing appropriate build dependencies."

if [ $RELEASE_ID = "arch" ]; then
    # Dependencies from https://chromium.googlesource.com/chromium/src/+/refs/heads/main/docs/linux/build_instructions.md#arch-linux
    sudo pacman -S --needed python perl gcc gcc-libs bison flex gperf pkgconfig \
        nss alsa-lib glib2 gtk3 nspr freetype2 cairo dbus libgnome-keyring \
        xorg-server-xvfb xorg-xdpyinfo \
        lib32-glibc lib32-gcc-libs # <- those two were missing in the official instructions but required to successfully build the project
else
    $PROJECT_ROOT/src/build/install-build-deps.sh --android
fi

#install hooks
cd $PROJECT_ROOT/src
gclient runhooks

#prepare build folders
cd $PROJECT_ROOT/src
./scripts/prepare_build_folders.sh

sh $PROJECT_ROOT $PROJECT_ROOT/src/build/android/envsetup.sh
