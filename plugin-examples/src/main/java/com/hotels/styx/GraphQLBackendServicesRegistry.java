/*
  Copyright (C) 2013-2019 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.hotels.styx.api.Environment;
import com.hotels.styx.api.Resource;
import com.hotels.styx.api.configuration.Configuration;
import com.hotels.styx.api.configuration.ConfigurationException;
import com.hotels.styx.api.extension.service.BackendService;
import com.hotels.styx.api.extension.service.spi.AbstractStyxService;
import com.hotels.styx.api.extension.service.spi.Registry;
//import com.hotels.styx.infrastructure.FileBackedRegistry;
//import com.hotels.styx.proxy.backends.file.FileChangeMonitor.FileMonitorSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import static com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Throwables.propagate;
import static com.hotels.styx.api.extension.service.spi.Registry.Outcome.FAILED;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * File backed {@link BackendService} registry.
 */
public class GraphQLBackendServicesRegistry extends AbstractStyxService implements Registry<BackendService> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLBackendServicesRegistry.class);
    private final GraphQLBackedRegistry<BackendService> graphQLBackedRegistry;
    //private final FileMonitor fileChangeMonitor;

    @VisibleForTesting
    GraphQLBackendServicesRegistry(GraphQLBackedRegistry<BackendService> graphQLBackedRegistry) {
        super("GraphQLBackendServicesRegistry");
        this.graphQLBackedRegistry = requireNonNull(graphQLBackedRegistry);
       // this.fileChangeMonitor = requireNonNull(fileChangeMonitor);
    }

    @VisibleForTesting
    GraphQLBackendServicesRegistry() {
        this(new GraphQLBackedRegistry<BackendService>());
    }

    // Only used in OriginsHandlerTest, we need to refactor that test, since it should mock the registry instead
    public static GraphQLBackendServicesRegistry create(String originsFile) {
        return new GraphQLBackendServicesRegistry();
    }

    @Override
    public Registry<BackendService> addListener(ChangeListener<BackendService> changeListener) {
        return this.graphQLBackedRegistry.addListener(changeListener);
    }

    @Override
    public Registry<BackendService> removeListener(ChangeListener<BackendService> changeListener) {
        return this.graphQLBackedRegistry.removeListener(changeListener);
    }

    @Override
    public CompletableFuture<ReloadResult> reload() {
        return this.graphQLBackedRegistry.reload()
                .thenApply(outcome -> logReloadAttempt("Admin Interface", outcome));
    }

    @Override
    public Iterable<BackendService> get() {
        return this.graphQLBackedRegistry.get();
    }

    @Override
    protected CompletableFuture<Void> startService() {
        try {
            //fileChangeMonitor.start(this);
        } catch (Exception e) {
            CompletableFuture<Void> x = new CompletableFuture<>();
            x.completeExceptionally(e);
            return x;
        }
        return this.graphQLBackedRegistry.reload()
                .thenApply(result -> logReloadAttempt("Initial load", result))
                .thenAccept(result -> {
                    if (result.outcome() == FAILED) {
                        throw new RuntimeException(result.cause().orElse(null));
                    }
                });
    }

    @Override
    public CompletableFuture<Void> stop() {
        return null;
   //     return super.stop();
    }

    @VisibleForTesting
 /*   FileMonitor monitor() {
        return fileChangeMonitor;
    }*/

    //@Override
    public void fileChanged() {
        this.graphQLBackedRegistry.reload()
                .thenApply(outcome -> logReloadAttempt("File Monitor", outcome));
    }

    private ReloadResult logReloadAttempt(String reason, ReloadResult outcome) {

        return outcome;
    }

    /**
     * Factory for creating a {@link FileBackedBackendServicesRegistry}.
     */
    public static class Factory implements Registry.Factory<BackendService> {

        @Override
        public Registry<BackendService> create(Environment environment, Configuration registryConfiguration) {
            String originsFile = registryConfiguration.get("originsFile", String.class)
                    .map(Factory::requireNonEmpty)
                    .orElseThrow(() -> new ConfigurationException(
                            "missing [services.registry.factory.config.originsFile] config value for factory class FileBackedBackendServicesRegistry.Factory"));

           /* FileMonitorSettings monitorSettings = registryConfiguration.get("monitor", FileMonitorSettings.class)
                    .orElseGet(FileMonitorSettings::new);*/

            return registry();
        }

        private static Registry<BackendService> registry() {
            //requireNonEmpty(originsFile);

            // Hack up a disabled file monitor for now
            //FileMonitor monitor = FileMonitor.DISABLED;
            //Resource resource = newResource(originsFile);
            // Dont really need a resource for now.. figure this out later for configuring a graphql server
            return new GraphQLBackendServicesRegistry();
        }

        private static String requireNonEmpty(String originsFile) {
            if (originsFile.isEmpty()) {
                throw new ConfigurationException("empty [services.registry.factory.config.originsFile] config value for factory class FileBackedBackendServicesRegistry.Factory");
            } else {
                return originsFile;
            }
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("originsFileName", graphQLBackedRegistry.fileName())
                .toString();
    }
}
