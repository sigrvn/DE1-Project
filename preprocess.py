#!/usr/bin/env python3
import json

MAX_FILESIZE = 536870912 # 512 MB
DATA_FILEPATH = "/home/ubuntu/corpus-webis-tldr-17.json"

def read_input(infile):
    with open(infile, 'r', encoding='utf-8') as f:
        for line in f:
            yield line.strip()

def write_to_file(filename, data):
    print(f"Writing data to file {filename}...")
    with open(filename, 'w') as f:
        for d in data:
            json.dump(d, f)

def main():
    data = []
    total_size = 0
    file_no = 0

    for line in read_input(DATA_FILEPATH):
        if total_size >= MAX_FILESIZE:
            filename = f"comments_{file_no}.json"
            write_to_file(filename, data)
            data.clear()
            total_size = 0
            file_no += 1

        data.append(json.loads(line))
        total_size += len(line)

if __name__ == '__main__':
    main()
