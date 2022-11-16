#!/usr/bin/env bash

function print_help() {
cat <<-EOF
build [build-type] [cpu-type]
  build-type
    help (show this message)
    all (builds release versions)
    debug (builds arm debug version by default)
    release (builds arm release version by default)
  cpu-type
    arm (default)
    arm64
    x86
    x64
EOF
}


if [ "$1" == 'help' ]; then
    print_help
    exit 1;
fi

if [ "$1" == 'all' ]; then
    echo 'Building all releases'
    autoninja -C out/release_arm chrome_public_apk;
    autoninja -C out/release_arm64 chrome_public_apk;
    autoninja -C out/release_x86 chrome_public_apk;
    autoninja -C out/release_x64 chrome_public_apk;
    exit 1;
fi

if [ "$1" != "debug" -a "$1" != "release" ]; then
    echo "Parameter 1 must be either debug or release"
    exit 1;
fi

# if no second parameter provided then default to arm build
if [ $# -eq 1 ]; then
    autoninja -C out/$1_arm;
    exit 1;
fi

if [ "$2" != "arm" -a "$2" != "arm64" -a "$2" != "x86" -a "$2" != "x64" ]; then
    echo "Parameter 2 must be arm, arm64, x86 or x64"
    exit 1;
fi

autoninja -C out/$1_$2;
