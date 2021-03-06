/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.body

import java.io.FileInputStream
import java.nio.charset.Charset

import io.gatling.commons.util.{ FastByteArrayInputStream, GzipHelper }

object BodyProcessors {

  def gzip: Body => ByteArrayBody =
    (body: Body) => {
      val gzippedBytes = body match {
        case StringBody(string, _)    => string.map(GzipHelper.gzip)
        case pebbleBody: PebbleBody   => pebbleBody.map(GzipHelper.gzip)
        case ByteArrayBody(byteArray) => byteArray.map(GzipHelper.gzip)
        case RawFileBody(resourceAndCachedBytes) =>
          resourceAndCachedBytes.map {
            case ResourceAndCachedBytes(resource, cachedBytes) =>
              cachedBytes match {
                case Some(bytes) => GzipHelper.gzip(bytes)
                case _           => GzipHelper.gzip(new FileInputStream(resource.file))
              }
          }
        case InputStreamBody(inputStream) => inputStream.map(GzipHelper.gzip)
        case b: ElBody                    => b.asStream.map(GzipHelper.gzip)
      }

      ByteArrayBody(gzippedBytes)
    }

  def stream(charset: Charset): Body => InputStreamBody =
    (body: Body) => {
      val stream = body match {
        case stringBody: StringBody   => stringBody.asBytes.bytes.map(new FastByteArrayInputStream(_))
        case pebbleBody: PebbleBody   => pebbleBody.map(string => new FastByteArrayInputStream(string.getBytes(charset)))
        case ByteArrayBody(byteArray) => byteArray.map(new FastByteArrayInputStream(_))
        case RawFileBody(resourceAndCachedBytes) =>
          resourceAndCachedBytes.map {
            case ResourceAndCachedBytes(resource, cachedBytes) =>
              cachedBytes match {
                case Some(bytes) => new FastByteArrayInputStream(bytes)
                case _           => new FileInputStream(resource.file)
              }
          }
        case InputStreamBody(inputStream) => inputStream
        case b: ElBody                    => b.asStream
      }

      InputStreamBody(stream)
    }
}
