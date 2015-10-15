


# How to use this test tomcat instance

1. build the jmxtrans-agent jar file with `mvn package`
1. open a shell command
1. declare the environment variable `CATALINA_HOME=/path/to/tomcat8` (typically creating a file `.envrc` under `./bin`
1. run `./catalina.sh run` from the `./bin` directory

You are all set, verify that your jmxtrans agent is running properly
   
   
   