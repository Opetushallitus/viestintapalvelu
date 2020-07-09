#!/usr/bin/env python

import csv
import sys

col = int(sys.argv[1])
s = sys.argv[2]

reader = csv.reader(sys.stdin)
writer = csv.writer(sys.stdout)

writer.writerow(next(reader))
for row in reader:
    if row[col] == s:
        writer.writerow(row)
