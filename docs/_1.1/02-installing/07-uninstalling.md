---
title: "Uninstalling"
excerpt: "Uninstalling IBM FHIR Server."
categories: installing
slug: uninstalling
toc: true
---

You can delete an IBM FHIR Server instance by using the OpenShift Container Platform web console. You can also uninstall the IBM FHIR Server operator and the catalog.

## Deleting an IBM FHIR Server instance

1.  Log into the OpenShift Container Platform web console using your login credentials.

2. Change the Project to the namespace you want to work with.

3. Navigate to Installed Operators and click on the IBM FHIR Server operator.

4. Navigate to the IBM FHIR Server tab and click on the instance you want to delete.

5. Select Delete IBMFHIRServer from the Actions menu.

## Uninstalling the Operator

1.  Log into the OpenShift Container Platform web console using your login credentials.

2. Change the Project to the namespace you want to work with.

3. Navigate to Installed Operators and click on the IBM FHIR Server operator.

4. Select Uninstall Operator from the Actions menu.

## Uninstalling the catalog

The latest version of the IBM Operator Catalog includes the IBM FHIR Server Operator. If that catalog is installed, you may have skipped creating the ibm-fhir-server-operator-catalog CatalogSource resource, in which case, these instructions do not apply.

1.  Log into the OpenShift Container Platform web console using your login credentials.

2. Navigate to Custom Resource Definitions and click on CatalogSource.

3. Navigate to the Instances tab and click on ibm-fhir-server-operator-catalog.

4. Select Delete CatalogSource from the Actions menu.

