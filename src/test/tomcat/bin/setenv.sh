#!/usr/bin/env bash

export JAVA_OPTS="$JAVA_OPTS -javaagent:$(ls ../../../../target/jmxtrans-agent-*-SNAPSHOT.jar)=$CATALINA_BASE/jmxtrans-agent.xml"
