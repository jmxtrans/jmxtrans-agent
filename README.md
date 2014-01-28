# Jmxtrans Agent

[![Build Status](https://jmxtrans.ci.cloudbees.com/job/jmxtrans-agent/badge/icon)](https://jmxtrans.ci.cloudbees.com/job/jmxtrans-agent/)

## What is jmxtrans-agent ?

jmxtrans-agent is a version of [jmxtrans](http://jmxtrans.org/) intended to be used as a java agent. JmxTrans Agent has zero dependencies to ease integration.

## Java Agent Declaration

Download [jmxtrans-agent-1.0.6.jar](http://repo1.maven.org/maven2/org/jmxtrans/agent/jmxtrans-agent/1.0.6/jmxtrans-agent-1.0.6.jar)

Sample `setenv.sh` for Apache Tomcat

```
export JAVA_OPTS="$JAVA_OPTS -javaagent:/path/to/jmxtrans-agent-1.0.6.jar=jmxtrans-agent.xml"
```

* java agent jar path can be relative to the working dir
* `jmxtrans-agent.xml` is the configuration file, can be classpath relative (`classpath:…`), http(s) (`http(s)://...`) or file system system based (relative to the working dir)

## Query configuration

### Simple mono-valued attribute

Use `attribute` to specify the value to lookup. See `javax.management.MBeanServer.getAttribute(objectName, attribute)`.

```xml
<query objectName="java.lang:type=Threading" attribute="ThreadCount"
   resultAlias="jvm.thread"/>
```

### MBean Composite Data attribute

Use `key` to specify the key of the CompositeData. See `javax.management.openmbean.CompositeData#get(key)`.

```xml
 <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="used"
    resultAlias="jvm.heapMemoryUsage.used"/>
```

### Multi-valued attribute (Iterable or array)

Use `position` to specify the value to lookup. Position is `0 based.

```xml
 <query objectName="MyApp:type=MyMBean" attribute="MyMultiValuedAttribute" position="2"
    resultAlias="myMBean.myMultiValuedAttributeValue"/>
```

If `position` is not specified, all the values of the attribute are outputted with the name `${resultAlias}_${position}`.

Sample: `myMBean.myMultiValuedAttributeValue_0`, `myMBean.myMultiValuedAttributeValue_1`, ...


## Sample configuration file

Sample `jmxtrans-agent.xml` configuration file for Tomcat:

```xml
<jmxtrans-agent>
    <queries>
        <!-- OS -->
        <query objectName="java.lang:type=OperatingSystem" attribute="SystemLoadAverage" resultAlias="os.systemLoadAverage"/>

        <!-- JVM -->
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="used"
               resultAlias="jvm.heapMemoryUsage.used"/>
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="committed"
               resultAlias="jvm.heapMemoryUsage.committed"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage" key="used"
               resultAlias="jvm.nonHeapMemoryUsage.used"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage" key="committed"
               resultAlias="jvm.nonHeapMemoryUsage.committed"/>
        <query objectName="java.lang:type=ClassLoading" attribute="LoadedClassCount" resultAlias="jvm.loadedClasses"/>

        <query objectName="java.lang:type=Threading" attribute="ThreadCount" resultAlias="jvm.thread"/>

        <!-- TOMCAT -->
        <query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="requestCount"
               resultAlias="tomcat.requestCount"/>
        <query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="errorCount"
               resultAlias="tomcat.errorCount"/>
        <query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="processingTime"
               resultAlias="tomcat.processingTime"/>
        <query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="bytesSent"
               resultAlias="tomcat.bytesSent"/>
        <query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="bytesReceived"
               resultAlias="tomcat.bytesReceived"/>

        <!-- APPLICATION -->
        <query objectName="Catalina:type=Manager,context=/,host=localhost" attribute="activeSessions"
               resultAlias="application.activeSessions"/>
    </queries>
    <outputWriter class="org.jmxtrans.agent.GraphitePlainTextTcpOutputWriter">
        <host>localhost</host>
        <port>2003</port>
        <namePrefix>app_123456.servers.i876543.</namePrefix>
    </outputWriter>
    <outputWriter class="org.jmxtrans.agent.ConsoleOutputWriter"/>
    <collectIntervalInSeconds>20</collectIntervalInSeconds>
</jmxtrans-agent>
```

**Note** why xml and not json ? because XML parsing is out of the box in the JVM when json requires additional libraries.



## OutputWriters

OutputWriters are very simple to develop, you just have to extend [AbstractOutputWriter.java](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/AbstractOutputWriter.java) or to implement [OutputWriter.java](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/OutputWriter.java).

Out of the box output writers

* [GraphitePlainTextTcpOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/GraphitePlainTextTcpOutputWriter.java): output to Graphite Carbon plain text protocol on TCP. Configuration parameters:
  * `enabled`: to enable/disable the output writer. Optional, default value `true` 
  * `host`: Graphite Carbon listener host
  * `port`: Graphite Carbon Plain Text TCP listener port. Optional, default value `2003`
  * `namePrefix`; prefix of the metric name. Optional, default values `servers.#hostname#.` where `#hostname#` is the auto discovered hostname of computer with `.` escpaed as `_` (`InetAddress.getLocalHost().getHostName()`).
* [FileOverwriterOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/FileOverwriterOutputWriter.java): store the last collection of metrics in a file. Configuration parameters:
  * `fileName`: name of the file in which the collected metrics are stored. Optional, default value `jmxtrans-agent.data` (in JVM working dir, for example `$TOMCAT_HOME/bin`)
* [SummarizingFileOverwriterOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/SummarizingFileOverwriterOutputWriter.java): Similar to the `FileOverwriterOutputWriter` but displays "per minute" values for counters of type `counter`
* [ConsoleOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/ConsoleOutputWriter.java): output metric values to `stdout`
* [SummarizingConsoleOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/SummarizingConsoleOutputWriter.java): Similar to the `ConsoleOutputWriter` but displays "per minute" values for counters of type `counter`

Output writers configuration support  an expression language based on property placeholders with the `{prop-name[:default-value]}` syntax (e.g. "`${graphite.host:2003}`").

The `default-value` is optional. An exception is raised if no default value is defined and the property placeholder is not found.

Environment variables are looked-up in the following order:

1. JVM system properties (```System.getProperty("graphite.host")```)
1. JVM environment variables (```System.getenv("graphite.host")```)
1. JVM environment variables after a "to-upper-case + dot-to-underscore" transformation (```System.getenv("GRAPHITE_HOST")```)

## Sample of ConsoleOutputWriter

```
os.systemLoadAverage 1.80419921875 1366199958
jvm.heapMemoryUsage.used 20438792 1366199958
jvm.heapMemoryUsage.committed 119668736 1366199958
jvm.nonHeapMemoryUsage.used 15953560 1366199958
jvm.nonHeapMemoryUsage.committed 24313856 1366199958
jvm.loadedClasses 2162 1366199958
jvm.thread 13 1366199958
tomcat.requestCount 0 1366199958
tomcat.requestCount 0 1366199958
tomcat.errorCount 0 1366199958
tomcat.errorCount 0 1366199958
tomcat.processingTime 0 1366199958
tomcat.processingTime 0 1366199958
tomcat.bytesSent 0 1366199958
tomcat.bytesSent 0 1366199958
tomcat.bytesReceived 0 1366199958
tomcat.bytesReceived 0 1366199958
application.activeSessions 0 1366199958
```

## Sample of FileOverwriterOutputWriter

```
os.systemLoadAverage 1.27734375
jvm.heapMemoryUsage.used 33436016
jvm.heapMemoryUsage.committed 133365760
jvm.nonHeapMemoryUsage.used 23623096
jvm.nonHeapMemoryUsage.committed 24707072
jvm.loadedClasses 3002
jvm.thread 21
tomcat.requestCount 27
tomcat.requestCount 0
tomcat.errorCount 0
tomcat.errorCount 0
tomcat.processingTime 881
tomcat.processingTime 0
tomcat.bytesSent 135816
tomcat.bytesSent 0
tomcat.bytesReceived 0
tomcat.bytesReceived 0
application.activeSessions 0
```

## Auto-aliasing for Queries

Instead of specifying a `resultAlias` for every query the alias can automatically be generated by a user-provided `QueryAliasFactory`. For this, implement the `QueryAliasFactory` interface and add the class to your application's classpath. 

Settings (optional) will be read from the configuration file and passed to your `QueryAliasFactory` after construction.

Sample configuration:

```xml
<jmxtrans-agent>
    <queries>
        <!-- JVM -->
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="used" />
    </queries>

    <resultAliasFactory class="com.organisation.jmxtrans.DomainSpecificQueryAliasFactory">
        <settingKeyA>settingKeyA</settingKeyA>
    </resultAliasFactory>
</jmxtrans-agent>
```

# Relase Notes

See GitHub [milestones history](https://github.com/jmxtrans/jmxtrans-agent/issues/milestones?state=closed)

# Sample ActiveMQ Configuration

* Create directory `${ACTIVEMQ_HOME}/jmxtrans-agent/`
* Copy `jmxtrans-agent-1.0.6.jar` under `${ACTIVEMQ_HOME}/jmxtrans-agent/`
* Update `${ACTIVEMQ_HOME}/bin/activemq`, add in `invoke_start()` and `invoke_console()`:
    ```
JMXTRANS_AGENT="-javaagent:${ACTIVEMQ_HOME}/jmxtrans-agent/jmxtrans-agent-1.0.6.jar=${ACTIVEMQ_HOME}/jmxtrans-agent/jmxtrans-agent-activemq.xml"
ACTIVEMQ_OPTS="$ACTIVEMQ_OPTS $JMXTRANS_AGENT"
```
* Copy to `${ACTIVEMQ_HOME}/jmxtrans-agent/` a config file similar to
    ```xml
<jmxtrans-agent>
    <queries>
        <!-- OS -->
        <query objectName="java.lang:type=OperatingSystem" attribute="SystemLoadAverage"
               resultAlias="os.systemLoadAverage"/>

        <!-- JVM -->
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="used"
               resultAlias="jvm.heapMemoryUsage.used"/>
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" key="committed"
               resultAlias="jvm.heapMemoryUsage.committed"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage" key="used"
               resultAlias="jvm.nonHeapMemoryUsage.used"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage" key="committed"
               resultAlias="jvm.nonHeapMemoryUsage.committed"/>
        <query objectName="java.lang:type=ClassLoading" attribute="LoadedClassCount" resultAlias="jvm.loadedClasses"/>

        <query objectName="java.lang:type=Threading" attribute="ThreadCount" resultAlias="jvm.thread"/>

        <!-- ACTIVE MQ -->
        <query objectName="org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"
               attribute="QueueSize" resultAlias="activemq.%brokerName%.queue.%destinationName%.QueueSize"/>
        <query objectName="org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"
               attribute="EnqueueCount" resultAlias="activemq.%brokerName%.queue.%destinationName%.EnqueueCount"/>
        <query objectName="org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"
               attribute="ExpiredCount" resultAlias="activemq.%brokerName%.queue.%destinationName%.ExpiredCount"/>
        <query objectName="org.apache.activemq:type=Broker,brokerName=*,destinationType=Queue,destinationName=*"
               attribute="DequeueCount" resultAlias="activemq.%brokerName%.queue.%destinationName%.DequeueCount"/>

        <query objectName="org.apache.activemq:type=Broker,brokerName=*,destinationType=Topic,destinationName=*"
               attribute="EnqueueCount" resultAlias="activemq.%brokerName%.topic.%destinationName%.EnqueueCount"/>
    </queries>
    <outputWriter class="org.jmxtrans.agent.GraphitePlainTextTcpOutputWriter">
        <host>localhost</host>
        <port>2203</port>
    </outputWriter>
    <outputWriter class="org.jmxtrans.agent.ConsoleOutputWriter">
        <enabled>false</enabled>
    </outputWriter>
</jmxtrans-agent>
```

# Release Notes

See https://github.com/jmxtrans/jmxtrans-agent/releases
