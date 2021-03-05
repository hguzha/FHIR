/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.term.graph.loader;

import java.util.Map;

import org.apache.commons.cli.Options;

public interface FHIRTermGraphLoader {
    enum Type {
        CODESYSTEM {
            @Override
            public Options options() {
                return new Options()
                    .addRequiredOption("config", null, true, "Configuration properties file")
                    .addOption("url", null, true, "CodeSystem url")
                    .addOption("file", null, true, "CodeSystem file");
            }
        },
        SNOMED {
            @Override
            public Options options() {
                return new Options()
                    .addRequiredOption("config", null, true, "Configuration properties file")
                    .addRequiredOption("base", null, true, "SNOMED-CT base directory")
                    .addRequiredOption("concept", null, true, "SNOMED-CT concept file")
                    .addRequiredOption("relation", null, true, "SNOMED-CT relationship file")
                    .addRequiredOption("desc", null, true, "SNOMED-CT description file")
                    .addRequiredOption("lang", null, true, "SNOMED-CT language refset file")
                    .addOption("labels", null, true, "labels");
            }
        },
        UMLS {
            @Override
            public Options options() {
                return new Options()
                    .addRequiredOption("config", null, true, "Configuration properties file")
                    .addRequiredOption("base", null, true, "UMLS base directory")
                    .addOption("labels", null, true, "labels");
            }
        };

        public abstract Options options();
    }
    void load();
    void close();
    Map<String, String> options();
}
