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
package com.hotels.styx.infrastructure.configuration.yaml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Object field - a type of path element.
 */
final class ObjectField implements PathElement {
    private final String name;

    public ObjectField(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public void setChild(JsonNode parent, JsonNode child) {
        ((ObjectNode) parent).set(name, child);
    }

    @Override
    public JsonNode child(JsonNode parent) {
        return parent.get(name);
    }

    @Override
    public boolean isArrayIndex() {
        return false;
    }

    @Override
    public boolean isObjectField() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }
}
