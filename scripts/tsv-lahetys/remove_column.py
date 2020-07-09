#!/usr/bin/env python

import csv
import sys

col = int(sys.argv[1])

reader = csv.reader(sys.stdin)
writer = csv.writer(sys.stdout)

for row in reader:
    del(row[col])
    writer.writerow(row)
