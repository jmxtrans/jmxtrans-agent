#!/usr/bin/env bash

# set -x
set -e

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`
pushd $PRGDIR
PRGDIR=`pwd`
popd


if [ -r "$PRGDIR/.envrc" ]; then
  echo "Source file $PRGDIR/.envrc"
  . "$PRGDIR/.envrc"
else
  echo "No file $PRGDIR/.envrc found to source environment variables (e.g. STATSD_HOME)"
fi

if [ -z "$STATSD_HOME" ]; then
    echo "Unable to start as STATSD_HOME is not defined"
    exit 1
fi


CONFIG_FILE="$PRGDIR/statsd-config-console.js"

echo "START STATSD ..."
echo "STATSD_HOME: $STATSD_HOME"
echo "Statsd configuration file: $CONFIG_FILE"

pushd $STATSD_HOME
node stats.js $CONFIG_FILE
