/**
 * (C) Copyright IBM Corp. 2016,2017,2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.persistence;

import java.util.Properties;

import com.ibm.watsonhealth.fhir.persistence.exception.FHIRPersistenceException;

public interface PropertyBasedFHIRPersistenceFactory {

    FHIRPersistence getInstance(Properties properties) throws FHIRPersistenceException;
    
}
