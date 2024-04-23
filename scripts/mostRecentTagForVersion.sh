git ls-remote --tags upstream | awk -F/ '{print $3}' | grep "^$1\." | sort -n | tail -n 1
