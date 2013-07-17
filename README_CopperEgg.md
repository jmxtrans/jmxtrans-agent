# CopperEgg Fork of embedded-jmxtrans

In process JMX metrics exporter. Inspired by the standalone version of jmxtrans but embedded inside your java process (e.g. Tomcat).

An in process JMX Exporter will solve the problem of remote JMX access in cloud-style and elastic environments where the IP address of the Java servers is unknown and where RMI-IIOP is disabled (e.g. Amazon Elastic Beanstalk, Cloudbees, ...).

This is the CopperEgg fork of the embedded-jmxtrans GitHub repo. This is a temporary fork, which has been created to build the CopperEggWriter.
It will hopefully be rolled into the next release of embedded-jmxtrans. 

Please refer to the documentation provided in the README.md of this directory. It clearly describes how to integrate the embedded-jmxtrans module with
a java servlet, or a spring-enabled webapp.

* [Documentation](https://github.com/jmxtrans/embedded-jmxtrans/wiki)
* [Latest javadocs](http://jmxtrans.github.com/embedded-jmxtrans/apidocs/)
* [Sample](https://github.com/sjohnsoncopperegg/embedded-jmxtrans)
* [CopperEgg Sample](https://github.com/sjohnsoncopperegg/Sample-copperegg-jmxmon)


## Instructions for enabling the CopperEggWriter


### Clone and build the CopperEgg fork of embedded-jmxtrans :

```xml
git clone https://github.com/sjohnsoncopperegg/embedded-jmxtrans.git
cd embedded-jmxtrans
mvn install dependency:go-offline
```
The embedded-jmxtrans module is now built on your machine.


### Edit jmxtrans.json

```xml
Locate the jmxtrans.json file you are using to to integrate jmxtrans with your webapp.
In the "OutputWriters" array, add the following block to enable the CopperEggWriter:
    {
        "@class": "org.jmxtrans.embedded.output.CopperEggWriter",
        "settings": {
            "source": "${CopperEgg.source:#hostname#}",
            "username": "${CopperEgg.username:<YOUR_USER_NAME>}",
            "token": "${CopperEgg.token:<YOUR_APIKEY>}",
            "enabled": "${CopperEgg.enabled:true}"
        }
    }
Now add your username and CopperEgg APIKey:
    Replace  <YOURUSERNAMEHERE>  with your user name
    Replace  <YOURAPIKEY> with your CopperEgg APIKEY
Save and close jmxtrans.json
```

### Create a copperegg_config.json file
This file is used to define the metric groups that jmxtrans will monitor, as well as define two dashboards where the monitored metrics will be displayed.
If you are just starting with CopperEgg monitoring, just copy the copperegg_default_config.json to copperegg_config.json. copperegg_config.json should 
reside in the same directory as your jmxtrans.json file.

The copperegg_default_config.json file can be found here:
   https://github.com/sjohnsoncopperegg/embedded-jmxtrans-samples/blob/master/embedded-jmxtrans-webapp-coktail/src/main/resources/copperegg_default_config.json 


### Build your WebApp with integrated jmxtrans as described in the accopanying embedded-jmxtrans README.md.


## Notes

When you build embedded-jmxtrans from the CopperEgg fork, it will build jmxtrans-1.0.9-SNAPSHOT, not jmxtrans-1.0.8 (as the README.md indicates)

The same is true when you build the CopperEgg fork of the embedded-jmxtrans-sample Cocktail App Demo... you will build cocktail-app-1.0.9-snapshot.war.


To learn more about CopperEgg, and to sign up for a free trial: 
* [CopperEgg Homepage](http://www.copperegg.com)
* [CopperEgg Signup](https://app.copperegg.com/signup)
* [CopperEgg Login](https://app.copperegg.com/login)


License
==================

Please refer to the LICENSE and NOTIFICATION files included by the authors of embedded-jmxtrans and embedded-jmxtrans-samples.

CopperEgg has provided herein the CopperEggWriter, which is designed to be used in conjuction with embedded-jmxtrans.
CopperEggWriter.java code heavily leveraged the LibratoWriter.java code base.

CopperEggWriter is made available under the terms of the MIT License:

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without
limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons
to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.





