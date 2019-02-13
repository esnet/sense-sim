#!/bin/bash

for i in `ls sense*.pid`; do
  if [ -f $i ]; then
    echo "Stopping $i."
    kill -9 `cat $i`
  fi
done

