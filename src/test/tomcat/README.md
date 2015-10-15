


# How to use this test tomcat instance

1. Build the jmxtrans-agent jar file with `mvn package`
1. Open a shell command
1. Declare the environment variable `CATALINA_HOME=/path/to/tomcat8` and `CATALINA_BASE="$(pwd)/.."` (typically creating a file `.envrc` under `./bin`)
1. Run `./catalina.sh run` from the `./bin` directory

You are all set, verify that your jmxtrans agent is running properly
   
   
   