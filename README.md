[![Build Status](https://buildhive.cloudbees.com/job/jmxtrans/job/embedded-jmxtrans/badge/icon)](https://buildhive.cloudbees.com/job/jmxtrans/job/embedded-jmxtrans/)

# embedded-jmxtrans

In process JMX metrics exporter. Inspired by the standalone version of jmxtrans but embedded inside your java process (e.g. Tomcat).

An in process JMX Exporter will solve the problem of remote JMX access in cloud-style and elastic environments where the IP address of the Java servers is unknown and where RMI-IIOP is disabled (e.g. Amazon Elastic Beanstalk, Cloudbees, ...).


* [Documentation](https://github.com/jmxtrans/embedded-jmxtrans/wiki)
* [Google Group](https://groups.google.com/forum/#!forum/jmxtrans) if you have anything to discuss
* [Latest javadocs](http://jmxtrans.github.com/embedded-jmxtrans/apidocs/)
* [Sample](https://github.com/jmxtrans/embedded-jmxtrans-samples)

## Getting started

Getting started guide for Spring Framework enabled web applications.

### Maven

Add `embedded-jmxtrans` dependency

```xml
<dependency>
    <groupId>org.jmxtrans.embedded</groupId>
    <artifactId>embedded-jmxtrans</artifactId>
    <version>1.0.11</version>
</dependency>
```

### Spring Framework

Declare `<jmxtrans:jmxtrans>` in your Spring configuration :
```xml
<beans ...
       xmlns:jmxtrans="http://www.jmxtrans.org/schema/embedded"
       xsi:schemaLocation="...
		http://www.jmxtrans.org/schema/embedded http://www.jmxtrans.org/schema/embedded/jmxtrans-1.0.xsd">

    <jmxtrans:jmxtrans>
        <jmxtrans:configuration>classpath:jmxtrans.json</jmxtrans:configuration>
        <jmxtrans:configuration>classpath:org/jmxtrans/embedded/config/tomcat-6.json</jmxtrans:configuration>
        <jmxtrans:configuration>classpath:org/jmxtrans/embedded/config/jmxtrans-internals.json</jmxtrans:configuration>
        <jmxtrans:configuration>classpath:org/jmxtrans/embedded/config/jvm-sun-hotspot.json</jmxtrans:configuration>
    </jmxtrans:jmxtrans>
</beans>
```

### Configure writers

Create `src/main/resources/jmxtrans.json`, add your mbeans and declare both `ConsoleWriter` (output to `stdout`) and `GraphiteWriter`

```json
{
  "queries": [
      {
      "objectName": "cocktail:type=ShoppingCartController,name=ShoppingCartController",
      "resultAlias": "",
      "attributes": [
        {
          "name": "SalesRevenueInCentsCounter",
          "resultAlias": "sales.revenueInCentsCounter"
        }
      ]
    },
    {
      "objectName": "com.cocktail:type=CocktailService,name=cocktailService",
      "resultAlias": "cocktail.controller",
      "attributes": ["SearchedCocktailCount", "DisplayedCocktailCount", "SendCocktailRecipeCount"]
    }
  ],
  "outputWriters": [
    {
      "@class": "org.jmxtrans.embedded.output.ConsoleWriter"
    },
    {
      "@class": "org.jmxtrans.embedded.output.GraphiteWriter",
      "settings": {
        "host": "${graphite.host:localhost}",
        "port": "${graphite.port:2003}"
      }
    }
  ]
}
```

In this sample, Graphite host & port are defaulted to `localhost:2003` and can be overwritten with system properties or environment variables, for example in `$CATALINA_BASE/conf/catalina.properties`.

### Start application and check metrics

#### Check metrics in the Console

```
...
jvm.os.SystemLoadAverage 2.97265625 1358242428
tomcat.thread-pool.http-8080.currentThreadsBusy 0 1358242458
tomcat.manager.localhost._.activeSessions 0 1358242458
tomcat.servlet.__localhost_.jsp.processingTime 0 1358242458
tomcat.servlet.__localhost_.jsp.errorCount 0 1358242458
tomcat.servlet.__localhost_.jsp.requestCount 0 1358242458
cocktail.controller.SearchedCocktailCount 12 1358242458
...
```

#### Check metrics in Graphite

![Graphite Screenshot](https://raw.github.com/wiki/jmxtrans/embedded-jmxtrans/img/graphite-screenshot-basic.png)



