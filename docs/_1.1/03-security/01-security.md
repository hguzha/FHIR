---
title: "Security"
excerpt: ""
categories: security
slug: security
toc: true
---

### Data in motion

* To secure all inbound and outbound requests from the IBM FHIR Server are recommended to be encrypted. 
* Users are recommended to use TLS v1.2.

### Data at rest

* The prerequisite database must have data encryption enabled.  
* Each instance is responsible for Backup and Recovery of the Database and must backup solution specific configurations.