/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.fhir.audit.environment;

import static org.testng.Assert.assertEquals;

import java.util.Properties;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ibm.fhir.audit.configuration.type.KafkaType;
import com.ibm.fhir.config.PropertyGroup;

/**
 * Tests Kafka Environment
 */
public class KafkaEnvironmentTest {
    private static final JsonBuilderFactory BUILDER_FACTORY = Json.createBuilderFactory(null);
    private JsonObject jsonObjNoKafka = null;
    private JsonObject jsonObjKafka = null;
    private JsonObject jsonObjKafkaWithoutEntries = null;

    @BeforeClass
    public void setup() {
        // Build a JSON object for testing.
        jsonObjNoKafka = BUILDER_FACTORY.createObjectBuilder().build();

        //@formatter:off
        jsonObjKafka = BUILDER_FACTORY.createObjectBuilder()
                            .add("kafka",
                                BUILDER_FACTORY.createObjectBuilder()
                                    .add("level2a","level2aprop")
                                    .add("level2b","level2bprop")
                                    .build())
                        .build();
        //@formatter:on

        //@formatter:off
        jsonObjKafkaWithoutEntries = BUILDER_FACTORY.createObjectBuilder()
                            .add("kafka",
                                BUILDER_FACTORY.createObjectBuilder()
                                    .build())
                        .build();
        //@formatter:on
    }

    @Test(expectedExceptions= {IllegalArgumentException.class})
    public void testNullCase() throws Exception {
        KafkaType.getEnvironment(null);
    }


    @Test(expectedExceptions= {IllegalArgumentException.class})
    public void testNullCaseWithoutKafka() throws Exception {
        KafkaType.getEnvironment(new PropertyGroup(jsonObjNoKafka));
    }

    @Test
    public void testCaseWithKafka() throws Exception {
        Properties props = KafkaType.getEnvironment(new PropertyGroup(jsonObjKafka));
        assertEquals(props.size(),4);
    }

    @Test
    public void testCaseWithoutEntriesForKafka() throws Exception {
        Properties props = KafkaType.getEnvironment(new PropertyGroup(jsonObjKafkaWithoutEntries));
        assertEquals(props.size(),2);
    }
}