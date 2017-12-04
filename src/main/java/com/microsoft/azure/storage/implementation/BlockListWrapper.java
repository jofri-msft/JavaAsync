/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 * Changes may cause incorrect behavior and will be lost if the code is
 * regenerated.
 */

package com.microsoft.azure.storage.implementation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "BlockList")
public class BlockListWrapper {

    @JacksonXmlProperty(localName = "Latest")
    private final List<String> blockList;

    @JsonCreator
    public BlockListWrapper(@JsonProperty("blockList") List<String> blockList) {
        this.blockList = blockList;
    }

    /**
     * Get the BlockList value.
     *
     * @return the BlockList value
     */
    public List<String> blockList() {
        return blockList;
    }
}
