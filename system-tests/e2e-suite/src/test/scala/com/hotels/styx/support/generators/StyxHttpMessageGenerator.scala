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
package com.hotels.styx.support.generators

import java.nio.charset.StandardCharsets.UTF_8

import com.hotels.styx.api.HttpHeaderNames.CONTENT_LENGTH
import com.hotels.styx.api.{HttpRequest, LiveHttpRequest}
import com.hotels.styx.api.HttpMethod._
import com.hotels.styx.api.HttpVersion._
import com.hotels.styx.support.generators.HttpHeadersGenerator.{HeaderTuple, contentTypeCharset, httpHeaders}
import com.hotels.styx.support.generators.UrlGenerator.genUrl
import org.scalacheck.Gen

class StyxHttpMessageGenerator {

  val maxContentChunkLength = 2000

  def nettyHttpRequest: Gen[LiveHttpRequest] = for {
    method <- Gen.oneOf(GET, POST, PUT, HEAD, CONNECT, DELETE, OPTIONS, PATCH, TRACE)
    version <- Gen.oneOf(HTTP_1_0, HTTP_1_1)
    uri <- genUrl
    headers <- httpHeaders
    contentDataLen <- Gen.choose(0, maxContentChunkLength)
    contentData <- Gen.listOfN(contentDataLen, Gen.alphaChar).map(_.mkString)
  } yield {
    val builder = new HttpRequest.Builder(method, uri).version(version)
    if (method != HEAD) {
      addContent(builder, headers, contentData)
    }
    addHeaders(builder, headers)
    builder.build().stream
  }

  def addHeaders(builder: HttpRequest.Builder, headers: List[HeaderTuple]) = {
    for (header <- headers) {
      builder.addHeader(header._1, header._2)
    }
  }

  def addContent(builder: HttpRequest.Builder, headers: List[HeaderTuple], content: String): Unit = {
    val charset: String = contentTypeCharset(headers)
    builder.body(content, UTF_8)
    builder.addHeader(CONTENT_LENGTH, content.getBytes(charset).length)
  }

}
