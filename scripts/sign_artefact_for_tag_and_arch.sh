#!/usr/bin/env bash

set -o nounset
set -o errexit
set -o pipefail

function print_help() {
cat <<-EOF
build [version-tag] [arch]
  version-tag format: v_{name}_{code}. e.g "v_4.0.0_200"
  arch: arm, arm64, x86, x64
EOF
}

VNAME=""
VCODE=""

# There has to be two parameters: the tag and the architecture (arm, x86, arm64, x64)!
if [ $# -eq 2 ]; then
    VTAG="$1"
    ARCH="$2"
    
    IFS='_' read -ra my_array <<< "$VTAG"
    VNAME="${my_array[1]}"
    
    #Determine code for architecture, each arch has a different offset
    # arm = 0, x86 = 1 arm64 = 2, x64=3
    VCODE=""
    case $ARCH in
        "arm")
                VCODE="${my_array[2]}"
                ;;
        "x86")
                VCODE="$((${my_array[2]} + 1))"
                ;;
        "arm64")
                VCODE="$((${my_array[2]} + 2))"
                ;;        
        "x64")
                VCODE="$((${my_array[2]} + 3))"
                ;;

    esac
    echo "Setting VNAME=$VNAME"
    echo "Setting VCODE=$VCODE"

    #Move .aab to out-folder including version codes
    mkdir -p out/upload
    OUT_AAB_PATH="out/upload/${ARCH}_${VNAME}_${VCODE}_MonochromePublic.aab"

    if [ $ARCH = "x64" ]; then
    	mv out/release_${ARCH}/apks/MonochromePublic6432.aab $OUT_AAB_PATH
    else  
    	mv out/release_${ARCH}/apks/MonochromePublic.aab $OUT_AAB_PATH
    fi

    #Codesign .aab 
    jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore ecosia.keystore -storepass $ANDROID_KEYSTORE_PASSWORD $OUT_AAB_PATH ecosia

    #Move all mapping files
    if [ $ARCH = "x64" ]; then
    	mv out/release_${ARCH}/apks/MonochromePublic6432.aab.mapping ${OUT_AAB_PATH}.mapping.txt
    else  
    	mv out/release_${ARCH}/apks/MonochromePublic.aab.mapping ${OUT_AAB_PATH}.mapping.txt
    fi
else 
    echo "No tag and arch defined! Please append at least two params in this format v_{name}_{code}. e.g \"v_4.0.0_200\" + arch (
        arm, arm64, x86, x64)"
fi
