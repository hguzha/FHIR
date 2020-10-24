/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc;

import java.sql.Connection;

import javax.transaction.TransactionSynchronizationRegistry;

import com.ibm.fhir.database.utils.api.IDatabaseTranslator;
import com.ibm.fhir.database.utils.db2.Db2Translator;
import com.ibm.fhir.database.utils.derby.DerbyTranslator;
import com.ibm.fhir.database.utils.postgresql.PostgreSqlTranslator;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.jdbc.connection.FHIRDbFlavor;
import com.ibm.fhir.persistence.jdbc.dao.api.IResourceReferenceDAO;
import com.ibm.fhir.persistence.jdbc.dao.api.ParameterDAO;
import com.ibm.fhir.persistence.jdbc.dao.api.ResourceDAO;
import com.ibm.fhir.persistence.jdbc.dao.impl.ResourceDAOImpl;
import com.ibm.fhir.persistence.jdbc.dao.impl.ResourceReferenceDAO;
import com.ibm.fhir.persistence.jdbc.derby.DerbyResourceDAO;
import com.ibm.fhir.persistence.jdbc.derby.ReindexResourceDAO;
import com.ibm.fhir.persistence.jdbc.impl.ParameterTransactionDataImpl;
import com.ibm.fhir.persistence.jdbc.postgresql.Db2ResourceReferenceDAO;
import com.ibm.fhir.persistence.jdbc.postgresql.PostgreSqlResourceDAO;
import com.ibm.fhir.persistence.jdbc.postgresql.PostgresReindexResourceDAO;
import com.ibm.fhir.persistence.jdbc.postgresql.PostgresResourceReferenceDAO;

/**
 * Factory for constructing ResourceDAO implementations specific to a
 * particular {@link FHIRDbFlavor}.
 */
public class FHIRResourceDAOFactory {

