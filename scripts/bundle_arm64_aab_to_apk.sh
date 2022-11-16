#!/usr/bin/env bash
set -o nounset
set -o errexit
set -o pipefail

target_name=MonochromePublic6432
arch=arm64

#copy keystore
cp ecosia.keystore chrome/android/

#bundle .aab to apk
out/release_${arch}/bin/monochrome_64_32_public_bundle build-bundle-apks --output-apks=out/release_${arch}/apks/${target_name}.apks --build-mode=universal

#unzip .apk
unzip -o out/release_${arch}/apks/${target_name}.apks -d out/release_${arch}/apks/

#rename .apk
mv -f out/release_${arch}/apks/universal.apk out/release_${arch}/apks/${target_name}.apk
