/*
 * (C) Copyright IBM Corp. 2019, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.model.type.code;

import com.ibm.fhir.model.annotation.System;
import com.ibm.fhir.model.type.Code;
import com.ibm.fhir.model.type.Extension;
import com.ibm.fhir.model.type.String;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Generated;

@System("http://hl7.org/fhir/guide-page-generation")
@Generated("com.ibm.fhir.tools.CodeGenerator")
public class GuidePageGeneration extends Code {
    /**
     * HTML
     * 
     * <p>Page is proper xhtml with no templating. Will be brought across unchanged for standard post-processing.
     */
    public static final GuidePageGeneration HTML = GuidePageGeneration.builder().value(ValueSet.HTML).build();

    /**
     * Markdown
     * 
     * <p>Page is markdown with templating. Will use the template to create a file that imports the markdown file prior to 
     * post-processing.
     */
    public static final GuidePageGeneration MARKDOWN = GuidePageGeneration.builder().value(ValueSet.MARKDOWN).build();

    /**
     * XML
     * 
     * <p>Page is xml with templating. Will use the template to create a file that imports the source file and run the 
     * nominated XSLT transform (see parameters) if present prior to post-processing.
     */
    public static final GuidePageGeneration XML = GuidePageGeneration.builder().value(ValueSet.XML).build();

    /**
     * Generated
     * 
     * <p>Page will be generated by the publication process - no source to bring across.
     */
    public static final GuidePageGeneration GENERATED = GuidePageGeneration.builder().value(ValueSet.GENERATED).build();

    private volatile int hashCode;

    private GuidePageGeneration(Builder builder) {
        super(builder);
    }

    public ValueSet getValueAsEnumConstant() {
        return (value != null) ? ValueSet.from(value) : null;
    }

    /**
     * Factory method for creating GuidePageGeneration objects from a passed enum value.
     */
    public static GuidePageGeneration of(ValueSet value) {
        switch (value) {
        case HTML:
            return HTML;
        case MARKDOWN:
            return MARKDOWN;
        case XML:
            return XML;
        case GENERATED:
            return GENERATED;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    /**
     * Factory method for creating GuidePageGeneration objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static GuidePageGeneration of(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    /**
     * Inherited factory method for creating GuidePageGeneration objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static String string(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    /**
     * Inherited factory method for creating GuidePageGeneration objects from a passed string value.
     * 
     * @param value
     *     A string that matches one of the allowed code values
     * @throws IllegalArgumentException
     *     If the passed string cannot be parsed into an allowed code value
     */
    public static Code code(java.lang.String value) {
        return of(ValueSet.from(value));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        GuidePageGeneration other = (GuidePageGeneration) obj;
        return Objects.equals(id, other.id) && Objects.equals(extension, other.extension) && Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        int result = hashCode;
        if (result == 0) {
            result = Objects.hash(id, extension, value);
            hashCode = result;
        }
        return result;
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.id(id);
        builder.extension(extension);
        builder.value(value);
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Code.Builder {
        private Builder() {
            super();
        }

        @Override
        public Builder id(java.lang.String id) {
            return (Builder) super.id(id);
        }

        @Override
        public Builder extension(Extension... extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder extension(Collection<Extension> extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder value(java.lang.String value) {
            return (value != null) ? (Builder) super.value(ValueSet.from(value).value()) : this;
        }

        public Builder value(ValueSet value) {
            return (value != null) ? (Builder) super.value(value.value()) : this;
        }

        @Override
        public GuidePageGeneration build() {
            return new GuidePageGeneration(this);
        }
    }

    public enum ValueSet {
        /**
         * HTML
         * 
         * <p>Page is proper xhtml with no templating. Will be brought across unchanged for standard post-processing.
         */
        HTML("html"),

        /**
         * Markdown
         * 
         * <p>Page is markdown with templating. Will use the template to create a file that imports the markdown file prior to 
         * post-processing.
         */
        MARKDOWN("markdown"),

        /**
         * XML
         * 
         * <p>Page is xml with templating. Will use the template to create a file that imports the source file and run the 
         * nominated XSLT transform (see parameters) if present prior to post-processing.
         */
        XML("xml"),

        /**
         * Generated
         * 
         * <p>Page will be generated by the publication process - no source to bring across.
         */
        GENERATED("generated");

        private final java.lang.String value;

        ValueSet(java.lang.String value) {
            this.value = value;
        }

        /**
         * @return
         *     The java.lang.String value of the code represented by this enum
         */
        public java.lang.String value() {
            return value;
        }

        /**
         * Factory method for creating GuidePageGeneration.ValueSet values from a passed string value.
         * 
         * @param value
         *     A string that matches one of the allowed code values
         * @throws IllegalArgumentException
         *     If the passed string cannot be parsed into an allowed code value
         */
        public static ValueSet from(java.lang.String value) {
            for (ValueSet c : ValueSet.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }
}
