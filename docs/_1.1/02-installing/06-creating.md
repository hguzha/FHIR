---
title: "Creating an IBM FHIR Server"
excerpt: "Configure your IBM FHIR Server installation."
categories: installing
slug: creating
toc: true
---
## Prerequisites

Creating an instance of IBM FHIR Server requires an installed IBM FHIR Server Operator.
See [Installing](../installing) for instructions to install the IBM FHIR Server Operator.

Creating an instance of IBM FHIR Server requires at least namespace administration privileges. 

If you intend to use CLI commands, ensure you have the following installed:

  - [IBM Cloud Pak CLI (cloudctl)](https://github.com/IBM/cloud-pak-cli)
  - [Red Hat OpenShift Container Platform CLI (oc)](https://docs.openshift.com/container-platform/4.4/cli_reference/openshift_cli/getting-started-cli.html)

## Creating an instance

Complete the following steps to create an instance of IBM FHIR Server.

### 1. Define IBM FHIR Server configuration

Create Secret resource containing IBM FHIR Server configuration.

1. Define values for the following secret keys:

  - IBM\_FHIR\_SCHEMA\_TOOL\_INPUT

      The value must be set to the contents of the configuration file for IBM FHIR Server Schema Tool.

      See [Defining the schema tool input](#defining-the-schema-tool-input) for configuring the input.

      Put the contents of the configuration file in a file named `persistence.json`.

  - IBM\_FHIR\_SERVER\_CONFIG

      The value must be set to the contents of the fhir-server-config.json configuration file for IBM FHIR Server.

      See [IBM FHIR Server](https://hub.docker.com/r/ibmcom/ibm-fhir-server) for how to create a fhir-server-config.json configuration file.

      See [creating the configuration](#creating-the-configuration) for limitation and examples.

      Put the contents of the configuration file in a file named `fhir-server-config.json`.

  - IBM\_FHIR\_SERVER\_DATASOURCE
  
      The value must be set to the contents of the datasource definition file for IBM FHIR Server.

      See [creating the datasource](#creating-the-datasource) for limitation and examples.

      Put the contents of the datasource definition file in a file named `datasource.xml`.

  - IBM\_FHIR\_SERVER\_ADMIN\_PASSWORD

      The value must be set to the admin password to use for the IBM FHIR Server.

      Put the admin password in a file named `admin.txt`.

  - IBM\_FHIR\_SERVER\_USER\_PASSWORD

      The value must be set to the user password to use for the IBM FHIR Server.

      Put the user password in a file named `user.txt`.

  - IBM\_FHIR\_SERVER\_CERT

      Defining this secret key is optional, but it is needed when using PostgreSQL as the database.

      If defined, the value must be set to the public key of the intermediate CA certificate.

      Put the public key of the intermediate CA certificate in a file named `db.cert`.

  - IBM\_FHIR\_SERVER\_HOST

      Defining this secret key is optional.
      
      If defined, the value must be set to the contents of a configuration file for defining the host.
      
      Put the contents of the configuration file in a file named `host.xml`.

      See [Defining the host](#defining-the-host) for how to define the host.

  - IBM\_FHIR\_SERVER\_TRACE\_SPEC

      Defining this secret key is optional.
      
      If defined, the value must be set to the contents of a configuration file for defining the trace level.

      Put the contents of the configuration file in a file named `trace.txt`.

      See [defining the trace specification](#defining-the-trace-specification) for how to define the timeout.

  - IBM\_FHIR\_SERVER\_TRANSACTION\_TIMEOUT

      Defining this secret key is optional.
      
      If defined, the value must be set to the contents of a configuration file for defining the timeout. You should start with `240s`

      Put the contents of the configuration file in a file named `timeout.txt`.

      See [defining the transaction timeout](#defining-the-transaction-timeout) for how to define the timeout.

2. Create the secret from the files.

  *Note:* Any keys that are optional do not need to be created in the secret if they are not needed for the configuration.

  ```
  $ oc create secret generic <secret-name> \
      --from-file=IBM_FHIR_SCHEMA_TOOL_INPUT=./persistence.json \
      --from-file=IBM_FHIR_SERVER_CONFIG=./fhir-server-config.json \
      --from-file=IBM_FHIR_SERVER_DATASOURCE=./datasource.xml \
      --from-file=IBM_FHIR_SERVER_ADMIN_PASSWORD=./admin.txt \
      --from-file=IBM_FHIR_SERVER_USER_PASSWORD=./user.txt \
      --from-file=IBM_FHIR_SERVER_CERT=./db.cert \
      --from-file=IBM_FHIR_SERVER_HOST=./host.xml \
      --from-file=IBM_FHIR_SERVER_TRACE_SPEC=./trace.txt \
      --from-file=IBM_FHIR_SERVER_TRANSACTION_TIMEOUT=./timeout.txt \
      --namespace=<target-namespace>
  ```

  - `<secret-name>` is the name of the secret to contain the IBM FHIR Server configuration.
  - `<target-namespace>` is the target namespace.

### 2. Create IBM FHIR Server instance

Create an instance of IBM FHIR Server using one of the following methods:

#### Red Hat OpenShift Container Platform web console
 
  1. Log into the OpenShift Container Platform web console using your login credentials.

  2. Navigate to Installed Operators and click on IBM FHIR Server.

  3. Click `Create Instance` on the `IBMFHIRServer` tile.

  4. Enter a name for the instance, and enter the name of the Secret resource containing the IBM FHIR Server configuration.

  5. Click `Create`.

#### Red Hat OpenShift Container Platform CLI

  1. Create an IBMFHIRServer resource in the target namespace by editing the namespace and secret name in the sample file, `files/fhirserver_v1beta1_ibmfhirserver.yaml`, and then apply the file.

  ```
  $ oc apply -f files/fhirserver_v1beta1_ibmfhirserver.yaml
  ```

#### IBM Cloud Pak CLI

  1. Run the apply-custom-resources action.

  ```
  $ cloudctl case launch \
      --case case/ibm-fhir-server-case \
      --namespace <target-namespace> \
      --inventory ibmFhirServerOperator \
      --action apply-custom-resources \
      --args "--secretName <secret-name>"
  ```

  - `<target-namespace>` is the target namespace.
  - `<secret-name>` is the name of the Secret resource containing the IBM FHIR Server configuration.

### 3. Accessing IBM FHIR Server instance

  1. Verify the IBM FHIR Server instance is functional.

  ```
  $ oc get ibmfhirservers -n <target-namespace>
  ```

  - `<target-namespace>` is the target namespace.

    The READY value of "True" indicates the IBM FHIR Server instance is functional.

  2. Connect to the IBM FHIR Server instance.

  ```
  $ oc get services -n <target-namespace>
  ```

  - `<target-namespace>` is the target namespace.

    To make external connections to the IBM FHIR Server instance, either port-forward or create a route to the service.

    See the [IBM FHIR Server User's Guide](https://ibm.github.io/FHIR/guides/FHIRServerUsersGuide) for how to verify that it’s running properly by invoking the `$healthcheck` endpoint.

## Updating an instance

### 1. Update IBM FHIR Server configuration

Create a new Secret with the updated configuration by following the [Define IBM FHIR Server configuration](#1-define-ibm-fhir-server-configuration) instructions.

### 2. Update IBM FHIR Server instance

Update the existing instance of IBM FHIR Server to use the new Secret using one of the following methods:

#### Red Hat OpenShift Container Platform web console
 
  1. Log into the OpenShift Container Platform web console using your login credentials.

  2. Navigate to Installed Operators and click on IBM FHIR Server.

  3. Click the `IBM FHIR Server` tab.

  4. Click on the instance.

  5. Click the `YAML` tab.

  6. Update the `spec.secretName` field with name of the Secret resource containing the IBM FHIR Server configuration.

  7. Click `Save`.

#### Red Hat OpenShift Container Platform CLI

  1. Update the IBMFHIRServer resource in the target namespace by editing the namespace and secret name in the sample file, `files/fhirserver_v1beta1_ibmfhirserver.yaml`, and then apply the file.

  ```
  $ oc apply -f files/fhirserver_v1beta1_ibmfhirserver.yaml
  ```

#### IBM Cloud Pak CLI

  1. Run the apply-custom-resources action.

  ```
  $ cloudctl case launch \
      --case case/ibm-fhir-server-case \
      --namespace <target-namespace> \
      --inventory ibmFhirServerOperator \
      --action apply-custom-resources \
      --args "--secretName <secret-name>"
  ```

  - `<target-namespace>` is the target namespace.
  - `<secret-name>` is the name of the Secret resource containing the IBM FHIR Server configuration.

## Scaling

By default, the deployment uses 2 replicas.

Use the `oc scale` command to manually scale an IBM FHIR Server deployment.

## Defining the schema tool input

For the [IBM FHIR Server Schema Tool](https://hub.docker.com/r/ibmcom/ibm-fhir-schematool), the database must be configured with a functional user, which is referred to as FHIRSERVER. 

### Db2

The type must be specified as 'db2', the behavior must be 'onboard', and the corresponding database settings must be input into the json format. The tenant name must be `default`.

   Example: persistence.json 

  ``` json
  {
      "persistence": [
          {
              "db":  {
                  "host": "example.appdomain.cloud",
                  "port": "20010",
                  "database": "bludb",
                  "user": "myuser",
                  "password": "password",
                  "type": "db2",
                  "ssl": "true"
              },
              "tenant": {
                  "name": "default",
                  "key": "custom-key-here"
              },
              "schema": {
                  "fhir": "FHIRDATA",
                  "batch": "FHIR_JBATCH",
                  "oauth": "FHIR_OAUTH"
              },
              "grant":  "FHIRSERVER",
              "behavior": "onboard"
          }
      ]
  }
  ```

For db2, it is recommended that the granted user, and the schemas be configured as uppercase.  Further, the user schema names up to database limited sizes. Whatever is chosen as the schema name must match in the datasource.xml and the fhir-server-config.json. The tenant-key is further used in the fhir-server-config.json to authorize the configuration's access to the fhir schema's data partition.

### Postgres

The type must be specified as 'postgresql', the behavior must be 'onboard', and the corresponding database settings must be input into the json format. The certificate must be the base64 encoding of the trusted Certificate Authority of the Database server.

   Example: persistence.json 

  ``` json
  {
      "persistence": [
          {
              "db":  {
                  "host": "example.appdomain.cloud",
                  "port": "13999",
                  "database": "ibmclouddb",
                  "user": "dbuser",
                  "password": "password",
                  "type": "postgresql",
                  "ssl": "true",
                  "certificate_base64": "DWHuqdAPsuE67b4FP3mdUYVnY99wjCPm2vYA"
              },
              "schema": {
                  "fhir": "FHIRDATA",
                  "batch": "FHIR_JBATCH",
                  "oauth": "FHIR_OAUTH"
              },
              "grant":  "FHIRSERVER",
              "behavior": "onboard"
          }
      ]
  }
  ```

For postgres, it is recommended that the granted user, and the schemas be configured as uppercase.  Further, the user schema names up to database limited sizes. Whatever is chosen as the schema name must match in the datasource.xml and the fhir-server-config.json.

For production usage, the input must be specified as `"ssl": "true"`. It is not recommended to not use ssl.

Note, the schema tool does not support Apache Derby. The configuration does not support multiple tenants.

For more detail, see [IBM FHIR Server Schema Tool](https://hub.docker.com/r/ibmcom/ibm-fhir-schematool) for how to create a configuration file.

## Creating the configuration

The installer of the IBM FHIR Server must make sure the configuration is a valid IBM FHIR Server json, and a `fhirServer/persistence/datasources` entry must be created for postgresql or db2.

The currentSchema must match the `schema/fhir` entry in the persistence.json.

For db2, the tenantKey must match the entry in persistence.json.

The hints and searchOptimizerOptions must be specified.

 Example: db2 

``` json
"datasources": {
    "default": {
        "type": "db2",
        "tenantKey": "custom-key-here",
        "currentSchema": "FHIRDATA",
        "hints": {
            "search.reopt": "ONCE"
        }
    }
```

 Example: postgresql 

``` json
"datasources": {
    "default": {
        "type": "postgresql",
        "currentSchema": "FHIRDATA",
        "searchOptimizerOptions": {
            "from_collapse_limit": 12,
            "join_collapse_limit": 12
        }
    }
}
```

Note, the schema tool does not support Apache Derby. The configuration does not support multiple tenants.

Please note that the configuration does not support File,Http with the Bulkdata feature.

To view examples of the IBM FHIR Server fhir-server-config.json, please refer to [https://github.com/IBM/FHIR/tree/4.6.1/fhir-server/liberty-config/config/default](https://github.com/IBM/FHIR/tree/4.6.1/fhir-server/liberty-config/config/default). The documentation of the possible configuration elements are [https://github.com/IBM/FHIR/tree/4.6.1/fhir-server/liberty-config/config/default](https://github.com/IBM/FHIR/tree/4.6.1/fhir-server/liberty-config/config/default).

## Creating the datasource

The datasources must be configured with the `default` tenant and `default` datastore. The IBM FHIR Server uses the format `jdbc/fhir_` and the tenant id and datastore id to look the datasource when executing any FHIR data operation.  The `jdbc/fhirbatchDB` is fixed, and must not be changed in order to use the bulk data feature. The configuration must match the configuration defined in the persistence.json secret.

### Db2

The following is an example of the db2 datasource to support bulk data and fhir data.

  ``` xml
  <server>
      <dataSource id="fhirDefaultDefault" jndiName="jdbc/fhir_default_default" type="javax.sql.XADataSource" statementCacheSize="200" syncQueryTimeoutWithTransactionTimeout="true" validationTimeout="30s">
          <jdbcDriver javax.sql.XADataSource="com.ibm.db2.jcc.DB2XADataSource" libraryRef="sharedLibDb2"/>
          <properties.db2.jcc
              serverName="example.appdomain.cloud"
              portNumber="20010"
              user="fhirserver"
              password="change-password"
              databaseName="FHIRDB"
              currentSchema="FHIRDATA"
              driverType="4"
              sslConnection="true"
          />
          <connectionManager maxPoolSize="200" minPoolSize="40"/>
      </dataSource>
      <dataSource id="fhirbatchDS" jndiName="jdbc/fhirbatchDB" type="javax.sql.XADataSource" statementCacheSize="200" syncQueryTimeoutWithTransactionTimeout="true" validationTimeout="30s">
          <jdbcDriver javax.sql.XADataSource="com.ibm.db2.jcc.DB2XADataSource" libraryRef="sharedLibDb2"/>
          <properties.db2.jcc
              serverName="example.appdomain.cloud"
              portNumber="20010"
              user="fhirserver"
              password="change-password"
              databaseName="FHIRDB"
              currentSchema="FHIR_JBATCH"
              driverType="4"
              sslConnection="true"/>
      </dataSource>
  </server>
  ```

### Postgres

The following is an example of the postgres datasource to support bulk data and fhir data.

  ``` xml
  <server>
      <dataSource id="fhirDefaultDefault" jndiName="jdbc/fhir_default_default" type="javax.sql.XADataSource" statementCacheSize="200" syncQueryTimeoutWithTransactionTimeout="true" validationTimeout="30s">
          <jdbcDriver javax.sql.XADataSource="org.postgresql.xa.PGXADataSource" libraryRef="sharedLibPostgres"/>
          <properties.postgresql
              serverName="example.appdomain.cloud"
              portNumber="13999"
              databaseName="ibmclouddb"
              user="fhirserver"
              password="change-password"
              currentSchema="fhirdata"
          />
          <connectionManager maxPoolSize="200" minPoolSize="40"/>
      </dataSource>
      <dataSource id="fhirbatchDS" jndiName="jdbc/fhirbatchDB" type="javax.sql.XADataSource" statementCacheSize="200" syncQueryTimeoutWithTransactionTimeout="true" validationTimeout="30s">
          <jdbcDriver javax.sql.XADataSource="org.postgresql.xa.PGXADataSource" libraryRef="sharedLibPostgres"/>
          <properties.postgresql
              serverName="example.appdomain.cloud"
              portNumber="13999"
              databaseName="ibmclouddb"
              user="fhirserver"
              password="change-password"
              currentSchema="FHIR_JBATCH"
          />
          <connectionManager maxPoolSize="200" minPoolSize="40"/>
      </dataSource>
  </server>
  ```

Note, the schema tool does not support Apache Derby. The configuration does not support multiple tenants and does not support multiple datasources.

See [IBM FHIR Server](https://hub.docker.com/r/ibmcom/ibm-fhir-server) for how to create a datasource definition file via Liberty configDropin.

## Defining the host

It is recommended, for security purposes, to explicitly define the hosts for which the IBM FHIR Server will handle requests.

For any Route resources created for the IBM FHIR Server, ensure those hosts (e.g. test-fhir-server) are represented in the configuration file.

For example, the following configuration limits access to 9443 locally on the server, and the incoming route https://my-fhir-server.com. The host should match the host defined in the route.

``` xml
<server description="fhir-server">
    <httpEndpoint host="*" httpPort="-1" httpsPort="9443" id="defaultHttpEndpoint" onError="FAIL" />
    <virtualHost id="default_host" allowedFromEndpointRef="defaultHttpEndpoint">
      <hostAlias>*:9443</hostAlias>
      <hostAlias>my-fhir-server.com:443</hostAlias>
    </virtualHost>
</server>
```

## Defining the trace specification

The Trace specification is the WLP specification. 

- Normal Operation - `*=info`
- Trace Database access - `*=info:com.ibm.fhir.persistence.jdbc.dao.impl.*=fine`
- Trace all IBM FHIR Server - `*=info:com.ibm.fhir.*=fine`

## Defining the transaction timeout

The timeout controls the transaction timeout used when the IBM FHIR Server cluster connects to the user defined database. This value is the maximum time allowed for transactions started on the server to complete. Any transaction must be complete before this timeout, or the transaction is rolled back. Specify a positive integer followed by a unit of time, which can be hours (h), minutes (m), or seconds (s). For example, specify 30 seconds as 30s. You can include multiple values in a single entry. For example, 1m30s is equivalent to 90 seconds. The timeout value should not be smaller than 240 seconds.

