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
* Schema upgrades require downtime: The IBM FHIR Server requires downtime to complete upgrades of the IBM FHIR Server's relational data. During the upgrade, the database's schema is updated to support workload for the HL7 FHIR® specification. 
* There is no rollback from the upgrade and changes to the schema.