/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dao.api;

import com.ibm.fhir.persistence.exception.FHIRPersistenceException;

/**
 * Provides access to all the identity information we need when processing
 * resources and their parameters
 */
public interface JDBCIdentityCache {

    /**
     * Get the database id for the named resourceType. Reads from a cache or database
     * if required.
     * @param resourceType
     * @return
     * @throws FHIRPersistenceException
     */
    Integer getResourceTypeId(String resourceType) throws FHIRPersistenceException;

    /**
     * Get the database id for the named code-system. Creates new records if necessary
     * @param codeSystem
     * @return
     * @throws FHIRPersistenceException
     */
    Integer getCodeSystemId(String codeSystem) throws FHIRPersistenceException;

    /**
     * Get the database id for the given parameter name. Creates new records if necessary.
     * @param parameterName
     * @return
     * @throws FHIRPersistenceException
     */
    Integer getParameterNameId(String parameterName) throws FHIRPersistenceException;

    /**
     * Get the common_token_value_id for the given tokenValue and codeSystem. Reads from
     * a cache, or the database if not found in the cache.
     * @param codeSystem
     * @param tokenValue
     */
    Long getCommonTokenValueId(String codeSystem, String tokenValue);
}