#!/usr/bin/env bash
set -eux -o pipefail

# Required tools: docker python
# Assuming default python to be python2.

chromium_version=85.0.4183.127

## Clone chromium repo
git clone --depth 1 --no-tags https://chromium.googlesource.com/chromium/src.git -b ${chromium_version} src

## Fetch depot-tools
depot_tools_commit=$(grep 'depot_tools.git' src/DEPS | cut -d\' -f8)
mkdir -p depot_tools
pushd depot_tools
git init
git remote add origin https://chromium.googlesource.com/chromium/tools/depot_tools.git
git fetch --depth 1 --no-tags origin "${depot_tools_commit}"
git reset --hard FETCH_HEAD
popd
OLD_PATH=$PATH
export PATH="$(pwd -P)/depot_tools:$PATH"
pushd src/third_party
ln -s ../../depot_tools
popd

## Sync files
gclient.py sync --no-history --shallow --revision=${chromium_version}

#build the Docker image
docker build -t ecosiadev/chromium:vanilla-${chromium_version}-synced --build-arg depot_tools_commit=${depot_tools_commit} .
