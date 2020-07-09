#!/usr/bin/env python

import csv
import sys

reader = csv.reader(sys.stdin)

print('<table>')
print('<thead><tr>', end='')
for header in next(reader):
    print('<td>' + header + '</td>', end='')
print('</tr></thead>')
for row in reader:
    print('<tr>', end='')
    for col in row:
        print('<td>' + col + '</td>', end='')
    print('</tr>')
print('</table>')
