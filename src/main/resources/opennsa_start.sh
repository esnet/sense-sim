#!/bin/bash

for i in `ls nsa*.tac`; do
  if [ -f $i ]; then
    echo "Starting $i with pid file ${i%.*}.pid"
    nohup twistd -noy $i --pidfile ${i%.*}.pid &
  fi
done
