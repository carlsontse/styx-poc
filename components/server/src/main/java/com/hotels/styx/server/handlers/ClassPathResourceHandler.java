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
package com.hotels.styx.server.handlers;

import com.google.common.io.ByteStreams;
import com.hotels.styx.api.Buffer;
import com.hotels.styx.api.ByteStream;
import com.hotels.styx.api.HttpResponse;
import com.hotels.styx.api.LiveHttpRequest;
import com.hotels.styx.api.LiveHttpResponse;
import com.hotels.styx.api.HttpResponseStatus;
import com.hotels.styx.common.http.handler.BaseHttpHandler;
import reactor.core.publisher.Flux;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static com.hotels.styx.api.HttpHeaderNames.CONTENT_TYPE;
import static com.hotels.styx.api.HttpResponseStatus.FORBIDDEN;
import static com.hotels.styx.api.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static com.hotels.styx.api.HttpResponseStatus.NOT_FOUND;
import static com.hotels.styx.api.HttpResponseStatus.OK;
import static com.hotels.styx.server.handlers.MediaTypes.mediaTypeOf;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handler for class path resources.
 */
public class ClassPathResourceHandler extends BaseHttpHandler {
    private final String root;

    public ClassPathResourceHandler(String root) {
        this.root = ensureRootEndsInSlash(root);
    }

    private static String ensureRootEndsInSlash(String root) {
        return root.endsWith("/") ? root : root + "/";
    }

    @Override
    protected LiveHttpResponse doHandle(LiveHttpRequest request) {
        try {
            String path = request.path();

            if (!path.startsWith(root) || path.contains("..")) {
                return error(FORBIDDEN);
            }

            return new HttpResponse.Builder(OK)
                    .body(resourceBody(path), true)
                    .header(CONTENT_TYPE, mediaTypeOf(path))
                    .build()
                    .stream();
        } catch (FileNotFoundException e) {
            return error(NOT_FOUND);
        } catch (IOException e) {
            return error(INTERNAL_SERVER_ERROR);
        }
    }

    private static byte[] resourceBody(String path) throws IOException {
        try (InputStream stream = classPathResourceAsStream(path)) {
            return readStream(stream);
        }
    }

    private static LiveHttpResponse error(HttpResponseStatus status) {
        return new LiveHttpResponse.Builder(status)
                .body(new ByteStream(Flux.just(new Buffer(status.description(), UTF_8))))
                .build();
    }

    private static InputStream classPathResourceAsStream(String resource) throws FileNotFoundException {
        InputStream stream = ClassPathResourceHandler.class.getResourceAsStream(resource);

        if (stream == null) {
            throw new FileNotFoundException(resource);
        }

        return stream;
    }

    private static byte[] readStream(InputStream stream) throws IOException {
        return ByteStreams.toByteArray(stream);
    }
}
