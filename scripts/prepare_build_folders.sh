#!/usr/bin/env bash

function print_help() {
cat <<-EOF
build [version-name] [version-code]
  Both parameters are optional.
EOF
}

VNAME=""
VCODE=""

# If there are parameters, there has to be 2
if [ $# -eq 2 ]; then
    VNAME="--version-name $1"
    VCODE="--version-code $2"
    VCODE_X86="--version-code $(($2 + 1))"
    VCODE_64="--version-code $(($2 + 2))"
    VCODE_X64="--version-code $(($2 + 3))"
fi

echo "Preparing build folders for: debug_arm, release_arm, debug_arm64, release_arm64, debug_x86, release_x86, debug_x64, release_x64"
echo "Running command python3 scripts/helpers/prepare_build_folder.py debug arm $VNAME $VCODE"
python3 scripts/helpers/prepare_build_folder.py debug arm $VNAME $VCODE
python3 scripts/helpers/prepare_build_folder.py release arm $VNAME $VCODE
python3 scripts/helpers/prepare_build_folder.py debug arm64 $VNAME $VCODE_64
python3 scripts/helpers/prepare_build_folder.py release arm64 $VNAME $VCODE_64
python3 scripts/helpers/prepare_build_folder.py debug x86 $VNAME $VCODE_X86
python3 scripts/helpers/prepare_build_folder.py release x86 $VNAME $VCODE_X86
python3 scripts/helpers/prepare_build_folder.py debug x64 $VNAME $VCODE_X64
python3 scripts/helpers/prepare_build_folder.py release x64 $VNAME $VCODE_X64
