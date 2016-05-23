/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.translate.util;

import com.google.common.base.Charsets;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class JsonUtils {

    private JsonUtils() {}

    /**
     * Serialize normalized node root structure into provided output stream
     *
     * @throws IOException if serialized data cannot be written into provided output stream
     */
    public static void writeJsonRoot(@Nonnull final NormalizedNode<?, ?> rootData,
                                     @Nonnull final SchemaContext schemaContext,
                                     @Nonnull final OutputStream outputStream) throws IOException {
        final JsonWriter
            jsonWriter = createJsonWriter(outputStream, true);
        final NormalizedNodeStreamWriter streamWriter = JSONNormalizedNodeStreamWriter
            .createNestedWriter(JSONCodecFactory.create(schemaContext), SchemaPath.ROOT, null, jsonWriter);
        final NormalizedNodeWriter normalizedNodeWriter =
            NormalizedNodeWriter.forStreamWriter(streamWriter, true);
        jsonWriter.beginObject();
        writeChildren(normalizedNodeWriter,(ContainerNode) rootData);
        jsonWriter.endObject();
        jsonWriter.flush();
    }

    /**
     * Read json serialized normalized node root structure and parse them into normalized nodes
     *
     * @return artificial normalized node holding all the top level nodes from provided stream as children. In case
     *         the stream is empty, empty artificial normalized node is returned
     *
     * @throws IllegalArgumentException if content in the provided input stream is not restore-able
     */
    public static ContainerNode readJsonRoot(@Nonnull final SchemaContext schemaContext,
                                             @Nonnull final InputStream stream) {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> builder =
            Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(schemaContext.getQName()));
        final NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(builder);

        final JsonParserStream jsonParser = JsonParserStream.create(writer, schemaContext);
        final JsonReader reader = new JsonReader(new InputStreamReader(stream, Charsets.UTF_8));
        jsonParser.parse(reader);

        return builder.build();
    }

    private static void writeChildren(final NormalizedNodeWriter nnWriter, final ContainerNode data) throws IOException {
        for(final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child : data.getValue()) {
            nnWriter.write(child);
        }
    }

    private static JsonWriter createJsonWriter(final OutputStream entityStream, boolean prettyPrint) {
        if (prettyPrint) {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, Charsets.UTF_8), 2);
        } else {
            return JsonWriterFactory.createJsonWriter(new OutputStreamWriter(entityStream, Charsets.UTF_8));
        }
    }
}
