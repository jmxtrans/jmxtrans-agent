[![Build Status](https://buildhive.cloudbees.com/job/jmxtrans/job/embedded-jmxtrans/badge/icon)](https://buildhive.cloudbees.com/job/jmxtrans/job/embedded-jmxtrans/)

# Embedded jmxtrans

In process JMX metrics exporter. Inspired by JMXTrans but embedded inside your java process (e.g. Tomcat).

An in process JMX Exporter will solve the problem of remote JMX access in cloud-style and elastic environments where the IP address of the Java servers is unknown and where RMI-IIOP is disabled (e.g. Amazon Elastic Beanstalk, Cloudbees, ...).


* [Documentation](https://github.com/jmxtrans/embedded-jmxtrans/wiki)
* [Latest javadocs](http://jmxtrans.github.com/embedded-jmxtrans/apidocs/)
* [Sample](https://github.com/jmxtrans/embedded-jmxtrans-samples)
