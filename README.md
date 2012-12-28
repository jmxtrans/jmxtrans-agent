[![Build Status](https://buildhive.cloudbees.com/job/cyrille-leclerc/job/jmxexporter/badge/icon)](https://buildhive.cloudbees.com/job/cyrille-leclerc/job/jmxexporter/)

jmxexporter
===========

In process JMX metrics exporter. Inspired by JMXTrans but embedded inside your java process (e.g. Tomcat).

An in process JMX Exporter will solve the problem of remote JMX access in cloud-style and elastic environments where the IP address of the Java servers is unknown and where RMI-IIOP is disabled (e.g. Amazon Elastic Beanstalk, Cloudbees, ...).


* [Documentation](https://github.com/cyrille-leclerc/jmxexporter/wiki)
* [Latest javadoc](http://cyrille-leclerc.github.com/jmxexporter/apidocs/)
