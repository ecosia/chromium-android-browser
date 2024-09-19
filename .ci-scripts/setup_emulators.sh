#!/bin/bash

# This file is part of eyeo Chromium SDK,
# Copyright (C) 2006-present eyeo GmbH
# eyeo Chromium SDK is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3 as
# published by the Free Software Foundation.
# eyeo Chromium SDK is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# You should have received a copy of the GNU General Public License
# along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.

# Setup emulators for testing
#!/bin/bash

PORTS=$PORTS
CONTAINER_NAME=$CONTAINER_NAME
ANDROID_IMAGE=$ANDROID_IMAGE

create_emulators() {
    sudo modprobe binder_linux devices="binder,hwbinder,vndbinder"
    for port in $PORTS; do
        docker run -itd --rm --privileged --name $CONTAINER_NAME-$port --pull always -v ~/data-$port:/data -p $port:5555 $ANDROID_IMAGE
    done
}

prepare_emulators() {
    adb kill-server
    for port in $PORTS; do
        adb connect localhost:${port}
        adb -s localhost:${port} wait-for-device
        adb -s localhost:${port} root
    done
}


clean_up_containers() {
    for port in $PORTS; do
        if [ "$(docker ps -q -f name=$CONTAINER_NAME-${port})" ]; then
          docker stop $CONTAINER_NAME-${port};
        fi
        sudo rm -rf ~/data-$port;
    done
}
# Sometimes "ghost" devices are left behind, so we need to reboot them in order to clean up
disconnect_devices() {
    devices=$(adb devices | grep -w "device" | awk '{print $1}')
    for device in $devices; do
        adb -s $device -e reboot
    done
    adb kill-server
}

case "$1" in
    setup_emulators)
        create_emulators
        prepare_emulators
        ;;
    teardown_emulators)
        clean_up_containers
        disconnect_devices
        ;;
    *)
        echo "Select a valid option: setup_emulators or teardown_emulators"
        exit 1
        ;;
esac