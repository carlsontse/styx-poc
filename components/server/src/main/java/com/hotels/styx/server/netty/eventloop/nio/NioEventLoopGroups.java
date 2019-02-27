/*
  Copyright (C) 2013-2018 Expedia Inc.

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
package com.hotels.styx.server.netty.eventloop.nio;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Creates NioEventLoopGroup instances.
 */
public class NioEventLoopGroups {
    public static EventLoopGroup newEventLoopGroup(int threadsCount, String threadsNameFormat) {
        return new NioEventLoopGroup(threadsCount, new ThreadFactoryBuilder()
                .setNameFormat(threadsNameFormat)
                .build());
    }
}
