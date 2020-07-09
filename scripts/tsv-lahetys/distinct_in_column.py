#!/usr/bin/env python

import csv
import sys

col = int(sys.argv[1])

distinct = set()
reader = csv.reader(sys.stdin)

next(reader)
for row in reader:
    distinct.add(row[col])

for d in distinct:
    print(d)
