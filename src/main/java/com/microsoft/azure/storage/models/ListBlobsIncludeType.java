/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 * Changes may cause incorrect behavior and will be lost if the code is
 * regenerated.
 */

package com.microsoft.azure.storage.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines values for ListBlobsIncludeType.
 */
public enum ListBlobsIncludeType {
    /** Enum value snapshots. */
    SNAPSHOTS("snapshots"),

    /** Enum value metadata. */
    METADATA("metadata"),

    /** Enum value uncommittedblobs. */
    UNCOMMITTEDBLOBS("uncommittedblobs"),

    /** Enum value copy. */
    COPY("copy");

    /** The actual serialized value for a ListBlobsIncludeType instance. */
    private String value;

    ListBlobsIncludeType(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a ListBlobsIncludeType instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed ListBlobsIncludeType object, or null if unable to parse.
     */
    @JsonCreator
    public static ListBlobsIncludeType fromString(String value) {
        ListBlobsIncludeType[] items = ListBlobsIncludeType.values();
        for (ListBlobsIncludeType item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
