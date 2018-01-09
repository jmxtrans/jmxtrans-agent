## Documentation

Google Stackdriver Writer is based on v3 of the Google Cloud Monitoring API.
The configuration settings description can be found at [Google Cloud Metrics value types](https://cloud.google.com/monitoring/api/ref_v3/rest/v3/projects.metricDescriptors)

Metrics are configured as Stackdriver *custom* metrics.

### Configuration

StadriverWriter is configured somewhat differently from the rest of the jmxtrans agents -
it embeds multiple configuration options in "type" and "resultAlias" fields.

```xml
  <query objectName="java.lang:type=MemoryPool,name=*Eden*" attribute="Usage" key="used" type="GAUGE:By" resultAlias="jvm.memorypool:%name%"/>
```

#### Type

Type embeds three options:

* Metric Kind
* Metric Measurement Unit
* Metric Value Type

Assuming the serapaitr is defined as ":":
```
type="[METRIC_KIND[:METRIC_UNIT[:METRIC_VALUE_TYPE]]]"
```

All options are optional, with default value for Metric Kind being "GAUGE" and Metric Unit "1".

If not explicitly defined, metric value type will be derived from the collected value itself.

Metric Kinds supported by the writer:

* GAUGE
* CUMULATIVE

Metric Value types supported by the agent

* BOOL
* INT64
* DOUBLE
* STRING


#### Result Alias

The separator splits the field into the metric name and the classification labels.
Classification label names derived from the resultAlias are hardcoded as "attribute_1", "attribute_2", etc.

The example above results in a metric "jvm.memorypool" with classification label "attribute_1" set to "Usage".

The example below:
```
attribute="ExchangesTotal" resultAlias="camel.route:%name%:#attribute#"
```

will result in a metric "camel.route" with "attribute_1" set to the route name (%name%) and "attribute_2" set to "ExchangesTotal".

#### Static classification labels

It is also possible to define static classification labels shared by all metrics written by the writer.
Those are defined within the stackdriver configuration entry:

```xml
    <outputWriter class="org.jmxtrans.agent.google.StackdriverWriter">
        <!-- mandatory -->
        <projectId>${APM_PROJECT_ID:}</projectId>
        <serviceAccount>${APM_SERVICE_ACCOUNT:}</serviceAccount>
        <serviceAccountKey>${APM_SERVICE_KEY:}</serviceAccountKey>
        <applicationCredentials>${APM_APPLICATION_CREDENTIALS:}</applicationCredentials>
        <!-- optional -->
        <separator>:</separator>
        <namePrefix>acme.</namePrefix>

        <!-- Unrecognised entries : parsed as labels -->
        <meta_name>${APM_META_NAME:}</meta_name>
        <hostname>${APM_HOSTNAME:}</hostname>
    </outputWriter>

```

All configuration option are split into three sections

##### Recognised entries (which are NOT parsed as labels):

*Mandatory*:

* projectId : GCP Project to write metrics to
* serviceAccount : client_email from the GCP JSON key file
* serviceAccountKey : private_key from the GCP JSON key file
* applicationCredentials : JSON key file location

*Optional*:
* separator  - The java regex used to split the resultAlias to get attribute values. Defaults to ":"
* namePrefix - Prefixes all metric names written by this class. Default to empty.
* hostname   - Defaults to environment variable $HOSTNAME


##### Unrecognised entries (which ARE parsed as labels):

All other entries except the ones above will be parsed as static labels.

The "meta_name" parameter above will be added as "meta_name" label to all metrics.

### GCP Cloud Authentication

The agent recognises the following authentication options (in the order of preference if multiple are specified):

* serviceAccount / serviceAccountKey : client_email and private_key as defined in Google Credentials JSON file
* applicationCredentials : Google Credentials JSON file
* Google Container Engine (GKE) default service account accessible via [Google Metadata API](https://cloud.google.com/compute/docs/storing-retrieving-metadata)
* GOOGLE_APPLICATION_CREDENTIALS : Default Google Credentials file.

**OBS! Change!**
GKE Service account now takes preference over GOOGLE_APPLICATION_CREDENTIALS environmental variable. To enforce GOOGLE_APPLICATION_CREDENTIALS, reuse applicationCredentials configuration option.

```xml
    <outputWriter class="org.jmxtrans.agent.google.StackdriverWriter">

        <applicationCredentials>${GOOGLE_APPLICATION_CREDENTIALS:}</applicationCredentials>

    </outputWriter>
```

This has been done to avoid the conflict between the agent and other GCP drivers that might rely on GOOGLE_APPLICATION_CREDENTIALS.

Google Project ID needs to be defined explicitly to specify which project the metrics should be written to.
If the agent is deployed to Google Container/Compute engine Engine, and the projectId is not defined, it will be sourced
from the [Google Metadata API](https://cloud.google.com/compute/docs/storing-retrieving-metadata).

## Special thank you.

Special thank you goes to [Ralf Sternberg](http://eclipsesource.com/blogs/author/rsternberg/) for the brilliant [Minimal JSON library](https://github.com/ralfstx/minimal-json)
