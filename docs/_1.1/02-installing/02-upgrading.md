---
title: "Upgrading"
excerpt: "Upgrading IBM FHIR Server."
categories: installing
slug: upgrading
toc: true
---

### Upgrading to IBM FHIR Server Operator v1.1.0

When upgrading to IBM FHIR Server Operator v1.1.0, the IBM_FHIR_SERVER_DATASOURCE and IBM_FHIR_SERVER_CONFIG must be created and updated. Until these secrets key is set, the IBM FHIR Server instance will not be functional.

To update the existing secret IBM_FHIR_SERVER_CONFIG, please download the secret to a file - fhir-server-config.json:

    1. remove `persistence/jdbc/enableProxyDatasource` from fhir-server-config.json
    2. remove `persistence/jdbc/dataSourceJndiName` from fhir-server-config.json
    3. remove `persistence/datasources/<dsid>/connectionProperties` from fhir-server-config.json

Create a new datasource.xml per the create-configuration section in the IBM FHIR Server operator readme.

There is no rollback from the upgrade and changes to the schema.