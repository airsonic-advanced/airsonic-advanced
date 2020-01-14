/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.airsonic.player.spring;

import org.airsonic.player.util.LambdaUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of {@link HttpMessageConverter} that can write a single
 * {@link ResourceRegion}, or Collections of {@link ResourceRegion
 * ResourceRegions}. This class temporarily donated to Airsonic until Spring MVC
 * 5.2.3 is released with
 * https://github.com/spring-projects/spring-framework/commit/0eacb443b01833eb1b34006d74c2ee6da04af403
 * which fixes the ResourceRange InputStream non-reuse.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Randomnic
 * @since 4.3
 */
@Component
public class ResourceRegionReusableHttpMessageConverter extends ResourceRegionHttpMessageConverter {

    @Override
    @SuppressWarnings("unchecked")
    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        if (object instanceof ResourceRegion) {
            writeResourceRegion((ResourceRegion) object, outputMessage);
        } else {
            Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
            if (regions.size() == 1) {
                writeResourceRegion(regions.iterator().next(), outputMessage);
            } else {
                writeResourceRegionCollection((Collection<ResourceRegion>) object, outputMessage);
            }
        }
    }

    private void writeResourceRegionCollection(Collection<ResourceRegion> resourceRegions,
            HttpOutputMessage outputMessage) throws IOException {

        Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
        HttpHeaders responseHeaders = outputMessage.getHeaders();

        MediaType contentType = responseHeaders.getContentType();
        String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
        OutputStream out = outputMessage.getBody();
        Map<Resource, Entry<InputStream, Long>> currStreams = new HashMap<>();

        try {
            for (ResourceRegion region : resourceRegions) {
                // retrieve an existing stream or generate a new one
                Entry<InputStream, Long> input = currStreams.computeIfAbsent(region.getResource(), LambdaUtils.uncheckFunction(r -> new SimpleEntry<>(r.getInputStream(), 0L)));

                // offset the existing stream location from the byte start so appropriate number of bytes are skipped
                long start = region.getPosition() - input.getValue();

                // check if range is out of order (stream pointer has advanced beyond what the range is requesting)
                if (start < 0) {
                    // close existing
                    input.getKey().close();
                    // open new stream
                    input = currStreams.computeIfPresent(region.getResource(), LambdaUtils.uncheckBiFunction((r, i) -> new SimpleEntry<>(r.getInputStream(), 0L)));

                    // reset start
                    start = region.getPosition();
                }

                long end = start + region.getCount() - 1;

                // Writing MIME header.
                println(out);
                print(out, "--" + boundaryString);
                println(out);
                if (contentType != null) {
                    print(out, "Content-Type: " + contentType.toString());
                    println(out);
                }
                Long resourceLength = region.getResource().contentLength();
                end = Math.min(end, resourceLength - 1);
                print(out, "Content-Range: bytes " + region.getPosition() + '-' + (Math.min(region.getPosition() + region.getCount(), region.getResource().contentLength()) - 1) + '/' + resourceLength);
                println(out);
                println(out);
                // Printing content
                copyRange(input.getKey(), out, start, end);

                //set stream position to reuse for next time
                input.setValue(end + 1);
            }
        } finally {
            currStreams.values().stream().map(e -> e.getKey()).forEach(in -> {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore
                }
            });
        }

        println(out);
        print(out, "--" + boundaryString + "--");
    }

    public static long copyRange(InputStream in, OutputStream out, long start, long end) throws IOException {
        Assert.notNull(in, "No InputStream specified");
        Assert.notNull(out, "No OutputStream specified");

        long skipped = in.skip(start);
        if (skipped < start) {
            throw new IOException("Skipped only " + skipped + " bytes out of " + start + " required");
        }

        long bytesToCopy = end - start + 1;
        byte[] buffer = new byte[Math.min(StreamUtils.BUFFER_SIZE, (int) bytesToCopy)];
        while (bytesToCopy > 0) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            } else if (bytesRead <= bytesToCopy) {
                out.write(buffer, 0, bytesRead);
                bytesToCopy -= bytesRead;
            } else {
                out.write(buffer, 0, (int) bytesToCopy);
                bytesToCopy = 0;
            }
        }
        return (end - start + 1 - bytesToCopy);
    }

    private static void println(OutputStream os) throws IOException {
        os.write('\r');
        os.write('\n');
    }

    private static void print(OutputStream os, String buf) throws IOException {
        os.write(buf.getBytes(StandardCharsets.US_ASCII));
    }

}
