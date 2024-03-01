#!/usr/bin/env python3
import sys
from itertools import groupby
from operator import itemgetter

def read_mapper_output(infile):
    for line in infile:
        yield line.rstrip().split('\t', 1)

def main():
    data = read_mapper_output(sys.stdin)
    for current_subreddit, group in groupby(data, itemgetter(0)):
        try:
            total_count = sum(int(count) for current_subreddit, count in group)
            print(f"{current_subreddit}\t{total_count}")
        except ValueError:
            pass

if __name__ == '__main__':
    main()
