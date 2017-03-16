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
package org.jmxtrans.agent.google;

import org.jmxtrans.agent.util.json.Json;
import org.jmxtrans.agent.util.json.JsonObject;
import org.jmxtrans.agent.util.json.JsonValue;
import org.jmxtrans.agent.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.jmxtrans.agent.util.StringUtils2.isNullOrEmpty;

/**
 * @author <a href="mailto:eminkevich@scentregroup.com">Evgeny Minkevich</a>
 * @author <a href="mailto:msimspon@scentregroup.com">Mitch Simpson</a>
 */
public class Connection {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/token";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String SCOPE = "https://www.googleapis.com/auth/monitoring";
    private static final String API_URL = "https://monitoring.googleapis.com/v3";

    private static Logger logger = Logger.getLogger(Connection.class.getName());

    private String token;
    private Date expiry;

    private String serviceAccount=null;
    private PrivateKey privateKey=null;

    Connection(String serviceAccount, String serviceAccountKey, String credentialsFileLocation) {
        this.serviceAccount = serviceAccount;

        if (!isNullOrEmpty(serviceAccount) && !isNullOrEmpty(serviceAccountKey)) {
            logger.info("Metrics Service Account has been provided : " + serviceAccount);
            this.serviceAccount = serviceAccount;
            this.privateKey = getPrivateKeyFromString(serviceAccountKey);
        }

        if ( privateKey == null  && !isNullOrEmpty(credentialsFileLocation)) {
            logger.info("Metrics Credentials File Name has been set explicitly : " + credentialsFileLocation);
            setFromFile(credentialsFileLocation);
        }

        String googleCredentialEnv = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if ( privateKey==null && !isNullOrEmpty(googleCredentialEnv)) {
            logger.info("No explicit Metrics connection configuration provided. Using Google defaults ");
            setFromFile(googleCredentialEnv);
        }

        if (this.privateKey == null)
            throw new RuntimeException("Failed to initialise connection to GCP Monitoring");
    }

    public String doGet(String urlString, String content) throws Exception {
        String token = getGoogleApiToken();
        return httpCall(API_URL+"/"+urlString, "GET", content, token);
    }

    public String doPost(String urlString, String content) throws Exception {
        String token = getGoogleApiToken();
        return httpCall(API_URL+"/"+urlString, "POST", content, token);
    }

    private void setFromFile(String credentialsFileLocation){
        try {
            JsonObject object = Json.parse(new InputStreamReader(new FileInputStream(credentialsFileLocation), "UTF-8")).asObject();
            this.serviceAccount = object.get("client_email").asString();
            this.privateKey = getPrivateKeyFromString(object.get("private_key").asString());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to parse '"+credentialsFileLocation+"' : "+ e.getMessage(), e);
        }
    }

    private PrivateKey getPrivateKeyFromString(String serviceKeyPem) {
        if (isNullOrEmpty(serviceKeyPem))
            return null;
        PrivateKey privateKey = null;
        try {
            String privKeyPEM = serviceKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                                             .replace("-----END PRIVATE KEY-----", "")
                                             .replace("\\r", "")
                                             .replace("\\n", "")
                                             .replace("\r", "")
                                             .replace("\n", "");

            byte[] encoded = DatatypeConverter.parseBase64Binary(privKeyPEM);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            privateKey = KeyFactory.getInstance("RSA")
                                   .generatePrivate(keySpec);
        } catch (Exception e) {
            String error = "Constructing Private Key from PEM string failed: " + e.getMessage();
            logger.log(Level.SEVERE, error, e);
        }
        return privateKey;
    }

    private String getGoogleApiToken() {
        if (isNullOrEmpty(token) ||
                expiry == null ||
                (System.currentTimeMillis() + 30000L) > expiry.getTime())
            prepareApiToken();
        return token;
    }

