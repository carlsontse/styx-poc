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
package loadtest.plugins;

import com.hotels.styx.api.LiveHttpRequest;
import com.hotels.styx.api.LiveHttpResponse;
import com.hotels.styx.api.Eventual;
import com.hotels.styx.api.plugins.spi.Plugin;
import com.hotels.styx.api.plugins.spi.PluginFactory;

import java.util.concurrent.CompletableFuture;

import static com.hotels.styx.common.CompletableFutures.fromSingleObservable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.Observable.timer;

public class AsyncRequestContentDecoderPluginFactory implements PluginFactory {

    @Override
    public Plugin create(PluginFactory.Environment environment) {
        AsyncPluginConfig config = environment.pluginConfig(AsyncPluginConfig.class);
        return new AsyncRequestContentDecoder(config);
    }

    private static class AsyncRequestContentDecoder extends AbstractTestPlugin {
        private final AsyncPluginConfig config;

        AsyncRequestContentDecoder(AsyncPluginConfig config) {
            this.config = config;
        }

        @Override
        public Eventual<LiveHttpResponse> intercept(LiveHttpRequest request, Chain chain) {
            return request.aggregate(config.maxContentLength())
                            .flatMap(fullHttpRequest -> Eventual.from(asyncOperation(config.delayMillis())))
                            .map(outcome -> request.newBuilder().header("X-Outcome", outcome.result()))
                            .flatMap(x -> chain.proceed(request));
        }
    }

    private static CompletableFuture<Outcome> asyncOperation(long delay) {
        return fromSingleObservable(timer(delay, MILLISECONDS)).thenApply(x -> new Outcome());
    }

    private static class Outcome {
        int result() {
            return 1;
        }
    }

}
