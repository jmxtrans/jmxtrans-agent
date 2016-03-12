#!/usr/bin/env bash

export CATALINA_OPTS="$CATALINA_OPTS \
   -javaagent:$(ls ../../../../target/jmxtrans-agent-*-SNAPSHOT.jar)=$CATALINA_BASE/jmxtrans-agent.xml \
   -Djmxtrans.agent.properties.file=$CATALINA_BASE/jmxtrans-agent.properties \
   -Dorg.jmxtrans.agent.util.logging.Logger.level=INFO \
   -Djmxtrans.agent.premain.delay=10"
