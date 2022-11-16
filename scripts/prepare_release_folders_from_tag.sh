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

# If there are parameters, there has to be 2
if [ $# -eq 1 ]; then
    VTAG="$1"
    IFS='_' read -ra my_array <<< "$VTAG"
    VNAME="--version-name ${my_array[1]}"
    VCODE="--version-code ${my_array[2]}"
    VCODE_X86="--version-code $((${my_array[2]} + 1))"
    VCODE_64="--version-code $((${my_array[2]} + 2))"
    VCODE_X64="--version-code $((${my_array[2]} + 3))"
fi

echo "Preparing build folders for: release_arm, release_arm64, release_x86, release_x64"
python3 scripts/helpers/prepare_build_folder.py release arm $VNAME $VCODE
python3 scripts/helpers/prepare_build_folder.py release arm64 $VNAME $VCODE_64
python3 scripts/helpers/prepare_build_folder.py release x86 $VNAME $VCODE_X86
python3 scripts/helpers/prepare_build_folder.py release x64 $VNAME $VCODE_X64
