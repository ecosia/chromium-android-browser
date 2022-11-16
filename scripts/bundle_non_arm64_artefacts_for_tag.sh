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
    VCODE_ARM="${my_array[2]}"
    VCODE_X86="$((${my_array[2]} + 1))"
    VCODE_X64="$((${my_array[2]} + 3))"

    echo "Setting VNAME=$VNAME"
    echo "Setting VCODE_ARM=$VCODE_ARM"
    echo "Setting VCODE_X86=$VCODE_X86"
    echo "Setting VCODE_X64=$VCODE_X64"

    #Move all .aar
    mkdir -p out/upload
    ARM_AAB="out/upload/arm_${VNAME}_${VCODE_ARM}_ChromeModernPublic.aab"
    X86_AAB="out/upload/x86_${VNAME}_${VCODE_X86}_ChromeModernPublic.aab"
    X64_AAB="out/upload/x64_${VNAME}_${VCODE_X64}_ChromeModernPublic.aab"

    mv out/release_arm/apks/ChromeModernPublic.aab $ARM_AAB
    mv out/release_x86/apks/ChromeModernPublic.aab $X86_AAB
    mv out/release_x64/apks/ChromeModernPublic.aab $X64_AAB

    #Codesign .aab 
    for bundle in $ARM_AAB $X86_AAB $X64_AAB
    do
      jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore ecosia.keystore -storepass $ANDROID_KEYSTORE_PASSWORD $bundle ecosia
    done

    #Move all mapping files
    mv out/release_arm/apks/ChromeModernPublic.aab.mapping ${ARM_AAB}.mapping.txt
    mv out/release_x86/apks/ChromeModernPublic.aab.mapping ${X86_AAB}.mapping.txt
    mv out/release_x64/apks/ChromeModernPublic.aab.mapping ${X64_AAB}.mapping.txt

    #Bundle and zip arm native symbols
    mkdir armeabi-v7a
    mv out/release_arm/lib.unstripped/*.so armeabi-v7a
    zip -r native_debug_symbols_arm.zip armeabi-v7a
    mv native_debug_symbols_arm.zip out/upload

else 
    echo "No tag defined! Please append at least one param in this format v_{name}_{code}. e.g \"v_4.0.0_200\""
fi
