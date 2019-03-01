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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.HashCode;
import com.hotels.styx.api.Identifiable;
import com.hotels.styx.api.Resource;
import com.hotels.styx.api.extension.Origin;
import com.hotels.styx.api.extension.service.BackendService;
import com.hotels.styx.api.extension.service.ConnectionPoolSettings;
import com.hotels.styx.api.extension.service.spi.AbstractRegistry;
import io.netty.util.Timeout;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.hash.HashCode.fromLong;
import static com.google.common.hash.Hashing.md5;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.hotels.styx.api.extension.service.spi.Registry.ReloadResult.failed;
import static com.hotels.styx.api.extension.service.spi.Registry.ReloadResult.reloaded;
import static com.hotels.styx.api.extension.service.spi.Registry.ReloadResult.unchanged;
import static java.lang.String.format;
import static java.nio.file.Files.getLastModifiedTime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * File backed registry for {@code T}.
 *
 * @param <T> type of the resource
 */
public class GraphQLBackedRegistry<T extends Identifiable> extends AbstractRegistry<T> {
    private static final Logger LOG = getLogger(GraphQLBackedRegistry.class);
    private final Resource configurationFile;
    private final Reader<T> reader;
    private final Supplier<FileTime> modifyTimeSupplier;
    private HashCode fileHash = fromLong(0);

    public GraphQLBackedRegistry() {
        configurationFile = null;
        reader = null;
        modifyTimeSupplier = null;
        /*super(resourceConstraint);
        this.configurationFile = requireNonNull(configurationFile);
        this.reader = requireNonNull(reader);
        this.modifyTimeSupplier = modifyTimeSupplier;*/
    }

    public String fileName() {
        return configurationFile.absolutePath();
    }
    @Override
    public CompletableFuture<ReloadResult> reload() {
        return supplyAsync(() -> {
            // hack it up for now


//Execute and get the response.
            HttpResponse response = null;
            JSONObject jsonObject = null;
            try {
                HttpClient httpclient = HttpClients.createDefault();

                URIBuilder builder = new URIBuilder();
                LOG.info("Connecting to graphql: 192.168.99.100:4000");
                builder.setScheme("http").setHost("192.168.99.100:4000").setPath("/graphql")
                        .setParameter("query", "{routes {id path upstream{uri}}}");

                URI uri = builder.build();
                HttpGet httpGet = new HttpGet(uri);

                response = httpclient.execute(httpGet);
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    try (InputStream inputStream = entity.getContent()) {

                        JSONParser jsonParser = new JSONParser();
                        jsonObject = (JSONObject)jsonParser.parse(
                                new InputStreamReader(inputStream, "UTF-8"));

                    }
                }
            } catch(Exception ex) {
                LOG.error("something bad happened", ex);
            }

            JSONObject data = (JSONObject)jsonObject.get("data");
            JSONArray routes = (JSONArray) data.get("routes");

            ArrayList<BackendService> resources = new ArrayList<>();

            for (int i=0; i < routes.size(); i++) {
                JSONObject route = (JSONObject) routes.get(i);
                JSONObject upstream = (JSONObject) route.get("upstream");

                resources.add(
                        new BackendService.Builder()
                                .id((String)route.get("id"))
                                .path((String)route.get("path"))
                                .connectionPoolConfig(new ConnectionPoolSettings.Builder()
                                        .maxConnectionsPerHost(45)
                                        .maxPendingConnectionsPerHost(15)
                                        .connectTimeout(100, TimeUnit.MILLISECONDS)
                                        .pendingConnectionTimeout(60000, TimeUnit.MILLISECONDS)
                                        .build()
                                )
                                .responseTimeoutMillis(60000)
                                // this should really be doing some parsing of the URI upstream
                                .origins(Origin.newOriginBuilder((String)upstream.get("uri"), 9090).id((String)route.get("id")).build())
                                .build());
            }

            Iterable<T> iterable = (Iterable<T>)resources;

            Changes<T> changes = changes(iterable, get());

            if (!changes.isEmpty()) {
                set(iterable);
            }
            return reloaded("fetched new results!");
        }, newSingleThreadExecutor());
    }

    private static Supplier<FileTime> fileModificationTimeProvider(Resource path) {
        return () -> {
            try {
                return getLastModifiedTime(Paths.get(path.path()));
            } catch (Throwable cause) {
                throw new RuntimeException(cause);
            }
        };
    }

    private Optional<FileTime> fileModificationTime() {
        try {
            return Optional.of(modifyTimeSupplier.get());
        } catch (Throwable cause) {
            return Optional.empty();
        }
    }

    private boolean updateResources(byte[] content, HashCode hashCode) {
        Iterable<T> resources = reader.read(content);
        Changes<T> changes = changes(resources, get());

        if (!changes.isEmpty()) {
            set(resources);
        }

        fileHash = hashCode;
        return !changes.isEmpty();
    }

    private byte[] readFile() {
        try (InputStream configurationContent = configurationFile.inputStream()) {
            return toByteArray(configurationContent);
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    /**
     * Reader.
     *
     * @param <T>
     */
    public interface Reader<T> {
        Iterable<T> read(byte[] content);
    }
}
