#!/usr/bin/env python3
import re
import json
import sys

def read_input(infile):
    for line in infile:
        yield line.strip()

def main():
    for line in read_input(sys.stdin):
        # Skip newlines
        if len(line) == 0:
            continue

        post = json.loads(line)

        content = post.get('content')
        subreddit = post.get('subreddit')
        if content is None:
            continue

        words = re.findall(r'\b\w+\b', content.lower())
        if 'the' in words:
            print(f"{subreddit}\t1")

if __name__ == '__main__':
    main()
