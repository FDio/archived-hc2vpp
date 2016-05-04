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

package io.fd.honeycomb.v3po.translate.v3po.util;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Naming context keeping a mapping between int index and string name.
 * Provides artificial names to unknown indices.
 */
public final class NamingContext implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NamingContext.class);

    private final BiMap<String, Integer> nameMapping = HashBiMap.create();
    private final String artificialNamePrefix;

    public NamingContext(final String artificialNamePrefix) {
        this.artificialNamePrefix = artificialNamePrefix;
    }

    @Nonnull
    public synchronized String getName(final int index) {
        if(!nameMapping.inverse().containsKey(index)) {
            final String artificialName = getArtificialName(index);
            LOG.info("Assigning artificial name: {} for index: {}", artificialName, index);
            addName(index, artificialName);
        }
        return nameMapping.inverse().get(index);
    }

    @Nonnull
    public synchronized boolean containsName(final int index) {
        return nameMapping.inverse().containsKey(index);
    }

    @Nonnull
    public synchronized void addName(final int index, final String name) {
        nameMapping.put(name, index);
    }

    @Nonnull
    public synchronized int removeName(final String name) {
        return nameMapping.remove(name);
    }

    /**
     * Returns index value associated with the given name.
     *
     * @param name the name whose associated index value is to be returned
     * @return integer index value matching supplied name
     * @throws IllegalArgumentException if name was not found
     */
    @Nonnull
    public synchronized int getIndex(String name) {
        checkArgument(nameMapping.containsKey(name), "Name %s not found. Known names: %s",
                name, nameMapping);
        return nameMapping.get(name);
    }

    @Nonnull
    public synchronized boolean containsIndex(String interfaceName) {
        return nameMapping.containsKey(interfaceName);
    }

    public String getArtificialName(final int index) {
        return artificialNamePrefix + index;
    }

    @Override
    public void close() throws Exception {
        nameMapping.clear();
    }
}
