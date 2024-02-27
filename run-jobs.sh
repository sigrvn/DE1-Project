#!/bin/bash

HADOOP_DIR=/home/ubuntu/hadoop-3.3.6
PROJECT_DIR=/home/ubuntu/DE1-Project
HDFS_BASEDIR=/user/ubuntu

$HADOOP_DIR/bin/hadoop jar $HADOOP_DIR/share/hadoop/tools/lib/hadoop-streaming-3.3.6.jar \
	-mapper $PROJECT_DIR/mapper.py \
	-reducer $PROJECT_DIR/reducer.py \
	-input $HDFS_BASEDIR/corpus-webis-tldr-17.json \
	-output $HDFS_BASEDIR/output
