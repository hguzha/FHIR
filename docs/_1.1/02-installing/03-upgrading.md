---
title: "Upgrading"
excerpt: "Upgrading IBM FHIR Server."
categories: installing
slug: upgrading
toc: true
---

### Upgrading from IBM FHIR Server operator 1.0.1

Prior to upgrading, it is recommended to create a new configuration secret (see [Define IBM FHIR Server configuration](../creating/#1-define-ibm-fhir-server-configuration) for details).

The new configuration secret will have the following changes from the existing configuration secret used in IBM FHIR Server operator 1.0.1:

* The value for the IBM_FHIR_SERVER_CONFIG secret key must be updated with the following changes:
1. Remove `persistence/jdbc/enableProxyDatasource`
2. Remove `persistence/jdbc/dataSourceJndiName`
3. Remove `persistence/datasources/<dsid>/connectionProperties`

* A new IBM_FHIR_SERVER_DATASOURCE secret key must be created. 

Until these secret keys are set, the IBM FHIR Server instance will not be functional.

There is no rollback from the upgrade and changes to the schema.