#!/usr/bin/env bash

set -o pipefail

if [ -z "$5" ]; then
  echo "Usage: $0 <csv file> <email subject> <email header> <email footer> <ryhmasahkoposti JSESSIONID> [environment virkailija URL]"
  exit 2
fi

IFS=$'\n'
for EMAIL in $(./distinct_in_column.py 0 < "$1"); do
    ./column_matches.py 0 "$EMAIL" < "$1" | ./remove_column.py 0 | ./csv_to_html.py | ./laheta.bash "$EMAIL" "$2" "$3" "$4" "$5" "$6"
done
