---
layout: default
title: Search Configuration Overview
date:   2020-01-15 08:37:05 -0400
permalink: /FHIRSearchConfiguration/
markdown: kramdown
---

# IBM FHIR Server - Search Configuration Overview
The [FHIR Specification](https://www.hl7.org/fhir/r4/search.html) defines a set of searchable fields for each resource type. The IBM FHIR Server supports searching via both specification-defined search parameters and tenant-specific search parameters.

Specifically, the IBM FHIR Server supports searching on additional fields, including:
* fields that are defined in the base specification, which are not configured for search; and
* extension elements that a tenant may add to a standard FHIR resource type

The IBM FHIR Server allows deployers to define search parameters on a tenant-specific basis. This allows each tenant to share an instance of the IBM FHIR Server while maintaining the ability to have their own set of search parameters. Additionally, specification-defined search parameters can be filtered out in order to avoid the cost of extracting and storing the corresponding indices.

Tenant search parameters are defined via a [Bundle](https://www.hl7.org/fhir/r4/bundle.html) of [SearchParameter](https://www.hl7.org/fhir/r4/searchparameter.html) resources that define the additional search parameters which describe the searchable field and define the FHIRPath expression for extraction.  For example, a tenant that extends the `Patient` resource type with the `favorite-color` extension, enables search on `favorite-color` by defining a SearchParameter as part of this bundle.

## 1 Configuration
There are three layers of search parameter configuration.  
- built-in parameters from the core specification and packaged implementation guides
- default tenant parameters
- tenant-specific parameters

The built-in SearchParameters are loaded from the `fhir-registry` in the `fhir-search` module during server startup.

The default and tenant level configurations are put in the `default` and tenant-specific (e.g. `tenant1`) config folders respectively. These folders are populated with `extension-search-parameters.json`.

The IBM FHIR Server configuration prefers the JSON formatted configuration documents, and implements caching via [TenantSpecificSearchParameterCache.java](https://github.com/IBM/FHIR/blob/main/fhir-search/src/main/java/com/ibm/fhir/search/parameters/cache/TenantSpecificSearchParameterCache.java).

The IBM FHIR Server supports compartment searches based on the CompartmentDefinition resources found at [fhir-search/src/main/resources/compartments.json](https://github.com/IBM/FHIR/blob/main/fhir-search/src/main/resources/compartments.json). These definitions come directly from the specification and the server provides no corresponding default or tenant-level configuration.

### 1.1 Tenant-specific parameters
To configure tenant-specific search parameters, create a file called `extension-search-parameters.json`, placing it in the `${server.config.dir}/config/<tenant-id>` directory. For example, the `${server.config.dir}/config/acme/extension-search-parameters.json` file would contain the search parameters for the `acme` tenant, while `${server.config.dir}/config/qpharma/extension-search-parameters.json` would contain search parameters used by the `qpharma` tenant.

When the IBM FHIR Server processes a request associated with the `acme` tenant, the server uses the built-in search parameters and the user-defined search parameters defined in the `acme` tenant's `extension-search-parameters.json` file. Likewise, when processing a request associated with the `qpharma` tenant, the server uses the built-in search parameters and the user-defined search parameters defined in the `qpharma` tenant's `extension-search-parameters.json` file.

If a tenant-specific extension-search-parameters.json file does not exist, the server falls back to the global `extension-search-parameters.json` file found at `${server.config.dir}/config/default/extension-search-parameters.json`. For performance reasons, we recommend having an `extension-search-parameters.json` for each tenant.

The IBM FHIR Server caches search parameters in memory (organized first by tenant id, then by resource type and search parameter code). Any updates to a tenant's `extension-search-parameters.json` file causes the IBM FHIR Server to re-load the tenant's search parameters and refresh the information stored in the cache, without requiring a server re-start. This allows the deployer to deploy a new tenant's `extension-search-parameters.json` or update an existing file without re-starting the IBM FHIR Server and any subsequent requests processed by the IBM FHIR Server after the updates have been made use the updated search parameters. However, it is important to note that this process **does not** re-index already-created resources that are stored on the IBM FHIR Server.

Starting in version 4.5.0, the IBM FHIR Server supports [re-indexing resources](#2-re-index) with an updated set of search parameters. This is very similar to creating a new version of the resources, except in this case the lastUpdated time and the resource version number don't change and the data for the resource never leaves the server.

#### 1.1.1 Search parameters configuration: extension-search-parameters.json
To configure the IBM FHIR Server with one or more custom search parameters, create a file called `extension-search-parameters.json` and populate the contents with a Bundle of `SearchParameter` resources.

The `fhir-search` module requires that the [expression](https://www.hl7.org/fhir/r4/searchparameter-definitions.html#SearchParameter.expression), [type](https://www.hl7.org/fhir/r4/searchparameter-definitions.html#SearchParameter.type) and [code](https://www.hl7.org/fhir/r4/searchparameter-definitions.html#SearchParameter.code) be set, as in the following example:
```
{
   "resourceType": "Bundle",
   "id": "searchParams",
   "meta": {
      "lastUpdated": "2019-07-12T22:37:54.724+11:00"
   },
   "type": "collection",
   "entry": [{
      "fullUrl": "http://ibm.com/fhir/SearchParameter/Patient-favorite-color",
      "resource": {
         "resourceType": "SearchParameter",
         "id": "Patient-favorite-color",
         "url": "http://ibm.com/fhir/SearchParameter/Patient-favorite-color",
         "version": "4.0.0",
         "name": "favorite-color",
         "status": "draft",
         "experimental": false,
         "date": "2018-12-27T22:37:54+11:00",
         "publisher": "IBM FHIR Server Test",
         "contact": [{
            "telecom": [{
               "system": "url",
               "value": "http://ibm.com/fhir"
            }]
         },
         {
            "telecom": [{
               "system": "url",
               "value": "http://ibm.com/fhir"
            }]
         }],
         "description": "the patient's favorite color",
         "code": "favorite-color",
         "base": ["Patient"],
         "type": "string",
         "xpathUsage": "normal",
         "xpath": "f:Patient/f:extension[@url='http://ibm.com/fhir/extension/Patient/favorite-color']/f:valueString",
         "expression": "Patient.extension.where(url='http://ibm.com/fhir/extension/Patient/favorite-color').value",
         "multipleOr": true,
         "multipleAnd": true,
         "modifier": []
      }
   }
}
```

A few things to note are:
- This SearchParameter includes an xpath element for completeness, but the IBM FHIR Server does not use the XPath during extraction; it only uses the expression (FHIRPath).
- The SearchParameter with a path including `value` use the Choice data types which are determined based on the SearchParameter type.
- Each time a resource is created or updated, the IBM FHIR Server evaluates the FHIRPath expression applicable to the resource type and indexes the values of the matching elements, making these available via a search where the query parameter name matches the `code` element on the `SearchParameter` definition.

In the preceding example, extension elements (on a Patient resource) with a url of `http://ibm.com/fhir/extension/Patient/favorite-color` are indexed by the `favorite-color` search parameter. To search for Patients with a favorite color of "pink", users could send an HTTP GET request to a URL like `[base]/Patient?favorite-color:exact=pink`.

For more information on search parameters, see the [HL7 FHIR specification](https://www.hl7.org/fhir/R4/searchparameter.html).

#### 1.1.2 Recommendations
When creating the SearchParameter FHIRPath expression, be sure to test both the FHIRPath expression and the search parameter.

If a search parameter expression extracts an element with a data type that is incompatible with the declared search parameter type, the server skips the value and logs a message. For choice elements, like Extension.value, its recommended to restrict the expression to values of the desired type by using the `as` function. For example, to select only Decimal values from the http://example.org/decimal extension, use an expressions like `Basic.extension.where(url='http://example.org/decimal').value.as(Decimal)`.

#### 1.1.2.1 The implicit-system extension
The IBM FHIR Server team has introduced a custom SearchParameter extension that can be used to improve search performance for queries that are made against a token SearchParameter without passing a system. Specifically, for SearchParameter resources that index elements of type Code which have a required binding with a single system, adding the following extension to the SearchParameter definition allows the server to infer the system value without requiring end users to explicitly pass it in their queries:

```json
{
    "url": "http://ibm.com/fhir/extension/implicit-system",
    "valueUri": "http://hl7.org/fhir/account-status"
}
```

See the [FHIR Performance Guide](FHIRPerformanceGuide#54-search-examples) for more information.

### 1.2 Filtering
The IBM FHIR Server supports the filtering of search parameters through `fhir-server-config.json`. The default behavior of the IBM FHIR Server is to consider all built-in and tenant-specific search parameters when storing resources or processing search requests, but you can configure inclusion filters to restrict the IBM FHIR Server's view to specific search parameters on a given resource type.

Why would you want to filter built-in search parameters? The answer lies in how search parameters are used by the IBM FHIR Server. When the FHIR server processes a _create_ or _update_ operation, it stores the resource contents in the datastore, along with search index information that is used by the IBM FHIR Server when performing search operations. The search index information stored for a particular resource instance is driven by the search parameters defined for that resource type. Therefore if you are storing a resource whose type has a lot of built-in search parameters defined for it (e.g. `Patient`), then you could potentially be storing a lot of search index information for each resource.

For performance and scalability reasons, it might be desirable to limit the number of search parameters considered during a _create_ or _update_ operation for particular resource types. If there will be no need to use the search index information, there's no need to store it. For example, if you know that due to the way in which a particular tenant's applications use the FHIR REST API that those applications will never need to search Patients by birthdate, then there would be no need to store search index information for the `birthdate` attribute in `Patient` resources. Consequently, you could filter out the `birthdate` search parameter for the `Patient` resource type and not lose any needed functionality, but yet save a little bit of storage capacity in your datastore.

The search parameter filtering feature is supported through a set of inclusion rules specified via the `fhirServer/resources` property group in `fhir-server-config.json`. The search parameter inclusion rules allow you to define a set of search parameters per resource type that should be included in the IBM FHIR Server's view of search parameters when storing resources and performing search operations. The following snippet shows the general form for specifying inclusion rules:

```
"resources": {
    "open": false,
    "CarePlan": {
        "searchParameters": {}
    },
    "ExplanationOfBenefit": {
        "searchParameters": {
            "_id": "http://hl7.org/fhir/SearchParameter/Resource-id",
            "_lastUpdated": "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated",
            "patient": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-patient",
            "type": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-type",
            "identifier": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-identifier",
            "service-date": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-service-date"
        }
    },
    "Patient": {
        "searchParameters": {}
    },
    "RelatedPerson": {
        "searchParameters": {}
    }
}
```

The `fhirServer/resources/<resourceType>/searchParameters` property group is a JSON map where the key is the search parameter code that will be used to search on this parameter and the value is a canonical URL which resolves to a SearchParameter definition from the `fhir-registry` at run-time.
Omitting this property is equivalent to supporting all search parameters in the server's registry that apply to the given resource type.
An empty object, `{}`, can be used to indicate that no global search parameters are supported.
It may be desirable to re-define a single search parameter code. In this case, if you do not wish to filter any other parameters for this type, a value of `"*": "*"` can be used to prevent further filtering.

Additionally, for SearchParameters defined across all resource types (i.e. SearchParameters with a base of type `Resource`), the filter can be applied at this level as well:

```
"resources": {
    "open": false,
    "CarePlan": {
        "searchParameters": {}
    },
    "ExplanationOfBenefit": {
        "searchParameters": {
            "patient": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-patient",
            "type": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-type",
            "identifier": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-identifier",
            "service-date": "http://hl7.org/fhir/us/carin-bb/SearchParameter/explanationofbenefit-service-date"
        }
    },
    "Patient": {
        "searchParameters": {}
    },
    "RelatedPerson": {
        "searchParameters": {}
    },
    "Resource": {
        "searchParameters": {
            "_id": "http://hl7.org/fhir/SearchParameter/Resource-id",
            "_lastUpdated": "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated"
        }
    }
}
```

For reference, the following is the list of codes in this category:

| code                          |
|-------------------------------|
| _id                           |
| _lastUpdated                  |
| _profile                      |
| _security                     |
| _source                       |
| _tag                          |
| _content                      |
| _query                        |

Note: `_content` and `_query` are not yet supported by the IBM FHIR Server.

The filter `"*": "*"` is not necessary to include these Resource-level parameters.

#### 1.2.1 Handling unexpected search parameters
The IBM FHIR Server supports configurable handling of unknown or unsupported search parameters as defined at https://ibm.github.io/FHIR/Conformance#http-headers.
Filtered search parameters are handled exactly the same as undefined search parameters, meaning that searches which include these parameters will fail in `strict` mode.

#### 1.2.2 Compartment search considerations with filtering
It is possible that results returned by compartment searches can be affected by filtering. For each compartment type, the rules for determining if a resource is a member of a compartment of that type, and thus if that resource should be returned on an associated compartment search, are based on inclusion criteria. Inclusion criteria is defined to be one or more search parameters whose value is a reference to the compartment resource type. These membership rules are defined by the FHIR specification for the following compartments:
- [Device](https://www.hl7.org/fhir/compartmentdefinition-device.html)
- [Encounter](https://www.hl7.org/fhir/compartmentdefinition-encounter.html)
- [Patient](https://www.hl7.org/fhir/compartmentdefinition-patient.html)
- [Practitioner](https://www.hl7.org/fhir/compartmentdefinition-practitioner.html)
- [RelatedPerson](https://www.hl7.org/fhir/compartmentdefinition-relatedperson.html)

For example, for the `Patient` compartment, to determine if an `Observation` is a member, the inclusion criteria search parameters are `subject` and `performer`. If the `Observation` resource fields associated with these search parameters reference a `Patient` resource, the `Observation` resource is a member of that `Patient` compartment.

However, in order for a resource to be returned as expected on a compartment search, the search index information associated with the inclusion criteria search parameters for that resource must get generated and stored at the time the resource is created or updated. If these search parameters have been filtered out based on the filtering configuration in `fhir-server-config.json`, the IBM FHIR Server will not store the necessary search index information, thus the resource will not be returned as expected on a compartment search. Using the example above, if the `subject` and `performer` search parameters have been filtered out for the `Observation` resource, it will not be returned as expected on a `Patient` compartment search. The following snippet illustrates a search parameter configuration in which the `subject` and `performer` search parameters have been filtered out:

```
"resources": {
    "open": true,
    "Observation": {
        "searchParameters": {
            "_id": "http://hl7.org/fhir/SearchParameter/Resource-id",
            "_lastUpdated": "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated",
            "patient": "http://hl7.org/fhir/SearchParameter/clinical-patient",
            "category": "http://hl7.org/fhir/SearchParameter/Observation-category",
            "code": "http://hl7.org/fhir/SearchParameter/clinical-code",
            "status": "http://hl7.org/fhir/SearchParameter/Observation-status",
            "date": "http://hl7.org/fhir/SearchParameter/clinical-date"
        }
    }
}
```

In order to avoid this issue, inclusion criteria search parameters should not be filtered out. If any filtering is configured in `fhir-server-config.json` for resources that may be members of a compartment, their inclusion criteria search parameters should be included in the list of allowed search parameters. Again using the example above, if search parameter filtering is specified for the `Observation` resource type, the `subject` and `performer` search parameters must be specified in the `searchParameters` list (assuming an entry of `"*": "*"` is not specified) in order for `Observation` resources to be returned in `Patient` compartment searches. The following snippet illustrates a search parameter configuration in which the `subject` and `performer` search parameters have been included in the list of allowed search parameters:

```
"resources": {
    "open": true,
    "Observation": {
        "searchParameters": {
            "_id": "http://hl7.org/fhir/SearchParameter/Resource-id",
            "_lastUpdated": "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated",
            "patient": "http://hl7.org/fhir/SearchParameter/clinical-patient",
            "category": "http://hl7.org/fhir/SearchParameter/Observation-category",
            "code": "http://hl7.org/fhir/SearchParameter/clinical-code",
            "status": "http://hl7.org/fhir/SearchParameter/Observation-status",
            "date": "http://hl7.org/fhir/SearchParameter/clinical-date",
            "subject": "http://hl7.org/fhir/SearchParameter/Observation-subject",
            "performer": "http://hl7.org/fhir/SearchParameter/Observation-performer"
        }
    }
}
```

##  2 Re-index
Reindexing is implemented as a custom operation that tells the IBM FHIR Server to read a set of resources and replace the existing search parameters with those newly extracted from the resource body.

The `$reindex` operation can be invoked via an HTTP(s) POST to `[base]/$reindex`. By default, the operation will select 10 resources and re-extract their search parameters values based on the current configuration of the server. The operation supports the following parameters to control the behavior:

|name|type|description|
|----|----|-----------|
|`tstamp`|string|Reindex any resource not previously reindexed before this timestamp. Format as a date YYYY-MM-DD or time YYYY-MM-DDTHH:MM:DDZ.|
|`resourceCount`|integer|The maximum number of resources to reindex in this call. If this number is too large, the processing time might exceed the transaction timeout and fail.|

The IBM FHIR Server tracks when a resource was last reindexed and only resources with a reindex_tstamp value less than the given tstamp parameter will be processed. When a resource is reindexed, its reindex_tstamp is set to the given tstamp value. In most cases, using the current date (for example "2020-10-27") is the best option for this value.

To aid in the re-indexing process, the IBM FHIR Server team has expanded the fhir-bucket resource-loading tool to support driving the reindex. The fhir-bucket tool uses a thread-pool to make concurrent POST requests to the IBM FHIR Server `$reindex` custom operation.

For more information on driving the reindex operation from fhir-bucket, see https://github.com/IBM/FHIR/tree/main/fhir-bucket#driving-the-reindex-custom-operation.

Reindexing is resource-intensive and can take several hours or even days to complete depending on the number of resources currently in the system and the capability of the hosting platform.

---
FHIR® is the registered trademark of HL7 and is used with the permission of HL7.
