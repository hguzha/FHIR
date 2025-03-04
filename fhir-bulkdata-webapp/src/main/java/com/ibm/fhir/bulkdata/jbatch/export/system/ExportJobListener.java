/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.bulkdata.jbatch.export.system;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.listener.JobListener;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.spark.sql.SparkSession;

import com.ibm.fhir.bulkdata.jbatch.context.BatchContextAdapter;
import com.ibm.fhir.bulkdata.jbatch.export.data.ExportCheckpointUserData;
import com.ibm.fhir.exception.FHIRException;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationAdapter;
import com.ibm.fhir.operation.bulkdata.config.ConfigurationFactory;
import com.ibm.fhir.operation.bulkdata.model.type.BulkDataContext;

@Dependent
public class ExportJobListener implements JobListener {

    private static final Logger logger = Logger.getLogger(ExportJobListener.class.getName());

    long currentExecutionStartTimeInMS;

    private BulkDataContext ctx = null;

    @Inject
    JobContext jobCtx;

    public ExportJobListener() throws FHIRException {
        // No Operation
    }

    @Override
    public void beforeJob() throws Exception {
        currentExecutionStartTimeInMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        long executionId = -1;

        try {
            executionId = jobCtx.getExecutionId();
            JobOperator jobOperator = BatchRuntime.getJobOperator();
            JobExecution jobExecution = jobOperator.getJobExecution(executionId);

            BatchContextAdapter contextAdapter = new BatchContextAdapter(jobExecution.getJobParameters());
            ctx = contextAdapter.getStepContextForSystemChunkWriter();

            // Register the context to get the right configuration.
            ConfigurationAdapter adapter = ConfigurationFactory.getInstance();
            adapter.registerRequestContext(ctx.getTenantId(), ctx.getDatastoreId(), ctx.getIncomingUrl());

            if (adapter.isStorageProviderParquetEnabled(ctx.getSource())) {
                try {
                    Class.forName("org.apache.spark.sql.SparkSession");

                    // Create the global spark session
                    SparkSession.builder().appName("parquetWriter")
                        // local : Run Spark locally with one worker thread (i.e. no parallelism at all).
                        // local[*] : Run Spark locally with as many worker threads as logical cores on your machine.
                        .master("local[*]")
                        // this undocumented feature allows us to avoid a bunch of unneccessary dependencies and avoid
                        // launching the unnecessary SparkUI stuff, but there is some risk in using it as its
                        // undocumented.
                        .config("spark.ui.enabled", false).getOrCreate();
                } catch (ClassNotFoundException e) {
                    logger.warning("No SparkSession in classpath; skipping spark session initialization");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ExportJobListener: beforeJob failed job[" + executionId + "]", e);
            throw e;
        }
    }

    @Override
    public void afterJob() {
        long currentExecutionEndTimeInMS = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        long executionId = -1;

        try {
            executionId = jobCtx.getExecutionId();
            // Used for generating response for all the import data resources.

            JobOperator jobOperator = BatchRuntime.getJobOperator();
            long totalJobExecutionMilliSeconds = 0;
            // The job can be started, stopped and then started again, so we need to add them all to get the whole job
            // execution duration.
            for (JobExecution jobExecution : jobOperator.getJobExecutions(jobOperator.getJobInstance(jobCtx.getExecutionId()))) {
                // For current execution, jobExecution.getEndTime() is either null or with wrong value because the
                // current execution is not finished yet, so always use system time for both job execution start time and end time.
                if (jobExecution.getExecutionId() == jobCtx.getExecutionId()) {
                    totalJobExecutionMilliSeconds += (currentExecutionEndTimeInMS - currentExecutionStartTimeInMS);
                } else {
                    totalJobExecutionMilliSeconds += (jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime());
                }
            }

            @SuppressWarnings("unchecked")
            List<ExportCheckpointUserData> partitionSummaries = (List<ExportCheckpointUserData>) jobCtx.getTransientUserData();

            // If the job is stopped before any partition is finished, then nothing to show.
            if (partitionSummaries == null) {
                if(logger.isLoggable(Level.FINE)) {
                    logger.fine("Partition Summaries is null, therefore something wasn't processed");
                }
                return;
            }

            double jobProcessingSeconds = totalJobExecutionMilliSeconds / 1000.0;
            jobProcessingSeconds = jobProcessingSeconds < 1 ? 1.0 : jobProcessingSeconds;

            // log the simple metrics.
            logger.info(" ---- FHIR resources exported in " + jobProcessingSeconds + "seconds ----");
            logger.info("ResourceType \t| Exported");
            int totalExportedFhirResources = 0;
            List<String> resourceTypeSummaries = new ArrayList<>();
            for (ExportCheckpointUserData partitionSummary : partitionSummaries) {
                logger.info(partitionSummary.getResourceTypeSummary() + "\t|"
                        + partitionSummary.getTotalResourcesNum());
                resourceTypeSummaries.add(partitionSummary.getResourceTypeSummary());
                totalExportedFhirResources += partitionSummary.getTotalResourcesNum();
            }

            logger.info(" ---- Total: " + totalExportedFhirResources
                    + " ExportRate: " + new DecimalFormat("#0.00").format(totalExportedFhirResources / jobProcessingSeconds) + " ----");

            if (resourceTypeSummaries.size() > 0) {
                // e.g, Patient[1000,1000,200]:Observation[1000,250]
                jobCtx.setExitStatus(String.join(":", resourceTypeSummaries));
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "ExportJobListener: afterJob failed job[" + executionId + "]", e);
            throw e;
        }
    }
}