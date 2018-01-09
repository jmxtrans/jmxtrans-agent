/*
 * Copyright (c) 2010-2016 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.jmxtrans.agent.zabbix;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.jmxtrans.agent.AbstractOutputWriter;
import org.junit.Rule;
import org.junit.Test;

/**
 * ZabbixTcpOutputWriterTest
 * 
 * @author Steve McDuff
 */
public class ZabbixTcpOutputWriterTest
{
    public ZabbixTcpOutputWriterTest()
    {
        super();
    }

    @Rule
    public TcpByteLineServer tcpByteServer = new TcpByteLineServer();

    @Test
    public void reconnectsAfterServerClosesConnection() throws Exception
    {
        ZabbixTcpOutputWriter zabbixWriter = new ZabbixTcpOutputWriter();
        Map<String, String> config = new HashMap<>();
        config.put(ZabbixOutputWriterCommonSettings.SETTING_HOST, "127.0.0.1");
        config.put(ZabbixOutputWriterCommonSettings.SETTING_PORT, "" + tcpByteServer.getPort());
        config.put(ZabbixOutputWriterCommonSettings.SETTING_SERVER_NAME, "jmxtransagenttest");
        zabbixWriter.postConstruct(config);
        // Write one metric to see it is received
        writeTestMetric(zabbixWriter);
        assertEventuallyReceived(tcpByteServer, hasSize(1));
        writeTestMetric(zabbixWriter);
        // Write one metric and verify that it is received
        writeTestMetric(zabbixWriter);
        assertEventuallyReceived(tcpByteServer, hasSize(greaterThan(1)));

        tcpByteServer.stop();
    }

    public int value = 1;

    public void switchValue()
    {
        if (value < 10)
        {
            value += 1;
        }
        else
        {
            value = 1;
        }
    }

    private void writeTestMetric(AbstractOutputWriter writer)
    {
        switchValue();
        try
        {
            writer.writeQueryResult("jmxtransagentinputtest", null, value);
            writer.writeQueryResult("second", null, value + 20);

            tcpByteServer.readResponse = "ZBXA100000000{\"result\":\"success\"}".getBytes(StandardCharsets.UTF_8);

            writer.postCollect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void assertEventuallyReceived(TcpByteLineServer server, Matcher<Collection<? extends Object>> matcher)
        throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            if (matcher.matches(server.getReceivedLines()))
            {
                return;
            }
            Thread.sleep(10);
        }
        assertThat(server.getReceivedLines(), matcher);
    }

}
