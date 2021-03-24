---
title: "Tracking License Usage"
excerpt: "Tracking license."
categories: installing
slug: tracking-license
toc: true
---


Refer to the [Tracking License Usage](https://ibm.github.io/FHIR/cloudpak/1.0/installing/tracking-license/) section in the IBM FHIR Server documentation.

### Upgrading to IBM FHIR Server Operator v1.1.0

Starting with IBM FHIR Server Operator v1.1.0, the IBM FHIR Server configuration requires a separate datasource configuration from the fhir-server-config.json configuration file.

Before upgrading to IBM FHIR Server Operator v1.1.0, set the IBM_FHIR_SERVER_DATASOURCE secret key as described in [Define IBM FHIR Server configuration](../creating).

Until the IBM_FHIR_SERVER_DATASOURCE secret key is set, the IBM FHIR Server instance will not be functional.