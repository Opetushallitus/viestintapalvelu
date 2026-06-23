#!/bin/bash
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "Käyttö: $0 <vanha_hakemisto> <uusi_hakemisto>"
  echo "Esim:   $0 kk_hyvaksymiskirje_syksy_2025 kk_hyvaksymiskirje_syksy_2026"
  exit 1
fi

VANHA="$1"
UUSI="$2"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ ! -d "$SCRIPT_DIR/$VANHA" ]; then
  echo "Virhe: hakemistoa '$VANHA' ei löydy"
  exit 1
fi

if [ -d "$SCRIPT_DIR/$UUSI" ]; then
  echo "Virhe: hakemisto '$UUSI' on jo olemassa"
  exit 1
fi

# Kopioi hakemisto
cp -r "$SCRIPT_DIR/$VANHA" "$SCRIPT_DIR/$UUSI"

# Nimeä tiedostot uudelleen: korvaa vanha nimi uudella tiedostonimissä
find "$SCRIPT_DIR/$UUSI" -type f -name "*${VANHA}*" | while read -r tiedosto; do
  hakemisto="$(dirname "$tiedosto")"
  vanha_nimi="$(basename "$tiedosto")"
  uusi_nimi="${vanha_nimi//$VANHA/$UUSI}"
  mv "$tiedosto" "$hakemisto/$uusi_nimi"
done

echo "Kopioitu: $VANHA -> $UUSI"
