# Jmxtrans Agent

[![Travis](https://img.shields.io/travis/jmxtrans/jmxtrans-agent.svg)]()
[![GitHub issues](https://img.shields.io/github/issues/jmxtrans/jmxtrans-agent.svg)](https://github.com/jmxtrans/jmxtrans-agent/issues)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/jmxtrans/jmxtrans-agent/master/LICENSE)
[![GitHub forks](https://img.shields.io/github/forks/jmxtrans/jmxtrans-agent.svg)](https://github.com/jmxtrans/jmxtrans-agent/network)
[![GitHub stars](https://img.shields.io/github/stars/jmxtrans/jmxtrans-agent.svg)](https://github.com/jmxtrans/jmxtrans-agent/stargazers)


## What is jmxtrans-agent ?

jmxtrans-agent is a version of [jmxtrans](http://jmxtrans.org/) intended to be used as a java agent. JmxTrans Agent has zero dependencies to ease integration.

## Java Agent Declaration

Download the [latest release](https://github.com/jmxtrans/jmxtrans-agent/releases/latest) of `jmxtrans-agent-<version>.jar`

Sample `setenv.sh` for Apache Tomcat

```
export JAVA_OPTS="$JAVA_OPTS -javaagent:/path/to/jmxtrans-agent-1.2.4.jar=jmxtrans-agent.xml"
```

* java agent jar path can be relative to the working dir
* `jmxtrans-agent.xml` is the configuration file, can be classpath relative (`classpath:…`), http(s) (`http(s)://...`) or file system based (relative to the working dir)

### Delayed startup (version >= 1.2.1)

For some application servers like JBoss, delaying premain is needed to start the agent, see [WFLY-3054](https://issues.jboss.org/browse/WFLY-3054)
This has been confirmed to be needed with JBoss 5.x, 6.x, 7.x and Wildfly 8.x. This is because a
custom MBeanServer is used by programmatically setting the ["javax.management.builder.initial"
system property](https://docs.oracle.com/javase/9/docs/api/javax/management/MBeanServerFactory.html)
in JBoss's startup sequence. If the `PlatformMBeanServer` is initialized before this is set, the
`PlatformMBeanServer` will not use the implementation JBoss expects.

To wait for the custom MBeanServer to be defined (version >= 1.2.8):

```
# delays calling premain() in jmxtrans agent until javax.management.builder.initial is set up to 2 minutes
java -Djmxtrans.agent.premain.waitForCustomMBeanServer=true
```

You can optionally increase the timeout to wait if necessary (defaults to 2 minutes):

```
# delays calling premain() in jmxtrans agent until javax.management.builder.initial is set up to 5 minutes
java -Djmxtrans.agent.premain.waitForCustomMBeanServer=true -Djmxtrans.agent.premain.waitForCustomMBeanServer.timeoutInSeconds=300
```

For versions <1.2.8, you have to add a flat delay. To add a flat delay set `jmxtrans.agent.premain.delay` (value in seconds):

```
# delays calling premain() in jmxtrans agent for 30 seconds
java -Djmxtrans.agent.premain.delay=30
```

## Query configuration

### Selecting attributes

To select which attributes to collect, use the `attribute` or `attributes` attribute on the `query` element. `attribute` accepts a
single attribute while `attributes` accepts a comma-separated list of attributes to collect. If you do not specify any attributes, all attributes
of the MBean will be dynamically discovered and collected. Use the [expression language](https://github.com/jmxtrans/jmxtrans-agent/wiki/Expression-Language) `#attribute#` in the `resultAlias`
to use the attribute name in the metric name when collecting many attributes.

Example - collect the `ThreadCount` attribute from the Threading MBean:

```xml
<query objectName="java.lang:type=Threading" attribute="ThreadCount"
   resultAlias="jvm.thread.count"/>
```

Example - collect the `SystemLoadAverage` gauge attribute from the OperatingSystem MBean:

<query objectName="java.lang:type=OperatingSystem" attributes="SystemLoadAverage" type="gauge" resultAlias="#attribute#"/>

(i) Note that the `type` attribute is customizable. Output writers such as the [LibratoWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/jmxtrans-agent-1.2.4/src/main/java/org/jmxtrans/agent/LibratoWriter.java), [StatsDOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/jmxtrans-agent-1.2.4/src/main/java/org/jmxtrans/agent/StatsDOutputWriter.java) and [PerMinuteSummarizerOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/jmxtrans-agent-1.2.4/src/main/java/org/jmxtrans/agent/PerMinuteSummarizerOutputWriter.java) are aware of the `type`s `counter` and `gauge` and assume that non defined `type`means `counter`.

Example - collect `ThreadCount` and `TotalStartedThreadCount` from the Threading MBean:

```xml
<query objectName="java.lang:type=Threading" attributes="ThreadCount,TotalStartedThreadCount"
  resultAlias="jvm.threads.#attribute#"/>
```

Example - collect all attributes from the Threading MBean:

```xml
<query objectName="java.lang:type=Threading" resultAlias="jvm.threads.#attribute#"/>
```


### Simple mono-valued attribute

Use `attribute` or `attributes` to specify the values to lookup. No additional configuration is required.
See `javax.management.MBeanServer.getAttribute(objectName, attribute)`.

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

* You can collect all the keys of the composite data omitting `key` in the `<query />` declaration.
* Use the [expression language](https://github.com/jmxtrans/jmxtrans-agent/wiki/Expression-Language) `#key#` (or its synonym `#compositeDataKey#`) in the `resultAlias` to use the composite data key in the metric name. Sample:

```xml
 <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage" resultAlias="jvm.heapMemoryUsage.#key#"/>
```

### Multi-valued data (Iterable or array)

Use `position` to specify the value to lookup. Position is `0 based.

```xml
 <query objectName="MyApp:type=MyMBean" attribute="MyMultiValuedAttribute" position="2"
    resultAlias="myMBean.myMultiValuedAttributeValue"/>
```

* `position` is 0 based
* You can collect all the entries of the multi-valued data omitting `position` in the `<query />` declaration.
* Use the [expression language](https://github.com/jmxtrans/jmxtrans-agent/wiki/Expression-Language) `#position#` in the `resultAlias` to use the multi-valued data position in the metric name. Sample:

       ```xml
 <query objectName="MyApp:type=MyMBean" attribute="MyMultiValuedAttribute" resultAlias="myMBean.myMultiValuedAttributeValue.#position#"/>
```

* If no `resultAlias` is specified, the generated metric name is suffixed by `_#position#`. Sample:

       ```
myMBean.myMultiValuedAttributeValue_0`
```

## Additional Configuration

### Dynamic configuration reloading
The configuration can be dynamically reloaded at runtime. To enable this feature, enable it with the `reloadConfigurationCheckIntervalInSeconds` element, e.g.:

```xml
<reloadConfigurationCheckIntervalInSeconds>60</reloadConfigurationCheckIntervalInSeconds>
```

### Collection interval

The interval for collecting data using the specified queries and invocations can be specified with the element `collectIntervalInSeconds`, e.g.:

```xml
<collectIntervalInSeconds>20</collectIntervalInSeconds>
```

The collect interval can be overridden for a specific query or invocation by setting the `collectIntervalInSeconds` attribute, e.g.:

```xml
<query objectName="java.lang:type=Threading" attributes="ThreadCount,TotalStartedThreadCount"
   resultAlias="jvm.threads.#attribute#" collectIntervalInSeconds="5"/>
```

### ResultNameStrategy

The `ResultNameStrategy` is the component in charge of building the metric name. The default implementation uses the `resultAlias`  if provided
and otherwise will build the metric name using the `ObjectName`.

You can use your own implementation for the `ResultNameStrategy`

```xml
<resultNameStrategy class="com.mycompany.jmxtrans.agent.MyResultNameStrategyImpl">
   <attrA>valA</attrA>
   <attrB>valB</attrB>
</resultNameStrategy>
```

You then have to make this implementation available in the classpath (adding it the the jmxtrans-agent jar, adding it to the boot classpath ...)

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

**Note** why XML and not JSON ? because XML parsing is out of the box in the JVM when JSON requires additional libraries.



## OutputWriters

OutputWriters are very simple to develop, you just have to extend [AbstractOutputWriter.java](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/AbstractOutputWriter.java) or to implement [OutputWriter.java](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/OutputWriter.java).

Out of the box output writers:

* [GraphitePlainTextTcpOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/GraphitePlainTextTcpOutputWriter.java): output to Graphite Carbon plain text protocol on TCP. Configuration parameters:
  * `enabled`: to enable/disable the output writer. Optional, default value `true`
  * `host`: Graphite Carbon listener host
  * `port`: Graphite Carbon Plain Text TCP listener port. Optional, default value `2003`
  * `namePrefix`; prefix of the metric name. Optional, default values `servers.#hostname#.` where `#hostname#` is the auto discovered hostname of computer with `.` escaped as `_` (`InetAddress.getLocalHost().getHostName()`).
* [GraphiteUdpOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/GraphiteUdpOutputWriter.java): output to Graphite Carbon plain text protocol on UDP. Supports the same configuration parameters as the GraphitePlainTextTcpOutputWriter
* [FileOverwriterOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/FileOverwriterOutputWriter.java): store the last collection of metrics in a file. Configuration parameters:
  * `fileName`: name of the file in which the collected metrics are stored. Optional, default value `jmxtrans-agent.data` (in JVM working dir, for example `$TOMCAT_HOME/bin`)
  * `showTimeStamp`: true or false value that determines if the time stamp is printed with the lines.  Optional tag, default is `false.
* [SummarizingFileOverwriterOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/SummarizingFileOverwriterOutputWriter.java): Similar to the `FileOverwriterOutputWriter` but displays "per minute" values for counters of type `counter`
* [ConsoleOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/ConsoleOutputWriter.java): output metric values to `stdout`
* [SummarizingConsoleOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/SummarizingConsoleOutputWriter.java): Similar to the `ConsoleOutputWriter` but displays "per minute" values for counters of type `counter`
* [RollingFileOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/RollingFileOutputWriter.java)
  * `fileName`: Name of the file in which the collected metrics are stored. Optional, default value `jmxtrans-agent.data` (in JVM working dir, for example `$TOMCAT_HOME/bin`)
  * `maxFileSize`: Maximum file size in MB before file is rolled. Optional, default is `10`
  * `maxBackupIndex`: Maximum number of backup files. Optional, default is `5
  * `singleLine`: true or false value that determines if all values are printed on a single line. Optional, default is false 
* [StatsDOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/StatsDOutputWriter.java): output to StatD using the counter metric type. Configuration parameters:
  * `host`: StatsD listener host
  * `port`: StatsD listener port
  * `statsd` : Optional StatsD server type, statsd, dd or sysdig
  * `tags` : Optional StatsD tags for dd and sysdig, i.e. serviceid:SERVICE_ID,environment:dev
  * `metricName`: metric name prefix. Optional, default value is machine hostname or IP (all `.` are scaped as `_`).
  * `bufferSize`: max buffer size. Holds data to be sent. Optional, default value is 1024.
* [InfluxDbOutputWriter](https://github.com/jmxtrans/jmxtrans-agent/blob/master/src/main/java/org/jmxtrans/agent/influxdb/InfluxDbOutputWriter.java): output to InfluxDb. **This writer is currently experimental** - behavior and options might change. See [InfluxDbOutputWriter Details](#influxdboutputwriter-details) for more details. Configuration parameters:
  * `url`: url to the influxdb server, e.g. `<url>http://influx.company.com:8086</url>` - required
  * `database`: name of the database to write to - required
  * `user`: username for authentication - optional
  * `password`: password for authentication - optional
  * `tags`: additional tags to use for all metrics on `n1=v1,n2=v2` format, e.g. `<tags>#hostname#</tags>` - optional
  * `retentionPolicy`: retention policy to use - optional
  * `connectTimeoutMillis`: connect timeout for the HTTP connection to influx - optional, defaults to 3000
  * `readTimeoutMillis`: read timeout for the HTTP connection to influx - optional, defaults to 5000


Output writers configuration support an [expression language](https://github.com/jmxtrans/jmxtrans-agent/wiki/Expression-Language) based on property placeholders with the `{prop-name[:default-value]}` syntax (e.g. "`${graphite.port:2003}`").

The `default-value` is optional. An exception is raised if no default value is defined and the property placeholder is not found.

Environment variables are looked-up in the following order:

1. JVM system properties (```System.getProperty("graphite.host")```)
1. JVM environment variables (```System.getenv("graphite.host")```)
1. JVM environment variables after a "to-upper-case + dot-to-underscore" transformation (```System.getenv("GRAPHITE_HOST")```)

### InfluxDbOutputWriter Details

**This writer is currently in beta, it might have bugs and the behavior and options might change**.

When using the `InfluxDbOutputWriter`, the queries' `resultAlias` have special semantics. The result alias is a comma-separated list where the first
item is the name of the measurement and the rest of the items are tags to add to metrics collected by the query. For example, the query

```xml
<query objectName="java.lang:type=GarbageCollector,name=*"
	attributes="CollectionTime,CollectionCount"
	resultAlias="#attribute#,garbageCollector=%name%,myTag=foo" />
```

will result in measurements named as the attributes collected (`CollectionTime` and `CollectionCount`).
The additional tag `garbageCollector` will be added which will correspond to the
name attribute of the object name. In addition, a tag called `myTag` with value `foo` will be added.

All measurements sent to InfluxDb will have only one field called `value`. Multiple fields are currently not supported.

Example complete output writer configuration:
```xml
<outputWriter class="org.jmxtrans.agent.influxdb.InfluxDbOutputWriter">
	<url>http://localhost:8086</url>
	<database>mydb</database>
	<user>admin</user>
	<password>shadow</password>
	<tags>host=#hostname#</tags>
</outputWriter>
```


### Sample of ConsoleOutputWriter

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

### Sample of FileOverwriterOutputWriter

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


# Release Notes

* [Milestones history](https://github.com/jmxtrans/jmxtrans-agent/milestones?state=closed)
* [Releases](https://github.com/jmxtrans/jmxtrans-agent/releases)

# Sample ActiveMQ Configuration

* Create directory `${ACTIVEMQ_HOME}/jmxtrans-agent/`
* Copy `jmxtrans-agent-1.2.4.jar` under `${ACTIVEMQ_HOME}/jmxtrans-agent/`
* Update `${ACTIVEMQ_HOME}/bin/activemq`, add in `invoke_start()` and `invoke_console()`:
    ```
JMXTRANS_AGENT="-javaagent:${ACTIVEMQ_HOME}/jmxtrans-agent/jmxtrans-agent-1.2.4.jar=${ACTIVEMQ_HOME}/jmxtrans-agent/jmxtrans-agent-activemq.xml"
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
    <outputWriter class="org.jmxtrans.agent.RollingFileOutputWriter">
      <fileName>rollingJMXOutputFile</fileName>
      <maxFileSize>10</maxFileSize>
      <maxBackupIndex>4</maxBackupIndex>
   </outputWriter>
</jmxtrans-agent>
```

# Release Notes

See https://github.com/jmxtrans/jmxtrans-agent/releases
