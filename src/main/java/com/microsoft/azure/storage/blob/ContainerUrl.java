/**
 * Copyright Microsoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.azure.storage.blob;

import com.microsoft.azure.storage.models.ContainerGetPropertiesHeaders;
import com.microsoft.azure.storage.pipeline.Pipeline;
import com.microsoft.rest.v2.RestResponse;
import rx.Single;
import rx.functions.Func1;

/**
 * Represents a URL to the Azure Storage container allowing you to manipulate its blobs.
 */
public final class ContainerUrl extends StorageUrl {

    public ContainerUrl(String url, Pipeline pipeline) {
        super(url, pipeline);
    }

    /**
     * Create creates a new container within a storage account.
     * If a container with the same name already exists, the operation fails.
     * For more information, see https://docs.microsoft.com/rest/api/storageservices/create-container.
     * @param containerAccessConditions
     * @return
     */
    public Single<Void> createAsync(ContainerAccessConditions containerAccessConditions) {
        return this.storageClient.containers().createAsync(this.url, containerAccessConditions);
    }

    /**
     * GetPropertiesAndMetadata returns the container's metadata and system properties.
     * For more information, see https://docs.microsoft.com/rest/api/storageservices/get-container-metadata.
     * @return
     */
    public Single<ContainerGetPropertiesHeaders> getPropertiesAndMetadataAsync() {
        Single<RestResponse<ContainerGetPropertiesHeaders, Void>> restResponse = this.storageClient.containers().getPropertiesWithRestResponseAsync(this.url);

        return restResponse.map(new Func1<RestResponse<ContainerGetPropertiesHeaders, Void>, ContainerGetPropertiesHeaders>() {
            public ContainerGetPropertiesHeaders call(RestResponse<ContainerGetPropertiesHeaders, Void> restResponse) {
                return restResponse.headers();
            }
        });
    }

    /**
     * Delete marks the specified container for deletion. The container and any blobs contained within it are later deleted during garbage collection.
     * For more information, see https://docs.microsoft.com/rest/api/storageservices/delete-container.
     * @return
     */
    public Single<Void> deleteAsync() {
        return this.storageClient.containers().deleteAsync(this.url);
    }

    /**
     * Creates a new {@link BlockBlobUrl} object by concatenating the blobName to the end of
     * ContainerUrl's URL. The new BlockBlobUrl uses the same request policy pipeline as the ContainerUrl.
     * To change the pipeline, create the BlockBlobUrl and then call its WithPipeline method passing in the
     * desired pipeline object. Or, call this package's NewBlockBlobUrl instead of calling this object's
     * NewBlockBlobUrl method.
     * @param blobName
     * @return
     */
    public BlockBlobUrl createBlockBlobUrl(String blobName) {
        return new BlockBlobUrl(this.url, blobName, this.storageClient.restClient());
    }

    /**
     * Creates a new {@link ContainerUrl} with the given pipeline.
     * @param pipeline
     *      A {@link Pipeline} object to set.
     * @return
     *      A {@link ContainerUrl} object with the given pipeline.
     */
    public ContainerUrl withPipeline(Pipeline pipeline) {
        return new ContainerUrl(this.url, pipeline);
    }
}
