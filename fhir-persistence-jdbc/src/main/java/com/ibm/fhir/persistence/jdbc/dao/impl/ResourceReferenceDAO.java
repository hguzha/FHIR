/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.jdbc.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.ibm.fhir.database.utils.api.IDatabaseTranslator;
import com.ibm.fhir.database.utils.common.DataDefinitionUtil;
import com.ibm.fhir.persistence.exception.FHIRPersistenceException;
import com.ibm.fhir.persistence.jdbc.dao.api.ICommonTokenValuesCache;
import com.ibm.fhir.persistence.jdbc.dao.api.IResourceReferenceDAO;
import com.ibm.fhir.persistence.jdbc.dto.CommonTokenValue;
import com.ibm.fhir.schema.control.FhirSchemaConstants;

/**
 * DAO to handle maintenance of the local and external reference tables
 * which contain the relationships described by "reference" elements in
 * each resource (e.g. Observation.subject).
 * 
 * The DAO uses a cache for looking up the ids for various entities. The
 * DAO can create new entries, but these can only be used locally until
 * the transaction commits, at which point they can be consolidated into
 * the shared cache. This has the benefit that we reduce the number of times
 * we need to lock the global cache, because we only update it once per
 * transaction.
 * 
 * For improved performance, we also make use of batch statements which
 * are managed as member variables. This is why it's important to close
 * this DAO before the transaction commits, ensuring that any outstanding
 * DML batched but not yet executed is processed. Calling close does not
 * close the provided Connection. That is up to the caller to manage.
 * Close does close any statements which are opened inside the class.
 */
public class ResourceReferenceDAO implements IResourceReferenceDAO, AutoCloseable {
    private static final Logger logger = Logger.getLogger(ResourceReferenceDAO.class.getName());
    
    private final String schemaName;

    // hold on to the connection because we use batches to improve efficiency
    private final Connection connection;
    
    // The cache used to track the ids of the normalized entities we're managing
    private final ICommonTokenValuesCache cache;
    
    // The translator for the type of database we are connected to
    private final IDatabaseTranslator translator;

    // The number of operations we allow before submitting a batch
    protected static final int BATCH_SIZE = 100;
    
    /**
     * Public constructor
     * @param c
     */
    public ResourceReferenceDAO(IDatabaseTranslator t, Connection c, String schemaName, ICommonTokenValuesCache cache) {
        this.translator = t;
        this.connection = c;
        this.cache = cache;
        this.schemaName = schemaName;
    }

    /**
     * Getter for the {@link IDatabaseTranslator} held by this DAO
     * @return
     */
    protected IDatabaseTranslator getTranslator() {
        return this.translator;
    }

    /**
     * Getter for the {@link Connection} held by this DAO
     * @return
     */
    protected Connection getConnection() {
        return this.connection;
    }

    /**
     * Getter for subclass access to the schemaName
     * @return
     */
    protected String getSchemaName() {
        return this.schemaName;
    }

    @Override
    public void flush() throws FHIRPersistenceException {
    }

    @Override
    public void close() throws FHIRPersistenceException {
        flush();
    }

    @Override
    public ICommonTokenValuesCache getResourceReferenceCache() {
        return this.cache;
    }

    /**
     * Look up the database id for the given externalSystemName
     * @param externalSystemName
     * @return the database id, or null if no record exists
     */
    public Integer queryExternalSystemId(String externalSystemName) {
        Integer result;
        
        final String SQL = "SELECT external_system_id FROM external_systems where external_system_name = ?";

        try (PreparedStatement ps = connection.prepareStatement(SQL)) {
            ps.setString(1, externalSystemName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getInt(1);
            } else {
                result = null;
            }
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            throw translator.translate(x);
        }
        
        return result;
    }
    
