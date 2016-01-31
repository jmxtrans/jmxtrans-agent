#!/usr/bin/env bash

export CATALINA_OPTS="$CATALINA_OPTS \
   -javaagent:$(ls ../../../../target/jmxtrans-agent-*-SNAPSHOT.jar)=$CATALINA_BASE/jmxtrans-agent.xml \
   -Dorg.jmxtrans.agent.util.logging.Logger.level=FINEST"
