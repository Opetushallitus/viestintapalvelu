#!/usr/bin/env bash

set -euo pipefail

if [ $# -ne 4 ]
then
    echo "usage: $0 JSESSIONID virkailija-host haku-oid caller-id"
    exit 1
fi

JSESSIONID="$1"
HOST="$2"
HAKU_OID="$3"
CALLER_ID="$4"

STATUS_FILE="$(mktemp)"
TEMPLATE_ID=$(curl -s -w '%{stderr}%{http_code}' -X POST -d @- \
                   -H 'Content-Type: application/json' \
                   -H "Caller-id: $CALLER_ID" \
                   -H "CSRF: $CALLER_ID" \
                   -H "Cookie: JSESSIONID=$JSESSIONID;CSRF=$CALLER_ID" \
                   "https://$HOST/viestintapalvelu/api/v1/template/insert" 2>"$STATUS_FILE")
STATUS=$(< "$STATUS_FILE")
rm "$STATUS_FILE"

if [ "$STATUS" != "200" ]
then
    echo "pohjan tuonti epäonnistui: HTTP $STATUS"
    exit 1
fi

STATUS=$(curl -s -o /dev/null -w '%{http_code}' -X POST \
              --data-raw '{"templateId":'"$TEMPLATE_ID"',"applicationPeriods":["'"$HAKU_OID"'"],"useAsDefault":false}' \
              -H 'Content-Type: application/json' \
              -H "Caller-id: $CALLER_ID" \
              -H "CSRF: $CALLER_ID" \
              -H "Cookie: JSESSIONID=$JSESSIONID;CSRF=$CALLER_ID" \
              "https://$HOST/viestintapalvelu/api/v1/template/saveAttachedApplicationPeriods")

if [ "$STATUS" != "200" ]
then
    echo "pohjan liittäminen hakuun $HAKU_OID epäonnistui: HTTP $STATUS"
    exit 1
fi