    /**
     * Find the database id for the given externalReferenceValue
     * @param externalReferenceValue
     * @return
     */
    public Integer queryExternalReferenceValueId(String externalReferenceValue) {
        Integer result;
        
        final String SQL = "SELECT external_reference_value_id FROM external_reference_values WHERE external_reference_value = ?";
        try (PreparedStatement ps = connection.prepareStatement(SQL)) {
            ps.setString(1, externalReferenceValue);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getInt(1);
            } else {
                result = null;
            }
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, SQL, x);
            throw translator.translate(x);
        }
        
        return result;
    }
    
    /**
     * Get a list of matching records from external_reference_values. Cheaper to do as one
     * query instead of individuals
     * @param externalReferenceValue
     * @return
     */
    public List<ExternalReferenceValue> queryExternalReferenceValues(String... externalReferenceValues) {
        List<ExternalReferenceValue> result = new ArrayList<>();
        if (externalReferenceValues.length == 0) {
            throw new IllegalArgumentException("externalReferenceValues array cannot be empty");
        }

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT external_reference_value_id, external_reference_value FROM external_reference_values WHERE external_reference_value IN (");
        
        for (int i=0; i<externalReferenceValues.length; i++) {
            if (i == 0) {
                sql.append("?");
            } else {
                sql.append(",?");
            }
        }
        sql.append(")");
        
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int a = 1;
            for (String xrv: externalReferenceValues) {
                ps.setString(a++, xrv);
            }
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new ExternalReferenceValue(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, sql.toString(), x);
            throw translator.translate(x);
        }
        
        return result;
    }
    
    public List<ExternalSystem> queryExternalSystems(String... externalSystemNames) {
        List<ExternalSystem> result = new ArrayList<>();
        if (externalSystemNames.length == 0) {
            throw new IllegalArgumentException("externalReferenceValues array cannot be empty");
        }

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT external_system_id, external_system_name FROM external_systems WHERE external_system_name IN (");
        
        for (int i=0; i<externalSystemNames.length; i++) {
            if (i == 0) {
                sql.append("?");
            } else {
                sql.append(",?");
            }
        }
        sql.append(")");
        
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int a = 1;
            for (String xrv: externalSystemNames) {
                ps.setString(a++, xrv);
            }
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new ExternalSystem(rs.getLong(1), rs.getString(2)));
            }
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, sql.toString(), x);
            throw translator.translate(x);
        }
        
        return result;
    }

    @Override
    public void deleteExternalReferences(int resourceTypeId, String logicalId) {
        final String DML = "DELETE FROM external_references "
                + "WHERE logical_resource_id IN ( "
                + " SELECT logical_resource_id FROM logical_resources "
                + "  WHERE resource_type_id = ? "
                + "    AND logical_id = ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(DML)) {
            ps.setInt(1, resourceTypeId);
            ps.setString(2, logicalId);
            ps.executeUpdate();
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, DML, x);
            throw translator.translate(x);
        }

    }

    @Override
    public void deleteLocalReferences(long logicalResourceId) {
        final String DML = "DELETE FROM local_references WHERE logical_resource_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(DML)) {
            ps.setLong(1, logicalResourceId);
            ps.executeUpdate();
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, DML, x);
            throw translator.translate(x);
        }
    }

    @Override
    public void deleteLogicalResourceCompartments(long logicalResourceId) {
        final String DML = "DELETE FROM logical_resource_compartments WHERE logical_resource_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(DML)) {
            ps.setLong(1, logicalResourceId);
            ps.executeUpdate();
        } catch (SQLException x) {
            // make the exception a little bit more meaningful knowing the database type
            logger.log(Level.SEVERE, DML, x);
            throw translator.translate(x);
        }
    }

    @Override
    public void addCommonTokenValues(String resourceType, Collection<ResourceTokenValueRec> xrefs) {
        // Grab the ids for all the code-systems, and upsert any misses
        List<ResourceTokenValueRec> systemMisses = new ArrayList<>();
        cache.resolveCodeSystems(xrefs, systemMisses);
        upsertCodeSystems(systemMisses);

        // Now that all the code-systems ids are known, we can search the cache
        // for all the token values, upserting anything new
        List<ResourceTokenValueRec> valueMisses = new ArrayList<>();
        cache.resolveTokenValues(xrefs, valueMisses);
        upsertCommonTokenValues(valueMisses);

        insertResourceTokenRefs(resourceType, xrefs);
    }

    /**
     * Insert the values in the resource-type-specific _resource_token_refs table. This
     * is a simple batch insert because all the FKs have already been resolved and updated
     * in the ResourceTokenValueRec records
     * @param resourceType
     * @param xrefs
     */
    protected void insertResourceTokenRefs(String resourceType, Collection<ResourceTokenValueRec> xrefs) {
        // Now all the values should have ids assigned so we can go ahead and insert them
        // as a batch
        final String tableName = resourceType + "_RESOURCE_TOKEN_REFS";
        DataDefinitionUtil.assertValidName(tableName);
        final String insert = "INSERT INTO " + tableName + "("
                + "parameter_name_id, logical_resource_id, common_token_value_id, ref_version_id) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(insert)) {
            int count = 0;
            for (ResourceTokenValueRec xr: xrefs) {
                ps.setInt(1, xr.getParameterNameId());
                ps.setLong(2, xr.getLogicalResourceId());

                // common token value can be null
                if (xr.getCommonTokenValueId() != null) {
                    ps.setLong(3, xr.getCommonTokenValueId());
                } else {
                    ps.setNull(3, Types.BIGINT);                    
                }

                // version can be null
                if (xr.getRefVersionId() != null) {
                    ps.setInt(4, xr.getRefVersionId());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.addBatch();
                if (++count == BATCH_SIZE) {
                    ps.executeBatch();
                    count = 0;
                }
            }
            
            if (count > 0) {
                ps.executeBatch();
            }
        } catch (SQLException x) {
            logger.log(Level.SEVERE, insert, x);
            throw translator.translate(x);
        }
    }
    
    /**
     * Add all the systems we currently don't have in the database. If all target
     * databases handled MERGE properly this would be easy, but they don't so
     * we go old-school with a negative outer join instead (which is pretty much
     * what MERGE does behind the scenes anyway).
     * INSERT INTO fhirdata.external_systems (external_system_name)
      SELECT v.name FROM fhirdata.external_systems s
  LEFT OUTER JOIN
      (VALUES ('hello'), ('world')) AS v(name)
           ON (s.external_system_name = v.name)
        WHERE s.external_system_name IS NULL;
     * @param systems
     */
    public void upsertCodeSystems(List<ResourceTokenValueRec> systems) {
        if (systems.isEmpty()) {
            return;
        }
        
        // Unique list so we don't try and create the same name more than once
        Set<String> systemNames = systems.stream().map(xr -> xr.getCodeSystemValue()).collect(Collectors.toSet());
        StringBuilder paramList = new StringBuilder();
        StringBuilder inList = new StringBuilder();
        for (int i=0; i<systemNames.size(); i++) {
            if (paramList.length() > 0) {
                paramList.append(", ");
                inList.append(",");
            }
            paramList.append("(CAST(? AS VARCHAR(" + FhirSchemaConstants.MAX_SEARCH_STRING_BYTES + ")))");
            inList.append("?");
        }
        
        final String paramListStr = paramList.toString();
        doCodeSystemsUpsert(paramListStr, systemNames);
        
        
        // Now grab the ids for the rows we just created. If we had a RETURNING implementation
        // which worked reliably across all our database platforms, we wouldn't need this
        // second query.
        StringBuilder select = new StringBuilder();
        select.append("SELECT code_system_name, code_system_id FROM code_systems WHERE code_system_name IN (");
        select.append(inList);
        select.append(")");
        
        Map<String, Integer> idMap = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(select.toString())) {
            // load a map with all the ids we need which we can then use to update the
            // ExternalResourceReferenceRec objects
            int a = 1;
            for (String name: systemNames) {
                ps.setString(a++, name);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                idMap.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException x) {
            logger.log(Level.SEVERE, select.toString(), x);
            throw translator.translate(x);
        }
        
        // Now update the ids for all the matching systems in our list
        for (ResourceTokenValueRec xr: systems) {
            Integer id = idMap.get(xr.getCodeSystemValue());
            if (id != null) {
                xr.setCodeSystemValueId(id);

                // Add this value to the (thread-local) cache
                cache.addCodeSystem(xr.getCodeSystemValue(), id);
            } else {
                // Unlikely...but need to handle just in case
                logger.severe("Record for code_system_name '" + xr.getCodeSystemValue() + "' inserted but not found");
                throw new IllegalStateException("id deleted from database!");
            }
        }
    }
    
    /**
     * Insert any missing values into the code_systems table
     * @param paramList
     * @param systems
     */
    public void doCodeSystemsUpsert(String paramList, Collection<String> systemNames) {
        // query is a negative outer join so we only pick the rows where
        // the row "s" from the actual table doesn't exist.
        
        // Derby won't let us use any ORDER BY, even in a sub-select so we need to
        // sort the values externally. It would be better if code_system_id were
        // an identity column, but that's a much bigger change.
        final List<String> sortedNames = new ArrayList<>(systemNames);
        sortedNames.sort((String left, String right) -> left.compareTo(right));
        
        final String nextVal = translator.nextValue(schemaName, "fhir_ref_sequence");
        StringBuilder insert = new StringBuilder();
        insert.append("INSERT INTO code_systems (code_system_id, code_system_name) ");
        insert.append("          SELECT ").append(nextVal).append(", v.name ");
        insert.append("            FROM (VALUES ").append(paramList).append(" ) AS v(name) ");
        insert.append(" LEFT OUTER JOIN code_systems s ");
        insert.append("              ON s.code_system_name = v.name ");
        insert.append("           WHERE s.code_system_name IS NULL ");
        
        // Note, we use PreparedStatement here on purpose. Partly because it's
        // secure coding best practice, but also because many resources will have the
        // same number of parameters, and hopefully we'll therefore share a small subset
        // of statements for better performance. Although once the cache warms up, this
        // shouldn't be called at all.
        try (PreparedStatement ps = connection.prepareStatement(insert.toString())) {
            // bind all the code_system_name values as parameters
            int a = 1;
            for (String name: sortedNames) {
                ps.setString(a++, name);
            }
            
            ps.executeUpdate();
        } catch (SQLException x) {
            logger.log(Level.SEVERE, insert.toString(), x);
            throw translator.translate(x);
        }
    }
    
    /**
     * Add reference value records for each unique reference name in the given list
     * @param values
     */
    public void upsertCommonTokenValues(List<ResourceTokenValueRec> values) {
        
        // Unique list so we don't try and create the same name more than once.
        // Ignore any null token-values, because we don't want to (can't) store
        // them in our common token values table.
        Set<CommonTokenValue> tokenValues = values.stream().filter(x -> x.getTokenValue() != null).map(xr -> new CommonTokenValue(xr.getCodeSystemValueId(), xr.getTokenValue())).collect(Collectors.toSet());
        
        if (tokenValues.isEmpty()) {
            // nothing to do
            return;
        }

        // Build a string of parameter values we use in the query to drive the insert statement.
        // The database needs to know the type when it parses the query, hence the slightly verbose CAST functions:
        // VALUES ((CAST(? AS VARCHAR(1234)), CAST(? AS INT)), (...)) AS V(common_token_value, parameter_name_id, code_system_id)
        StringBuilder inList = new StringBuilder(); // for the select query later
        StringBuilder paramList = new StringBuilder();
        for (int i=0; i<tokenValues.size(); i++) {
            if (paramList.length() > 0) {
                paramList.append(", ");
            }
            paramList.append("(CAST(? AS VARCHAR(" + FhirSchemaConstants.MAX_TOKEN_VALUE_BYTES + "))");
            paramList.append(",CAST(? AS INT))");
            
            // also build the inList for the select statement later
            if (inList.length() > 0) {
                inList.append(",");
            }
            inList.append("(?,?)");
        }
        
        // query is a negative outer join so we only pick the rows from v for which
        // there is no row found in ctv.
        final String paramListStr = paramList.toString();
        doCommonTokenValuesUpsert(paramListStr, tokenValues);
        
        // Now grab the ids for the rows we just created. If we had a RETURNING implementation
        // which worked reliably across all our database platforms, we wouldn't need this
        // second query.
        // Derby doesn't support IN LISTS with multiple members, so we have to join against
        // a VALUES again. No big deal...probably similar amount of work for the database
        StringBuilder select = new StringBuilder();
        select.append("     SELECT ctv.code_system_id, ctv.token_value, ctv.common_token_value_id FROM ");
        select.append("     (VALUES ").append(paramListStr).append(" ) AS v(token_value, code_system_id) ");
        select.append("       JOIN common_token_values ctv ");
        select.append("              ON ctv.token_value = v.token_value ");
        select.append("             AND ctv.code_system_id = v.code_system_id ");
        
        // Grab the ids
        Map<CommonTokenValue, Long> idMap = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(select.toString())) {
            int a = 1;
            for (CommonTokenValue tv: tokenValues) {
                ps.setString(a++, tv.getTokenValue());
                ps.setInt(a++, tv.getCodeSystemId());
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // code_system_id, token_value
                CommonTokenValue key = new CommonTokenValue(rs.getInt(1), rs.getString(2));
                idMap.put(key, rs.getLong(3));
            }
        } catch (SQLException x) {
            throw translator.translate(x);
        }
        
        // Now update the ids for all the matching systems in our list
        for (ResourceTokenValueRec xr: values) {
            // ignore entries with null tokenValue elements - we don't store them in common_token_values
            if (xr.getTokenValue() != null) {
                CommonTokenValue key = new CommonTokenValue(xr.getCodeSystemValueId(), xr.getTokenValue());
                Long id = idMap.get(key);
                if (id != null) {
                    xr.setCommonTokenValueId(id);
    
                    // update the thread-local cache with this id. The values aren't committed to the shared cache
                    // until the transaction commits
                    cache.addTokenValue(key, id);
                }
            }
        }
    }

    /**
     * Execute the insert (upsert) into the common_token_values table for the
     * given collection of values. Note, this insert from negative outer join
     * requires the database concurrency implementation to be correct. This does
     * not work for Postgres, hence Postgres gets its own implementation of this
     * method
     * @param paramList
     * @param tokenValues
     */
    protected void doCommonTokenValuesUpsert(String paramList, Collection<CommonTokenValue> tokenValues) {
        StringBuilder insert = new StringBuilder();
        insert.append("INSERT INTO common_token_values (token_value, code_system_id) ");
        insert.append("     SELECT v.token_value, v.code_system_id FROM ");
        insert.append("     (VALUES ").append(paramList).append(" ) AS v(token_value, code_system_id) ");
        insert.append(" LEFT OUTER JOIN common_token_values ctv ");
        insert.append("              ON ctv.token_value = v.token_value ");
        insert.append("             AND ctv.code_system_id = v.code_system_id ");
        insert.append("      WHERE ctv.token_value IS NULL ");
        
        // Note, we use PreparedStatement here on purpose. Partly because it's
        // secure coding best practice, but also because many resources will have the
        // same number of parameters, and hopefully we'll therefore share a small subset
        // of statements for better performance. Although once the cache warms up, this
        // shouldn't be called at all.
        try (PreparedStatement ps = connection.prepareStatement(insert.toString())) {
            // bind all the name values as parameters
            int a = 1;
            for (CommonTokenValue tv: tokenValues) {
                ps.setString(a++, tv.getTokenValue());
                ps.setInt(a++, tv.getCodeSystemId());
            }
            
            ps.executeUpdate();
        } catch (SQLException x) {
            StringBuilder values = new StringBuilder();
            for (CommonTokenValue tv: tokenValues) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append("{");
                values.append(tv.getTokenValue());
                values.append(",");
                values.append(tv.getCodeSystemId());
                values.append("}");
            }
            
            logger.log(Level.SEVERE, insert.toString() + "; [" + values.toString() + "]", x);
            throw translator.translate(x);
        }
    }

    @Override
    public void persist(Collection<ResourceTokenValueRec> records) {
        // Grab the ids for all the code-systems, and upsert any misses
        List<ResourceTokenValueRec> systemMisses = new ArrayList<>();
        cache.resolveCodeSystems(records, systemMisses);
        upsertCodeSystems(systemMisses);

        // Now that all the code-systems ids are known, we can search the cache
        // for all the token values, upserting anything new
        List<ResourceTokenValueRec> valueMisses = new ArrayList<>();
        cache.resolveTokenValues(records, valueMisses);
        upsertCommonTokenValues(valueMisses);

        // Now split the records into groups based on resource type.
        Map<String,List<ResourceTokenValueRec>> recordMap = new HashMap<>();
        for (ResourceTokenValueRec rtv: records) {
            List<ResourceTokenValueRec> list = recordMap.computeIfAbsent(rtv.getResourceType(), k -> { return new ArrayList<>(); });
            list.add(rtv);
        }
        
        for (Map.Entry<String, List<ResourceTokenValueRec>> entry: recordMap.entrySet()) {
            insertResourceTokenRefs(entry.getKey(), entry.getValue());
        }
    }
}
