#!/bin/bash

node="${1-1}"

case $node in

  1)
    port=2181
    ;;

  2)
    port=2182
    ;;

  3)
    port=2181
    ;;

  *)
    echo "Invalid node specified, please specify a number between 1 and 3"
    exit 1
    ;;
esac

echo dump | nc localhost $port