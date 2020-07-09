#!/usr/bin/env bash

set -o pipefail

if [ -z "$3" ]; then
  echo "Usage: $0 <email body file> <email subject> <ryhmasahkoposti JSESSIONID> [environment virkailija URL]"
  exit 2
fi

while read EMAIL; do
    ./laheta.bash "$EMAIL" "$2" '' '' "$3" "$4" < "$1"
done
