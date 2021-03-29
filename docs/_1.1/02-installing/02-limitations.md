---
title: "Limitations"
excerpt: "Limitations for IBM FHIR Server."
categories: installing
slug: limitations
toc: true
---

* The Operator may be deployed into different namespaces, one per namespace.
* The Operator has limited support for IBM FHIR Server configuration.
* The Operator does not support prepackaged implementation guides.
* Schema upgrades require downtime: The IBM FHIR Server requires downtime to complete upgrades of the IBM FHIR Server's relational data. During the upgrade, tables are refreshed, updated and optimized for the workloads that the FHIR specification supports.