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

import com.microsoft.azure.storage.pipeline.IRequestPolicyFactory;
import com.microsoft.azure.storage.pipeline.Pipeline;
import com.microsoft.azure.storage.pipeline.PipelineLogger;
import com.microsoft.azure.storage.pipeline.RequestPolicyNode;
import com.microsoft.rest.v2.http.HttpRequest;
import com.microsoft.rest.v2.http.HttpResponse;
import com.microsoft.rest.v2.policy.RequestPolicy;
import io.netty.handler.codec.http.HttpResponseStatus;
//import org.apache.log4j.Level;
import rx.Single;
import rx.functions.Action1;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Factory for logging requests and responses
 */
public final class RequestLoggingFactory implements IRequestPolicyFactory {

    private final RequestLoggingOptions requestLoggingOptions;

    private int tryCount;

    private long operationStartTime;

    public RequestLoggingFactory(RequestLoggingOptions requestLoggingOptions) {
        this.requestLoggingOptions = requestLoggingOptions;
        //PipelineLogger.initialize(this.requestLoggingOptions.getLoggingLevel());
    }

    private final class RequestLoggingPolicy implements RequestPolicy {

        private final RequestPolicyNode requestPolicyNode;

        final private AtomicReference<RequestLoggingFactory> factory;

        private AtomicLong requestStartTime;

        RequestLoggingPolicy(RequestPolicyNode requestPolicyNode, RequestLoggingFactory factory) {
            this.requestPolicyNode = requestPolicyNode;
            this.factory = new AtomicReference<>(factory);
        }

        /**
         * Signed the request
         * @param request
         *      the request to sign
         * @return
         *      A {@link Single} representing the HTTP response that will arrive asynchronously.
         */
        @Override
        public Single<HttpResponse> sendAsync(final HttpRequest request) {
            this.factory.get().tryCount++;
            this.requestStartTime.set(System.currentTimeMillis());
            if (this.factory.get().tryCount == 1) {
                this.factory.get().operationStartTime = requestStartTime.get();
            }

//            if (PipelineLogger.shouldLog(Level.INFO)) {
//                this.pipeline.log(Level.INFO, "'%s'==> OUTGOING REQUEST (Try number='%d')%n", request.url(), this.factory.get().tryCount);
//            }

            Single<HttpResponse> httpResponse = requestPolicyNode.sendAsync(request);
            return httpResponse;
//                    .doOnError(new Action1<Throwable>() {
//                        @Override
//                        public void call(Throwable throwable) {
//                            if (PipelineLogger.shouldLog(Level.SEVERE)) {
//                                //PipelineLogger.error("Unexpected failure attempting to make request.%nError message:'%s'%n", throwable.getMessage());
//                            }
//                        }
//                    })
//                    .doOnSuccess(new Action1<HttpResponse>() {
//                        @Override
//                        public void call(HttpResponse response) {
//                            if (!PipelineLogger.shouldLog(Level.SEVERE)) {
//                                return;
//                            }
//
//                            if (response.statusCode() >= 500 || (response.statusCode() >= 400 && response.statusCode() != 409 && response.statusCode() != 412)) {
//                                if (PipelineLogger.shouldLog(Level.SEVERE)) {
//                                    //PipelineLogger.error("HTTP request failed with status code:'%d'%n", response.statusCode());
//                                }
//                            }
//                            if (PipelineLogger.shouldLog(Level.INFO)) {
//                                RequestLoggingFactory loggingFactory = factory.get();
//                                long requestEndTime = System.nanoTime();
//                                long requestDuration = requestEndTime - requestStartTime.get();
//                                long operationDuration = requestEndTime - factory.get().operationStartTime;
//                                pipeline.log(Level.INFO, "Request try:'%d', request duration:'%d' ms, operation duration:'%d'", loggingFactory.tryCount, requestDuration, operationDuration);
//                            }
//                        }
//                    });
        }
    }

    @Override
    public RequestPolicy create(RequestPolicyNode requestPolicyNode) {
        return new RequestLoggingPolicy(requestPolicyNode, this);
    }
}
