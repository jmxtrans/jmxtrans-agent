/*
 * Copyright (c) 2010-2013 the original author or authors
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
package org.jmxtrans.embedded.output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Comparator;

import com.fasterxml.jackson.core.Base64Variants;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import org.jmxtrans.embedded.EmbeddedJmxTransException;
import org.jmxtrans.embedded.QueryResult;
import org.jmxtrans.embedded.util.io.IoUtils2;
import org.jmxtrans.embedded.util.StringUtils2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import java.lang.management.ManagementFactory;

/**
 * <a href="https://copperegg.com//">CopperEgg Metrics</a> implementation of the {@linkplain org.jmxtrans.embedded.output.OutputWriter}.
 * <p/>
 * This implementation uses v2 of the CopperEgg API <a href="http://dev.copperegg.com/">
 * <p/>
 * Settings:
 * <ul>
 * <li>"{@code url}": CopperEgg API server URL.
 * Optional, default value: {@value #DEFAULT_COPPEREGG_API_URL}.</li>
 * <li>"{@code user}": CopperEgg user. Mandatory</li>
 * <li>"{@code token}": CopperEgg APIKEY. Mandatory</li>
 * <li>"{@code coppereggApiTimeoutInMillis}": read timeout of the calls to CopperEgg HTTP API.
 * Optional, default value: {@value #DEFAULT_COPPEREGG_API_TIMEOUT_IN_MILLIS}.</li>
 * <li>"{@code enabled}": flag to enable/disable the writer. Optional, default value: <code>true</code>.</li>
 * <li>"{@code source}": CopperEgg . Optional, default value: {@value #DEFAULT_SOURCE} (the hostname of the server).</li>
 * </ul>
 * LibratoWriter.java author:
 * @author <a href="mailto:cleclerc@cloudbees.com">Cyrille Le Clerc</a>
 *
 * CopperEggWriter.java was derived from LibratoWriter.java 
 */
public class CopperEggWriter extends AbstractOutputWriter implements OutputWriter {
    public static final String METRIC_TYPE_GAUGE = "gauge";
    public static final String METRIC_TYPE_COUNTER = "counter";
    public static final String DEFAULT_COPPEREGG_API_URL = "https://api.copperegg.com/v2/revealmetrics";
    public static final String SETTING_COPPEREGG_API_TIMEOUT_IN_MILLIS = "coppereggApiTimeoutInMillis";
    public static final int DEFAULT_COPPEREGG_API_TIMEOUT_IN_MILLIS = 10000;
    public static final String SETTING_SOURCE = "source";
    public static final String DEFAULT_SOURCE = "#hostname#";
    private final static String DEFAULT_COPPEREGG_CONFIGURATION_PATH = "classpath:copperegg_config.json";
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicInteger exceptionCounter = new AtomicInteger();
    private JsonFactory jsonFactory = new JsonFactory();


   /**
     * CopperEgg API URL
     */
    private URL url;
    private String url_str;
    private int coppereggApiTimeoutInMillis = DEFAULT_COPPEREGG_API_TIMEOUT_IN_MILLIS;
    private long myPID = 0;
    private String myhost;
    private String myPID_host;
    private String config_location;

    private static Map<String, String> dashMap = new HashMap<String, String>();
    private static Map<String, String> metricgroupMap = new HashMap<String, String>();

    private String jvm_os_groupID = null;
    private String jvm_gc_groupID = null;
    private String jvm_runtime_groupID = null;
    private String jvm_class_groupID = null;
    private String jvm_thread_groupID = null;
    private String heap_metric_groupID = null;
    private String nonheap_metric_groupID = null;
    private String tomcat_thread_pool_groupID = null;
    private String tomcat_grp_groupID = null;
    private String tomcat_manager_groupID = null;
    private String tomcat_servlet_groupID = null;
    private String tomcat_db_groupID = null;
    private String jmxtrans_metric_groupID = null;
    private String app_groupID = null;
    private String app_sales_groupID = null;

    /**
     * CopperEgg API authentication username
     */
    private String user;
    /**
     * CopperEgg APIKEY
     */
    private String token;
    private String basicAuthentication;
    /**
     * Optional proxy for the http API calls
     */
    @Nullable
    private Proxy proxy;
    /**
     * CopperEgg measurement property 'source',
     */
    @Nullable
    private String source;

