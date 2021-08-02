#!/bin/bash

times="${1-100}"
for (( i=1; i<=$times; i++ ))
do
  echo "Restarting zookeeper nodes:" $i
  docker restart zoo1
  docker restart zoo2
  docker restart zoo3
  sleep 1
done
