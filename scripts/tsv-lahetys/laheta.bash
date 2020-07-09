#!/usr/bin/env bash

set -o pipefail

if [ -z "$5" ]; then
  echo "Usage: $0 <email address> <email subject> <email header> <email footer> <ryhmasahkoposti JSESSIONID> [environment virkailija URL]"
  echo "...and let the contents come via stdin."
  exit 2
fi

RECIPIENT="$1"
SUBJECT="$2"
HEADER="$3"
FOOTER="$4"
JSESSIONID="$5"
if [ -z "$6" ]; then
  VIRKAILIJA_URL=https://virkailija.testiopintopolku.fi
else
  VIRKAILIJA_URL="$6"
fi

API_URL="${VIRKAILIJA_URL}/ryhmasahkoposti-service/email?sanitize=false"

REQUEST_TEMPLATE_TEMP=$(mktemp)
REQUEST_CONTENT_TEMP=$(mktemp)
JQ_RAW_TEMP=$(mktemp)

cat << EOF > "$REQUEST_TEMPLATE_TEMP"
{
  "recipient": [
    {
      "email": "${RECIPIENT}",
      "languageCode": "fi",
      "name": "${RECIPIENT}",
      "recipientReplacements": [],
      "attachments": [],
      "attachInfo": []
    }
  ],
  "replacements": [],
  "email": {
    "callingProcess": "Manuaalinen postitus",
    "from": "noreply@opintopolku.fi",
    "sender": "Opintopolku",
    "replyTo": "noreply@opintopolku.fi",
    "senderOid": "1.2.246.562.10.00000000001",
    "organizationOid": "1.2.246.562.10.00000000001",
    "subject": "${SUBJECT}",
    "body": "just a placeholder to be overwritten by jq",
    "html": false,
    "charset": "UTF-8",
    "attachments": [],
    "attachInfo": [],
    "languageCode": "fi",
    "sourceRegister": [],
    "hakuOid": "",
    "valid": false
  }
}
EOF

(echo "$HEADER" && cat && echo "$FOOTER") | jq -Rs > "$JQ_RAW_TEMP"

jq --slurpfile texts "$JQ_RAW_TEMP" '.email.body=$texts[0]' "$REQUEST_TEMPLATE_TEMP" > "$REQUEST_CONTENT_TEMP"

curl "$API_URL" \
  -v \
  -H "Cookie: JSESSIONID=${JSESSIONID}" \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -H "Caller-Id: 1.2.246.562.10.00000000001.kehittaja.$(whoami)" \
  --data @"$REQUEST_CONTENT_TEMP"

rm "$REQUEST_TEMPLATE_TEMP" "$REQUEST_CONTENT_TEMP" "$JQ_RAW_TEMP"