    /**
     * Load settings<p/>
     */
    @Override
    public void start() {

        config_location = DEFAULT_COPPEREGG_CONFIGURATION_PATH;
        String path = config_location.substring("classpath:".length());
 
        long thisPID = getPID();
        if( myPID == thisPID) {
            logger.info("Started from two threads with the same PID, {}",thisPID);
            return;
        }
        myPID = thisPID;
        try {
            String str = String.valueOf(myPID);
            url_str = getStringSetting(SETTING_URL, DEFAULT_COPPEREGG_API_URL);
            url = new URL(url_str);
            user = getStringSetting(SETTING_USERNAME);
            token = getStringSetting(SETTING_TOKEN);
            user = token;
            basicAuthentication = Base64Variants.getDefaultVariant().encode((user + ":" + "U").getBytes(Charset.forName("US-ASCII")));

            if (getStringSetting(SETTING_PROXY_HOST, null) != null && !getStringSetting(SETTING_PROXY_HOST).isEmpty()) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getStringSetting(SETTING_PROXY_HOST), getIntSetting(SETTING_PROXY_PORT)));
            }

            coppereggApiTimeoutInMillis = getIntSetting(SETTING_COPPEREGG_API_TIMEOUT_IN_MILLIS, DEFAULT_COPPEREGG_API_TIMEOUT_IN_MILLIS);

            source = getStringSetting(SETTING_SOURCE, DEFAULT_SOURCE);
            source = getStrategy().resolveExpression(source);
            myhost = source;
            myPID_host = myhost + '.' + str;
            
            try{
                InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
                if(in == null) {
                    logger.warn("No file found for classpath:" + config_location);
                } else {
                  read_config(in);
                }
            } catch (Exception e){
                exceptionCounter.incrementAndGet();
                logger.warn("Exception in start " + e);
            }

            ensure_metric_groups();
            ensure_dashboards();


            logger.info("jvm_os_groupID : {}", jvm_os_groupID);
            logger.info("jvm_gc_groupID : {}", jvm_gc_groupID);
            logger.info("jvm_runtime_groupID : {}", jvm_runtime_groupID);
            logger.info("jvm_class_groupID : {}", jvm_class_groupID);
            logger.info("jvm_thread_groupID : {}", jvm_thread_groupID);
            logger.info("heap_metric_groupID : {}", heap_metric_groupID);
            logger.info("nonheap_metric_groupID : {}", nonheap_metric_groupID ); 
            logger.info("tomcat_thread_pool_groupID : {}",tomcat_thread_pool_groupID);
            logger.info("tomcat_grp_groupID : {}",tomcat_grp_groupID);
            logger.info("tomcat_servlet_groupID : {}", tomcat_servlet_groupID );
            logger.info("tomcat_manager_groupID : {}",tomcat_manager_groupID );
            logger.info("tomcat_db_groupID  : {}", tomcat_db_groupID);       
            logger.info("jmxtrans_metric_groupID : {}", jmxtrans_metric_groupID);
            logger.info("app_groupID : {}", app_groupID );
            logger.info("app_sales_groupID : {}", app_sales_groupID );
 


            logger.info("Started CopperEggWriter Successfully on jvm '{}', connected to '{}', proxy {}", myPID_host, url, proxy);
        } catch (MalformedURLException e) {
            exceptionCounter.incrementAndGet();
            throw new EmbeddedJmxTransException(e);
        }
    }

    /**
     * Export collected metrics to CopperEgg
     */
    @Override
    public void write(Iterable<QueryResult> results) {

        List<QueryResult> jvm_os_counters = new ArrayList<QueryResult>();
        List<QueryResult> jvm_gc_counters = new ArrayList<QueryResult>();
        List<QueryResult> jvm_runtime_counters = new ArrayList<QueryResult>();
        List<QueryResult> jvm_class_counters = new ArrayList<QueryResult>();
        List<QueryResult> jvm_thread_counters = new ArrayList<QueryResult>();
        List<QueryResult> heap_counters = new ArrayList<QueryResult>();
        List<QueryResult> nonheap_counters = new ArrayList<QueryResult>();
        List<QueryResult> tomcat_thread_pool_counters = new ArrayList<QueryResult>();
        List<QueryResult> tomcat_grp_counters = new ArrayList<QueryResult>();
        List<QueryResult> tomcat_manager_counters = new ArrayList<QueryResult>();
        List<QueryResult> tomcat_servlet_counters = new ArrayList<QueryResult>();
        List<QueryResult> tomcat_db_counters = new ArrayList<QueryResult>();
        List<QueryResult> jmxtrans_counters = new ArrayList<QueryResult>();
        List<QueryResult> app_counters = new ArrayList<QueryResult>();
        List<QueryResult> app_sales_counters = new ArrayList<QueryResult>();

        long epochInMillis = 0;
        String myname =  null;
        Object myval = null;
        long thisPID = 0;
        String tmp = null;
        String pidHost = null;

        String delims = "[.]";
        for (QueryResult result : results) {
            epochInMillis = result.getEpochInMillis();
            myname =  result.getName();
            myval = result.getValue();
            String valstr = myval.toString();
            thisPID = getPID();
            tmp = String.valueOf(thisPID);
            pidHost = source + "." + tmp;

            String[] parts = myname.split(delims);
            if( parts.length > 0 ) {
                String p1 = parts[0];
                if( (jmxtrans_metric_groupID != null) && (p1.equals("jmxtrans")) )  {
                    QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                    jmxtrans_counters.add(new_result);
                } else if( p1.equals("jvm") ) {
                    if( parts[1].equals("os")) {
                        if (parts[2].equals("OpenFileDescriptorCount")) {
                            QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                            jvm_os_counters.add(new_result);
                        } else if (parts[2].equals("CommittedVirtualMemorySize")){
                            float fval = Float.parseFloat(valstr);
                            try {
                                fval = fval/(1024.0f*1024.0f);  
                            } catch (Exception e) {
                                exceptionCounter.incrementAndGet();
                                logger.info("Exception doing Float: ", e);
                            }
                            QueryResult new_result = new QueryResult(myname, pidHost, fval, epochInMillis);
                            jvm_os_counters.add(new_result);
                        } else if (parts[2].equals("ProcessCpuTime")) { 
                            float fval = Float.parseFloat(valstr);
                            try {
                                fval = fval/(1000.0f*1000.0f*1000.0f);  
                            } catch (Exception e) {
                                exceptionCounter.incrementAndGet();
                                logger.warn("Exception doing Float: ", e);
                            }
                            QueryResult new_result = new QueryResult(myname, pidHost, fval, epochInMillis);
                            jvm_os_counters.add(new_result);
                        }
                    } else if( (parts[1].equals("runtime")) && (parts[2].equals("Uptime")) ) { 
                        float fval = Float.parseFloat(valstr);
                        try {
                            fval = fval/(1000.0f*60.0f);  
                        } catch (Exception e) {
                            exceptionCounter.incrementAndGet();
                            logger.warn("Exception doing Float: ", e);
                        }
                        QueryResult new_result = new QueryResult(myname, pidHost, fval, epochInMillis);
                        jvm_runtime_counters.add(new_result); 
                    } else if( (parts[1].equals("loadedClasses")) && (parts[2].equals("LoadedClassCount")) ) {
                        // jvm.loadedClasses.LoadedClassCount 5099 1374549969
                        QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                        jvm_class_counters.add(new_result); 
                    } else if( (parts[1].equals("thread")) && (parts[2].equals("ThreadCount")) ){
                        // jvm.thread.ThreadCount 13 1374549940
                        QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                        jvm_thread_counters.add(new_result);
                    } else if( (parts[1].equals("gc")) &&
                        ( (parts[2].equals("Copy")) ||  (parts[2].equals("MarkSweepCompact")) ) &&
                        ( (parts[3].equals("CollectionCount")) ||  (parts[3].equals("CollectionTime")) ) ) {
                        // jvm.gc.Copy.CollectionCount 68 1374549940
                        QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                        jvm_gc_counters.add(new_result);
                    } else if( parts[1].equals("memorypool") ){ 
                        if( ( (parts[2].equals("Perm_Gen")) || (parts[2].equals("Code_Cache")) ) && 
                            ( (parts[4].equals("committed")) || (parts[4].equals("used"))  ) ) {     
                            myname = "jvmNonHeapMemoryUsage";
                            String fullID = pidHost + "." + parts[2] + "." + parts[4];
                            float fval = Float.parseFloat(valstr);
                            try {
                                fval = fval/(1024.0f*1024.0f);
                            } catch (Exception e) {
                                exceptionCounter.incrementAndGet();
                                logger.warn("Exception doing Float: ", e);
                            }
                            QueryResult new_result = new QueryResult(myname, fullID, fval, epochInMillis);
                            nonheap_counters.add(new_result);
                        } else if( ( (parts[2].equals("Eden_Space")) || 
                                     (parts[2].equals("Survivor_Space")) || 
                                     (parts[2].equals("Tenured_Gen")) 
                                    ) && (  (parts[4].equals("committed")) ||  (parts[4].equals("used"))  ) ) {    
                            myname = "jvmHeapMemoryUsage";
                            String fullID = pidHost + "." + parts[2] + "." + parts[4];
                            float fval = Float.parseFloat(valstr);
                            try {
                                fval = fval/(1024.0f*1024.0f);
                            } catch (Exception e) {
                                exceptionCounter.incrementAndGet();
                                logger.warn("Exception doingFloat: ", e);
                            }
                            QueryResult new_result = new QueryResult(myname, fullID, fval, epochInMillis);
                            heap_counters.add(new_result);
                        }
                    } 
                } else if( p1.equals("tomcat") ) {
                    if( (parts[1].equals("thread-pool")) &&  
                         ( (parts[3].equals("currentThreadsBusy")) || (parts[3].equals("currentThreadCount")) ) ){
                        // tomcat.thread_pool.http-bio-8080.currentThreadCount 0 1374549955
                        String connector = parts[2];
                        myname = parts[0] + "." + parts[1] + "." + parts[3];
                        String fullID = pidHost + "." + connector;
                        QueryResult new_result = new QueryResult(myname, fullID, myval, epochInMillis);
                        tomcat_thread_pool_counters.add(new_result);
                   } else if( (parts[1].equals("global-request-processor")) ) {
                        // tomcat.global-request-processor.http-bio-8080.bytesSent
                        String connector = parts[2];
                        myname = parts[0] + "." + parts[1] + "." + parts[3];
                        String fullID = pidHost + "." + connector;
                        if( parts[3].equals("processingTime")) {
                            float fval = Float.parseFloat(valstr);
                            try {
                                fval = fval/(1024.0f);
                            } catch (Exception e) {
                                exceptionCounter.incrementAndGet();
                                logger.warn("Exception doingFloat: ", e);
                            }
                            QueryResult new_result = new QueryResult(myname, fullID, fval, epochInMillis);
                            tomcat_grp_counters.add(new_result);
                        } else {
                            QueryResult new_result = new QueryResult(myname, fullID, myval, epochInMillis);
                            tomcat_grp_counters.add(new_result);
                        }
                    } else if( (parts[1].equals("manager"))  && (parts[4].equals("activeSessions")) ){
                        //  tomcat.manager.localhost._docs.activeSessions 0 1374549955
                        String myhost = parts[2];
                        String mycontext = parts[3];
                        myname = parts[0] + "." + parts[1] + "." + parts[4];
                        String fullID = pidHost + "." + myhost + "." + mycontext;
                        QueryResult new_result = new QueryResult(myname, fullID, myval, epochInMillis);
                        tomcat_manager_counters.add(new_result);
                    } else if( (parts[1].equals("servlet")) && 
                          ( (parts[4].equals("processingTime")) || 
                            (parts[4].equals("errorCount")) || 
                            (parts[4].equals("requestCount")) ) ){
                        // tomcat.servlet.__localhost_cocktail-app-1_0_9-SNAPSHOT.spring-mvc.processingTime
                        String myWebmodule = parts[2];
                        String myServletname = parts[3];
                        myname = parts[0] + "." + parts[1] + "." + parts[4];
                        String fullID = pidHost + "." + myWebmodule + "." + myServletname;
                        QueryResult new_result = new QueryResult(myname, fullID, myval, epochInMillis);
                        tomcat_servlet_counters.add(new_result);
                    } else if( (tomcat_db_groupID != null) && (parts[1].equals("data-source")) ) {                   
                        String myhost = parts[2];
                        String mycontext = parts[3];
                        String mydbname = parts[4];
                        myname = parts[0] + "." + parts[1] + "." + parts[5];
                        String fullID = pidHost + "." + myhost + "." + mycontext + "." + mydbname;
                        QueryResult new_result = new QueryResult(myname, fullID, myval, epochInMillis);
                        tomcat_db_counters.add(new_result);
                    }
                } else if( (app_groupID != null) && (p1.equals("cocktail")) ) {
                    if( !(parts[1].equals("CreatedCocktailCount")) &&  !(parts[1].equals("UpdatedCocktailCount")) ) {
                        QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                        app_counters.add(new_result);
                    }
                } else if( ( (app_sales_groupID != null) && (p1.equals("sales")) ) &&
                      ( (parts[1].equals("ordersCounter")) || 
                        (parts[1].equals("itemsCounter")) || 
                        (parts[1].equals("revenueInCentsCounter")) ) ){
                        QueryResult new_result = new QueryResult(myname, pidHost, myval, epochInMillis);
                        app_sales_counters.add(new_result);
                }
            } else {
                logger.warn("parts return NULL!!!");
            }
        }
        if(jvm_os_counters.size() > 0) {
            sort_n_send(jvm_os_groupID, jvm_os_counters);
        }
        if(jvm_gc_counters.size() > 0) {
            sort_n_send(jvm_gc_groupID, jvm_gc_counters);
        }
        if(jvm_runtime_counters.size() > 0) {
            sort_n_send(jvm_runtime_groupID, jvm_runtime_counters);
        }
        if(jvm_class_counters.size() > 0) {
            sort_n_send(jvm_class_groupID, jvm_class_counters);
        }
        if(jvm_thread_counters.size() > 0) {
            sort_n_send(jvm_thread_groupID, jvm_thread_counters);
        }
        if(heap_counters.size() > 0) {
            sort_n_send(heap_metric_groupID, heap_counters);
        }
        if(nonheap_counters.size() > 0) {
            sort_n_send(nonheap_metric_groupID, nonheap_counters);
        }
        if(tomcat_thread_pool_counters.size() > 0) {
            sort_n_send(tomcat_thread_pool_groupID, tomcat_thread_pool_counters);
        }
        if(tomcat_grp_counters.size() > 0) {
            sort_n_send(tomcat_grp_groupID,tomcat_grp_counters);
        }
        if(tomcat_servlet_counters.size() > 0) {
            sort_n_send(tomcat_servlet_groupID, tomcat_servlet_counters);
        }
        if(tomcat_manager_counters.size() > 0) {
            sort_n_send(tomcat_manager_groupID, tomcat_manager_counters);
        }
        if(tomcat_db_counters.size() > 0) {
            sort_n_send(tomcat_db_groupID, tomcat_db_counters);
        }
        if(jmxtrans_counters.size() > 0) {
            sort_n_send(jmxtrans_metric_groupID, jmxtrans_counters);
        }
        if(app_counters.size() > 0) {
            sort_n_send(app_groupID, app_counters);
        }
        if(app_sales_counters.size() > 0) {
            sort_n_send(app_sales_groupID, app_sales_counters);
        }
    }
    public void sort_n_send(String mg_name, List<QueryResult> mg_counters) {
        Collections.sort(mg_counters, new Comparator<QueryResult>() {
            public int compare(QueryResult o1, QueryResult o2) {
                //Sorts by 'epochInMillis' property
                Integer rslt = o1.getEpochInMillis()<o2.getEpochInMillis()?-1:o1.getEpochInMillis()>o2.getEpochInMillis()?1:0;
                if(rslt == 0){
                    rslt = (o1.getType()).compareTo(o2.getType());
                }
                return rslt;
            }
        });
        send_metrics(mg_name, mg_counters);
    }
    public void send_metrics(String mg_name, List<QueryResult> counters) {
        long timeblock = counters.get(0).getEpoch(TimeUnit.SECONDS);
        String identifier = counters.get(0).getType();        
        int remaining = counters.size();
        List<QueryResult> sorted_ctrs = new ArrayList<QueryResult>();
   
       for (QueryResult counter : counters) {
            remaining = remaining - 1;
            if( (timeblock != (counter.getEpoch(TimeUnit.SECONDS))) || (!identifier.equals(counter.getType()) ) ) {
                one_set(mg_name, sorted_ctrs); 
                timeblock = counter.getEpoch(TimeUnit.SECONDS);
                identifier = counter.getType();  
                sorted_ctrs.clear();
                sorted_ctrs.add(counter);
            } else {
                sorted_ctrs.add(counter);
            }
            if( remaining == 0 ) {
                one_set(mg_name, sorted_ctrs); 
            }
        }
    }

    public void one_set(String mg_name, List<QueryResult> counters)  {
        HttpURLConnection urlCxn = null;
        URL newurl = null;
        try {
            newurl = new URL(url_str + "/samples/" + mg_name + ".json");
            if (proxy == null) {
                urlCxn = (HttpURLConnection) newurl.openConnection();
            } else {
                urlCxn = (HttpURLConnection) newurl.openConnection(proxy);
            }
            if (urlCxn != null) {
                urlCxn.setRequestMethod("POST");
                urlCxn.setDoInput(true);
                urlCxn.setDoOutput(true);
                urlCxn.setReadTimeout(coppereggApiTimeoutInMillis);
                urlCxn.setRequestProperty("content-type", "application/json; charset=utf-8");
                urlCxn.setRequestProperty("Authorization", "Basic " + basicAuthentication);
            }
        } catch (Exception e) {
            exceptionCounter.incrementAndGet();
            logger.warn("Exception: one_set: failed to connect to CopperEgg Service '{}' with proxy {}", newurl, proxy, e);
            return;
        }
        if( urlCxn != null ) {
            try {     
                cue_serialize(counters, urlCxn.getOutputStream());
                int responseCode = urlCxn.getResponseCode();
                if (responseCode != 200) {
                    logger.warn("one_set: Failure {}: {} to send result to CopperEgg service {}", responseCode, urlCxn.getResponseMessage(), newurl);
                }       
                try {
                        InputStream in = urlCxn.getInputStream();
                        IoUtils2.copy(in, IoUtils2.nullOutputStream());
                        IoUtils2.closeQuietly(in);
                        InputStream err = urlCxn.getErrorStream();
                        if (err != null) {
                            IoUtils2.copy(err, IoUtils2.nullOutputStream());
                            IoUtils2.closeQuietly(err);
                        }
                } catch (IOException e) {
                        exceptionCounter.incrementAndGet();
                        logger.warn("Execption one_set: Write-Exception flushing http connection", e);
                }

            } catch (Exception e) {
                exceptionCounter.incrementAndGet();
                logger.warn("Execption: one_set: Failure to send result to CopperEgg Service '{}' with proxy {}", newurl, proxy, e);
            }
        }
    }
    public void cue_serialize(@Nonnull Iterable<QueryResult> counters, @Nonnull OutputStream out) throws IOException {
        int first = 0;
        long time = 0;
        String myID = null;
        JsonGenerator g = jsonFactory.createGenerator(out, JsonEncoding.UTF8);

        for (QueryResult counter : counters) {
           if( 0 == first ) {
              time = counter.getEpoch(TimeUnit.SECONDS);
              myID = counter.getType();
              first = 1;
              g.writeStartObject();
              g.writeStringField("identifier", myID);
              g.writeNumberField("timestamp", time);
              g.writeObjectFieldStart("values");
           }
           if (counter.getValue() instanceof Integer) {
                g.writeNumberField(counter.getName(), (Integer) counter.getValue());
            } else if (counter.getValue() instanceof Long) {
                g.writeNumberField(counter.getName(), (Long) counter.getValue());
            } else if (counter.getValue() instanceof Float) {
                g.writeNumberField(counter.getName(), (Float) counter.getValue());
            } else if (counter.getValue() instanceof Double) {
                g.writeNumberField(counter.getName(), (Double) counter.getValue());
            }
        }
        g.writeEndObject();
        g.writeEndObject();
        g.flush();
        g.close();
    }

    private static long getPID() {
       String processName =
          java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
       return Long.parseLong(processName.split("@")[0]);
    }

    public int cue_getExceptionCounter() {
        return exceptionCounter.get();
    }

    /**
     * If metric group doesn't exist, create it
     * If it does exist, update it.
     */

    public void ensure_metric_groups() {
        HttpURLConnection urlConnection = null;
        OutputStreamWriter wr = null;
        URL myurl = null;

        try {
           myurl = new URL(url_str + "/metric_groups.json?show_hidden=1");
           urlConnection = (HttpURLConnection) myurl.openConnection();
           urlConnection.setRequestMethod("GET");
           urlConnection.setDoInput(true);
           urlConnection.setDoOutput(true);
           urlConnection.setReadTimeout(coppereggApiTimeoutInMillis);
           urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
           urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);

           int responseCode = urlConnection.getResponseCode();
           if (responseCode != 200) {
               logger.warn("Bad responsecode " + String.valueOf(responseCode)+ " from metric_groups Index: " +  myurl);
           }
        } catch (Exception e) {
              exceptionCounter.incrementAndGet();
              logger.warn("Failure to execute metric_groups index request "+ myurl + "  "+ e);
        } finally {
            if (urlConnection != null) {
                try {
                    InputStream in = urlConnection.getInputStream();
                    String theString = convertStreamToString(in);
                    for (Map.Entry<String, String> entry : metricgroupMap.entrySet()) {
                        String checkName =  entry.getKey();
                        try {
                            String Rslt = groupFind(checkName, theString, 0);
                            if(Rslt != null){
                                // Update it
                                Rslt = Send_Commmand("/metric_groups/" + Rslt + ".json?show_hidden=1", "PUT", entry.getValue(),0);
                            } else {
                                // create it
                                Rslt = Send_Commmand("/metric_groups.json", "POST", entry.getValue(),0);
                            }
                            if(Rslt != null) {
                                if (Rslt.toLowerCase().contains("tomcat")) {
                                    if (Rslt.toLowerCase().contains("thread_pool")) {
                                       tomcat_thread_pool_groupID = Rslt;
                                    } else if(Rslt.toLowerCase().contains("grp")) { 
                                        tomcat_grp_groupID = Rslt;
                                    } else if(Rslt.toLowerCase().contains("servlet")) { 
                                        tomcat_servlet_groupID = Rslt;
                                    } else if(Rslt.toLowerCase().contains("manager")) { 
                                        tomcat_manager_groupID = Rslt;
                                    } else if(Rslt.toLowerCase().contains("db")) { 
                                        tomcat_db_groupID = Rslt;
                                    }
                                } else if (Rslt.toLowerCase().contains("jmxtrans")){
                                    jmxtrans_metric_groupID = Rslt;
                                } else if (Rslt.toLowerCase().contains("sales")){
                                    app_sales_groupID = Rslt;
                                } else if (Rslt.toLowerCase().contains("cocktail")){
                                    app_groupID = Rslt;
                                } else if (Rslt.toLowerCase().contains("jvm")){
                                    if (Rslt.toLowerCase().contains("os")) {
                                        jvm_os_groupID = Rslt;
                                    } else if (Rslt.toLowerCase().contains("gc")) {
                                        jvm_gc_groupID = Rslt;
                                    } else if (Rslt.toLowerCase().contains("runtime")) {
                                        jvm_runtime_groupID = Rslt;
                                    } else if (Rslt.toLowerCase().contains("class")) {
                                        jvm_class_groupID = Rslt;
                                    } else if (Rslt.toLowerCase().contains("thread")) {
                                        jvm_thread_groupID = Rslt;
                                    }
                                } else if (Rslt.toLowerCase().contains("nonheap")) {
                                    nonheap_metric_groupID = Rslt;
                                } else if (Rslt.toLowerCase().contains("heap")) {
                                    heap_metric_groupID = Rslt;
                                } 
                            }
                        } catch (Exception e) {
                            exceptionCounter.incrementAndGet();
                            logger.warn("Exception in metric_group update or create: "+ myurl + "  "+ e);
                        }
                    }
                } catch (IOException e) {
                    exceptionCounter.incrementAndGet();
                    logger.warn("Exception flushing http connection"+ e);
                }
            }
        }
    }
    /**
     * If dashboard doesn't exist, create it
     * If it does exist, update it.
     */

    private void ensure_dashboards() {
        HttpURLConnection urlConnection = null;
        OutputStreamWriter wr = null;
        URL myurl = null;

        try {
           myurl = new URL(url_str + "/dashboards.json");
           urlConnection = (HttpURLConnection) myurl.openConnection();
           urlConnection.setRequestMethod("GET");
           urlConnection.setDoInput(true);
           urlConnection.setDoOutput(true);
           urlConnection.setReadTimeout(coppereggApiTimeoutInMillis);
           urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
           urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);

           int responseCode = urlConnection.getResponseCode();
           if (responseCode != 200) {
               logger.warn("Bad responsecode " + String.valueOf(responseCode)+ " from Dahsboards Index: " +  myurl);
           }
        } catch (Exception e) {
            exceptionCounter.incrementAndGet();
            logger.warn("Exception on dashboards index request "+ myurl + "  "+ e);
        } finally {
            if (urlConnection != null) {
                try {
                    InputStream in = urlConnection.getInputStream();
                    String theString = convertStreamToString(in);
                    for (Map.Entry<String, String> entry : dashMap.entrySet()) {
                        String checkName =  entry.getKey();
                        try {
                            String Rslt = groupFind(checkName, theString, 1);
                            if(Rslt != null){
                                // Update it
                                Rslt = Send_Commmand("/dashboards/" + Rslt + ".json", "PUT", entry.getValue(),1);
                            } else {
                                // create it
                                Rslt = Send_Commmand("/dashboards.json", "POST", entry.getValue(),1);
                            }
                        } catch (Exception e) {
                            exceptionCounter.incrementAndGet();
                            logger.warn("Exception in dashboard update or create: "+ myurl + "  "+ e);   
                        }
                    }
                } catch (IOException e) {
                    exceptionCounter.incrementAndGet();
                    logger.warn("Exception flushing http connection"+ e);
                }
            }
        }
    }

    private String jparse(String jsonstr, Integer ExpectInt) {

        ObjectMapper mapper = new ObjectMapper();
        String Result = null;
        try {
            JsonNode root = mapper.readTree(jsonstr);
            if(ExpectInt != 0) {
                int myid = root.get("id").asInt();
                Result = String.valueOf(myid);
            } else {
                Result = root.get("id").asText().toString();
            }
        } catch (JsonGenerationException e) {
            exceptionCounter.incrementAndGet();
            logger.warn("JsonGenerationException "+ e);
        } catch (JsonMappingException e) {
            exceptionCounter.incrementAndGet();
            logger.warn("JsonMappingException "+ e);
        } catch (IOException e) {
            exceptionCounter.incrementAndGet();
            logger.warn("IOException "+ e);
        }
        return(Result);
    }


    public String convertStreamToString(InputStream is)
            throws IOException {
        //
        // To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.
        //
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    /**
     * read_config()
     * The copperegg_config.json file contains a specification for the metric groups and dashboards to be created / or updated.
     * Mandatory
     */
    public void read_config(InputStream in) throws Exception {

        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(in);

        JsonToken current;

        current = jp.nextToken();
        if (current != JsonToken.START_OBJECT) {
            logger.warn("read_config: Error:  START_OBJECT not found : quiting.");
            return;
        }
        current = jp.nextToken();
        String fieldName = jp.getCurrentName();
        current = jp.nextToken();
        if (fieldName.equals("config")) {
            if (current != JsonToken.START_OBJECT) {
                logger.warn("read_config: Error:  START_OBJECT not found after config : quiting.");
                return;
            }
            current = jp.nextToken();
            String fieldName2 = jp.getCurrentName();
            if (fieldName2.equals("metric_groups")) {
                current = jp.nextToken();
                if (current != JsonToken.START_ARRAY) {
                  logger.warn("read_config: Error:  START_ARRAY not found after metric_groups : quiting.");
                  return;
                }

                current = jp.nextToken();
                while (current != JsonToken.END_ARRAY) {
                    if (current != JsonToken.START_OBJECT) {
                        logger.warn("read_config: Error:  START_OBJECT not found after metric_groups START_ARRAY : quiting.");
                        return;
                    }
                    current = jp.nextToken();
                    JsonNode node1 = jp.readValueAsTree();
                    String node1string = write_tostring(node1);
                    metricgroupMap.put(node1.get("name").asText(),node1string);
                    current = jp.nextToken();
                }

                current = jp.nextToken();
                String fieldName3 = jp.getCurrentName();

                if (fieldName3.equals("dashboards")) {
                    current = jp.nextToken();
                    if (current != JsonToken.START_ARRAY) {
                        logger.warn("read_config: Error:  START_ARRAY not found after dashboards : quiting.");
                        return;
                    }
                    current = jp.nextToken();
                    while (current != JsonToken.END_ARRAY) {
                        if (current != JsonToken.START_OBJECT) {
                            logger.warn("read_config: Error:  START_OBJECT not found after dashboards START_ARRAY : quiting.");
                            return;
                        }
                        current = jp.nextToken();
                        JsonNode node = jp.readValueAsTree();
                        String nodestring = write_tostring(node);
                        dashMap.put(node.get("name").asText(),nodestring);
                        current = jp.nextToken();

                    }
                    if(jp.nextToken() != JsonToken.END_OBJECT) {
                        logger.warn("read_config: Error:  END_OBJECT expected, not found (1): quiting.");
                        return;
                    }
                    if(jp.nextToken() != JsonToken.END_OBJECT) {
                        logger.warn("read_config: Error:  END_OBJECT expected, not found (2): quiting.");
                        return;
                    }
                } else {
                    logger.warn("read_config: Error:  Expected dashboards : quiting.");
                    return;
                }
            } else {
                logger.warn("read_config: Error:  Expected metric_groups : quiting.");
                return;
            }
        }
    }

    public String groupFind(String findName, String findIndex, Integer ExpectInt) throws Exception {
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(findIndex);

        int count = 0;
        int foundit = 0;
        String Result = null;

        JsonToken current = jp.nextToken();
        if (current != JsonToken.START_ARRAY) {
          logger.warn("groupFind: Error: START_ARRAY expected, not found : quiting.");
          return(Result);
        }
        current = jp.nextToken();
        while (current != JsonToken.END_ARRAY) {
            if (current != JsonToken.START_OBJECT) {
                logger.warn("groupFind: Error: START_OBJECT expected, not found : quiting.");
                return(Result);
            }
            current = jp.nextToken();
            JsonNode node = jp.readValueAsTree();
            String tmpStr = node.get("name").asText().toString();
             if(findName.equals(node.get("name").asText().toString())) {
                if(ExpectInt != 0) {
                    foundit = node.get("id").asInt();
                    Result = String.valueOf(foundit);
                } else {
                    Result = node.get("id").asText().toString();
                }
                break;
            }
            current = jp.nextToken();
            count = count + 1;
        }
        return(Result);
    }

    public String write_tostring(JsonNode json){
        ObjectMapper mapper = new ObjectMapper();
        StringWriter out = new StringWriter();

        try {
            JsonFactory fac = new JsonFactory();
            JsonGenerator gen = fac.createJsonGenerator(out);

            // Now write:
            mapper.writeTree(gen, json);
            gen.flush();
            gen.close();
            return out.toString();
        }
        catch(Exception e) {
            exceptionCounter.incrementAndGet();
            logger.warn("Exception in write_tostring: " + e); 
        }
        return(null);
    }

    public String Send_Commmand(String command, String msgtype, String payload, Integer ExpectInt){
        HttpURLConnection urlConnection = null;
        URL myurl = null;
        OutputStreamWriter wr = null;
        int responseCode = 0;
        String id = null;
        int error = 0;

        try {
            myurl = new URL(url_str + command);
            urlConnection = (HttpURLConnection) myurl.openConnection();
            urlConnection.setRequestMethod(msgtype);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setReadTimeout(coppereggApiTimeoutInMillis);
            urlConnection.addRequestProperty("User-Agent", "Mozilla/4.76");
            urlConnection.setRequestProperty("content-type", "application/json; charset=utf-8");
            urlConnection.setRequestProperty("Authorization", "Basic " + basicAuthentication);

            wr = new OutputStreamWriter(urlConnection.getOutputStream(),"UTF-8");
            wr.write(payload);
            wr.flush();

            responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Send Command: Response code " + responseCode + " url is " + myurl + " command " + msgtype);
                error = 1;
            }
        } catch (Exception e) {
            exceptionCounter.incrementAndGet();
            logger.warn("Exception in Send Command: url is " + myurl + " command " + msgtype + "; " + e);
            error = 1;
        } finally {
            if (urlConnection != null) {
                try {
                  if( error > 0 ) {
                        InputStream err = urlConnection.getErrorStream();
                        String errString = convertStreamToString(err);
                        logger.warn("Reported error : " + errString); 
                        IoUtils2.closeQuietly(err);
                    } else {
                        InputStream in = urlConnection.getInputStream();
                        String theString = convertStreamToString(in);
                        id = jparse(theString, ExpectInt);
                        IoUtils2.closeQuietly(in);
                    }
                } catch (IOException e) {
                    exceptionCounter.incrementAndGet();
                    logger.warn("Exception in Send Command : flushing http connection " +  e);
                }
            }
            if(wr != null) {
                try {
                    wr.close();
                } catch (IOException e) {
                    exceptionCounter.incrementAndGet();
                    logger.warn("Exception in Send Command: closing OutputWriter " +  e);
                }
            }
        }
        return(id);
    }
}

