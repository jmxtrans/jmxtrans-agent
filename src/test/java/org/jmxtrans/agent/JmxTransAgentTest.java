package org.jmxtrans.agent;

import org.junit.Test;

/**
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 */
public class JmxTransAgentTest {

    @Test
    public void testDumpDiagnosticInfo() throws Exception {
        JmxTransAgent.DIAGNOSTIC = true;
        JmxTransAgent.dumpDiagnosticInfo();

        Thread.sleep(1000);
    }
}
