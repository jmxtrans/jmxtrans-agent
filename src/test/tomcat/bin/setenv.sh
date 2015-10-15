#!/usr/bin/env bash

set -x

echo "setenv.sh"
export JAVA_OPTS="$JAVA_OPTS -javaagent:$(ls ../../../../target/jmxtrans-agent-*.jar)=$CATALINA_BASE/jmxtrans-agent.xml"
