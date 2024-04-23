#!/usr/bin/env bash

set -o nounset
set -o errexit
set -o pipefail

function print_help() {
cat <<-EOF
build [version-tag] 
  Format: v_{name}_{code}. e.g "v_4.0.0_200"
EOF
}

VNAME=""
VCODE=""

# There has to be one param!
if [ $# -eq 1 ]; then
    VTAG="$1"
    IFS='_' read -ra my_array <<< "$VTAG"
    VNAME="${my_array[1]}"
    VCODE_ARM64="$((${my_array[2]} + 2))"

    echo "Setting VNAME=$VNAME"
    echo "Setting VCODE_ARM64=$VCODE_ARM64"

    APPC_COMMON="--quiet --disable-telemetry -a Ecosia-GmbH/Ecosia --token $ANDROID_APPCENTER_STORE_TOKEN"
    APPC_RELEASE="$APPC_COMMON -g Collaborators --silent"

    #Move all .aar
    mkdir -p out/upload
    ARM64_APK="out/upload/arm64_${VNAME}_${VCODE_ARM64}_MonochromePublic6432.apk"
    ARM64_AAB="out/upload/arm64_${VNAME}_${VCODE_ARM64}_MonochromePublic6432.aab"

    mv out/release_arm64/apks/MonochromePublic6432.apk $ARM64_APK
    mv out/release_arm64/apks/MonochromePublic6432.aab $ARM64_AAB

    #Codesign .aab 
    for bundle in $ARM64_AAB
    do
      jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore ecosia.keystore -storepass $ANDROID_KEYSTORE_PASSWORD $bundle ecosia
    done
    
    #Move all mapping files
    mv out/release_arm64/apks/MonochromePublic6432.aab.mapping ${ARM64_AAB}.mapping.txt
   
    #Bundle and zip arm64 native symbols
    mkdir arm64-v8a
    mv out/release_arm64/lib.unstripped/*.so arm64-v8a
    zip -r native_debug_symbols_arm64.zip arm64-v8a
    mv native_debug_symbols_arm64.zip out/upload

    #Upload to AppCenter
    echo "Upload arm64 version"
    appcenter distribute release -f $ARM64_APK $APPC_RELEASE -r arm64  
    appcenter crashes upload-mappings -m ${ARM64_AAB}.mapping.txt $APPC_COMMON -c $VCODE_ARM64 -n $VNAME
else 
    echo "No tag defined! Please append at least one param in this format v_{name}_{code}. e.g \"v_4.0.0_200\""
fi
