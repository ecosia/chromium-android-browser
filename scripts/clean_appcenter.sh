#!/usr/bin/env bash
set -o nounset
set -o pipefail

### use this script to bulk delete apps from appcenter in the given range of ids

### usage
### npm install -g appcenter-cli (optional)
### appcenter login
### ./clean_appcenter.sh [app_name] [start] [end]


function print_help() {
cat <<-EOF
build [app_name] [start] [end] 
  Format: Int e.g. Ecosia-GmbH/Ecosia-Beta 1 10
EOF
}

# There has to be 3 params!
if [ $# -eq 3 ]; then
    APP_NAME="$1" 
    START="$2"
    END="$3"
    
    echo "Deleting releases $APP_NAME from $START to $END"

    APPC_COMMON="--quiet --disable-telemetry"
    
    for ((i=$START; i<=$END; i++))
    do
      appcenter distribute releases delete -r $i --app $APP_NAME $APPC_COMMON || continue $?    
    done 
else 
    echo "Not enough parameters defined! Please define app_name, from and to. e.g \"Ecosia-GmbH/Ecosia-Beta\"  1 10"
fi
