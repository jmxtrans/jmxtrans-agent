package org.jmxtrans.agent;

import com.sun.tools.attach.VirtualMachine;
import org.jmxtrans.agent.util.logging.Logger;

public class DynamicallyAgentAttacher {
    private static Logger logger = Logger.getLogger(DynamicallyAgentAttacher.class.getName());

    public static void main(String[] args) {
        if(args.length != 3) {
            printUsage();
            System.exit(1);
        }

        String agentJarPath = args[0];
        String targetJvmPid = args[1];
        String configurationFile = args[2];
        logger.info("Dynamically loading jmxtrans-agent");
        logger.info("Agent path: " + agentJarPath);
        logger.info("Target JVM PID: " + targetJvmPid);
        logger.info("XML configuration file: " + configurationFile);

        try {
            VirtualMachine vm = VirtualMachine.attach(targetJvmPid);
            vm.loadAgent(agentJarPath, configurationFile);
            vm.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar jmxtrans-agent.jar <agent-absolute-path> <target-jvm-pid> " +
                "<xml-configuration-file>");
        System.err.println("Dynamically attaches jmxtrans-agent to a running JVM");
        System.err.println("");
        System.err.println("Mandatory arguments:");
        System.err.println(" agent-absolute-path      Absolute path to jmxtrans-agent JAR file");
        System.err.println(" target-jvm-pid           Target JVM PID on which the agent will be attached");
        System.err.println(" xml-configuration-file   Absolute path to agent XML configuration file");
    }
}
