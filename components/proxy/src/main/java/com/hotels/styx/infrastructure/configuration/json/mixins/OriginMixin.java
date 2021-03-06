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
package com.hotels.styx.infrastructure.configuration.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson annotations for {@link com.hotels.styx.api.extension.Origin}.
 */
public abstract class OriginMixin {

    @JsonCreator
    OriginMixin(@JsonProperty("id") String originId,
           @JsonProperty("host") String host) {
    }

    @JsonProperty("host")
    public abstract String hostAndPortString();

    @JsonProperty("id")
    public abstract String idAsString();
}