    Object tokenLock = new Object();
    private void prepareApiToken() {

        synchronized (tokenLock) {
            if (!isNullOrEmpty(token) && expiry != null && (System.currentTimeMillis() + 30000L) <= expiry.getTime()) {
                return;
            }

            try {
                // Header
                JsonObject header = new JsonObject();
                header.add("alg", "RS256");
                header.add("typ", "JWT");

                // Claim
                long utcSeconds = (System.currentTimeMillis() / 1000);

                JsonObject claim = new JsonObject();
                claim.add("aud", AUTH_URL);
                claim.add("exp", utcSeconds + 3600l);
                claim.add("iat", utcSeconds);
                claim.add("iss", serviceAccount);
                claim.add("scope", SCOPE);

                // Assertion
                String payload = encodeBase64Url(header.toString().getBytes(Charset.forName("UTF-8"))) +
                        "." +
                        encodeBase64Url(claim.toString().getBytes(Charset.forName("UTF-8")));
                String assertion = payload + "." + encodeBase64Url(signSHA256withRSA(payload));

                LinkedHashMap<String, String> postParameters = new LinkedHashMap<>();
                postParameters.put("grant_type", GRANT_TYPE);
                postParameters.put("assertion", assertion);

                String content = convertMapToContent(postParameters);

                // Get token
                String response = httpCall(AUTH_URL, "GET", content, null);

                String newToken = null;
                Date newExpiry = null;

                JsonObject json = Json.parse(response).asObject();
                if (json != null && json.get("access_token") != null) {
                    newToken = json.get("access_token").asString();
                    newExpiry = new Date(utcSeconds * 1000 + json.get("expires_in").asInt() * 1000);
                }

                JsonValue error = json.get("error");
                if (error != null) {
                    logger.log(Level.SEVERE, error.toString());
                }

                JsonValue errorDesc = json.get("error_description");
                if (errorDesc != null) {
                    logger.log(Level.SEVERE, errorDesc.toString());
                }

                if (!isNullOrEmpty(newToken) && newExpiry != null && System.currentTimeMillis() < newExpiry.getTime()) {
                    token = newToken;
                    expiry = newExpiry;
                    logger.log(Level.FINE, "Token : " + token);
                    logger.fine("Refreshed token. New expiry instant : " + expiry);
                } else {
                    logger.log(Level.WARNING, "Token refresh failed. Token : " + token + " Expiry : " + expiry);
                }

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Token refresh failed " + ex.getMessage(), ex);
            }
        }
    }

    private byte[] signSHA256withRSA(String value) {
        byte[] result = null;
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(this.privateKey);
            signature.update(value.getBytes(Charset.forName("UTF-8")));
            result = signature.sign();
        } catch (Exception var7) {
            logger.log(Level.SEVERE,var7.getMessage(), var7);
        }
        return result;
    }

    private String encodeBase64Url(byte[] value) {
        if (value == null) {
            return null;
        }

        if (value.length == 0) {
            return "";
        }
        try {

            String encoded = DatatypeConverter
                    .printBase64Binary(value)
                    .replace("=", "")
                    .replace("+", "-")
                    .replace("/", "_");

            return encoded;
        } catch (Exception var4) {
            logger.log(Level.WARNING,"FAILED URL ENCODING " + var4.getMessage(), var4);
            return null;
        }
    }


    private String convertMapToContent(LinkedHashMap<String, String> postParameters) throws Exception {
        StringBuilder content = new StringBuilder("");

        for (Map.Entry<String, String> entry : postParameters.entrySet()) {
            content.append((content.length() > 0 ? "&" : "") +
                    URLEncoder.encode(entry.getKey(), "UTF-8") +
                    "=" +
                    (!isNullOrEmpty(entry.getValue()) ? URLEncoder.encode(entry.getValue(), "UTF-8") : ""));
        }

        return content.length() > 0 ? content.toString() : null;
    }


    private String httpCall(String urlString, String method, String content, String token) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlString);

        HttpURLConnection conn;

        conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("User-Agent", "jmxtrans-agent");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        conn.setRequestMethod(method.toUpperCase());
        if (method.equalsIgnoreCase("POST")){
            conn.setRequestProperty("Content-Type", "application/json");
        }

        if (!isNullOrEmpty(content)) {
            conn.setDoOutput(true);
        }

        conn.setDoInput(true);

        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);

        conn.setRequestProperty("Connection", "Keep-Alive");
        conn.setRequestProperty("Content-length", isNullOrEmpty(content) ? "0" : "" + content.length());

        conn.connect();
        if (!isNullOrEmpty(content)) {
            OutputStream output = conn.getOutputStream();
            output.write(content.getBytes(Charset.forName("UTF-8")));
            output.flush();
        }

        InputStream is;
        boolean isError = false;

        if (conn.getResponseCode() < 400) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
            isError = true;
        }

        BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();

        if (isError)
            throw new RuntimeException(result.toString());

        return result.toString();
    }
}