    /**
     * Construct a new ResourceDAO implementation matching the database type
     * @param connection valid connection to the database
     * @param schemaName the name of the schema containing the FHIR resource tables
     * @param flavor the type and capability of the database and schema
     * @param trxSynchRegistry
     * @return a concrete implementation of {@link ResourceDAO}
     * @throws IllegalArgumentException
     * @throws FHIRPersistenceException
     */
    public static ResourceDAO getResourceDAO(Connection connection, String adminSchemaName, String schemaName, FHIRDbFlavor flavor, TransactionSynchronizationRegistry trxSynchRegistry, 
        FHIRPersistenceJDBCCache cache, ParameterTransactionDataImpl ptdi)
        throws IllegalArgumentException, FHIRPersistenceException {
        ResourceDAO resourceDAO = null;
        
        IResourceReferenceDAO rrd;
        switch (flavor.getType()) {
        case DB2:
            rrd = new Db2ResourceReferenceDAO(new Db2Translator(), connection, schemaName, cache.getResourceReferenceCache(), adminSchemaName);
            resourceDAO = new ResourceDAOImpl(connection, schemaName, flavor, trxSynchRegistry, cache, rrd, ptdi);
            break;
        case DERBY:
            rrd = new ResourceReferenceDAO(new DerbyTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            resourceDAO = new DerbyResourceDAO(connection, schemaName, flavor, trxSynchRegistry, cache, rrd, ptdi);
            break;
        case POSTGRESQL:
            rrd = new PostgresResourceReferenceDAO(new PostgreSqlTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            resourceDAO = new PostgreSqlResourceDAO(connection, schemaName, flavor, trxSynchRegistry, cache, rrd, ptdi);
            break;
        }
        return resourceDAO;
    }

    /**
     * Instantiate a new instance of {@link ReindexResourceDAO} configured for the given database type
     * @param connection
     * @param schemaName
     * @param flavor
     * @param trxSynchRegistry
     * @param cache
     * @param parameterDao
     * @return
     */
    public static ReindexResourceDAO getReindexResourceDAO(Connection connection, String adminSchemaName, String schemaName, FHIRDbFlavor flavor, TransactionSynchronizationRegistry trxSynchRegistry,
        FHIRPersistenceJDBCCache cache, ParameterDAO parameterDao) {

        IDatabaseTranslator translator = null;
        IResourceReferenceDAO rrd = null;
        ReindexResourceDAO result = null;
        
        switch (flavor.getType()) {
        case DB2:
            translator = new Db2Translator();
            rrd = new Db2ResourceReferenceDAO(translator, connection, schemaName, cache.getResourceReferenceCache(), adminSchemaName);
            result = new ReindexResourceDAO(connection, translator, parameterDao, schemaName, flavor, cache, rrd);
            break;
        case DERBY:
            translator = new DerbyTranslator();
            rrd = new ResourceReferenceDAO(translator, connection, schemaName, cache.getResourceReferenceCache());
            result = new ReindexResourceDAO(connection, translator, parameterDao, schemaName, flavor, cache, rrd);
            break;
        case POSTGRESQL:
            translator = new PostgreSqlTranslator();
            rrd = new PostgresResourceReferenceDAO(translator, connection, schemaName, cache.getResourceReferenceCache());
            result = new PostgresReindexResourceDAO(connection, translator, parameterDao, schemaName, flavor, cache, rrd);
            break;
        }
        return result;
    }

    /**
     * Construct a new ResourceDAO implementation matching the database type
     * @param connection valid connection to the database
     * @param schemaName the name of the schema containing the FHIR resource tables
     * @param flavor the type and capability of the database and schema
     * @return a concrete implementation of {@link ResourceDAO}
     * @throws IllegalArgumentException
     * @throws FHIRPersistenceException
     */
    public static ResourceDAO getResourceDAO(Connection connection, String adminSchemaName, String schemaName, FHIRDbFlavor flavor, 
        FHIRPersistenceJDBCCache cache) throws IllegalArgumentException, FHIRPersistenceException {
        ResourceDAO resourceDAO = null;
        
        IResourceReferenceDAO rrd;
        switch (flavor.getType()) {
        case DB2:
            rrd = new Db2ResourceReferenceDAO(new Db2Translator(), connection, schemaName, cache.getResourceReferenceCache(), adminSchemaName);
            resourceDAO = new ResourceDAOImpl(connection, schemaName, flavor, cache, rrd);
            break;
        case DERBY:
            rrd = new ResourceReferenceDAO(new DerbyTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            resourceDAO = new DerbyResourceDAO(connection, schemaName, flavor, cache, rrd);
            break;
        case POSTGRESQL:
            rrd = new PostgresResourceReferenceDAO(new PostgreSqlTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            resourceDAO = new PostgreSqlResourceDAO(connection, schemaName, flavor, cache, rrd);
            break;
        }
        return resourceDAO;
    }

    /**
     * Get a standalone DAO to handle the inserts of the common token values and
     * resource token refs just prior to the transaction commit
     * @param connection
     * @param schemaName
     * @param flavor
     * @param cache
     * @return
     * @throws IllegalArgumentException
     * @throws FHIRPersistenceException
     */
    public static IResourceReferenceDAO getResourceReferenceDAO(Connection connection, String adminSchemaName, String schemaName, FHIRDbFlavor flavor, 
        FHIRPersistenceJDBCCache cache) throws IllegalArgumentException, FHIRPersistenceException {
        
        IResourceReferenceDAO rrd = null;
        switch (flavor.getType()) {
        case DB2:
            rrd = new Db2ResourceReferenceDAO(new Db2Translator(), connection, schemaName, cache.getResourceReferenceCache(), adminSchemaName);
            break;
        case DERBY:
            rrd = new ResourceReferenceDAO(new DerbyTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            break;
        case POSTGRESQL:
            rrd = new PostgresResourceReferenceDAO(new PostgreSqlTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            break;
        }
        return rrd;
    }

    /**
     * Get a standalone DAO to handle the inserts of the common token values and
     * resource token refs just prior to the transaction commit.
     * @param connection
     * @param schemaName
     * @param flavor
     * @param trxSynchRegistry
     * @param cache
     * @param ptdi
     * @return
     * @throws IllegalArgumentException
     * @throws FHIRPersistenceException
     */
    public static IResourceReferenceDAO getResourceReferenceDAO(Connection connection, String adminSchemaName, String schemaName, FHIRDbFlavor flavor, TransactionSynchronizationRegistry trxSynchRegistry, 
        FHIRPersistenceJDBCCache cache, ParameterTransactionDataImpl ptdi)
        throws IllegalArgumentException, FHIRPersistenceException {
        
        IResourceReferenceDAO rrd = null;
        switch (flavor.getType()) {
        case DB2:
            rrd = new Db2ResourceReferenceDAO(new Db2Translator(), connection, schemaName, cache.getResourceReferenceCache(), adminSchemaName);
            break;
        case DERBY:
            rrd = new ResourceReferenceDAO(new DerbyTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            break;
        case POSTGRESQL:
            rrd = new PostgresResourceReferenceDAO(new PostgreSqlTranslator(), connection, schemaName, cache.getResourceReferenceCache());
            break;
        }
        return rrd;
    }
}
