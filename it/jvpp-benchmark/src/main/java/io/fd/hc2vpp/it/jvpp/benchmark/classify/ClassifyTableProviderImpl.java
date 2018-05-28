/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.it.jvpp.benchmark.classify;

import io.fd.vpp.jvpp.core.dto.ClassifyAddDelTable;
import java.io.Serializable;
import java.util.Random;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class ClassifyTableProviderImpl implements ClassifyTableProvider {
    /**
     * Static seed to make rnd.nextBytes() output the same for all test run.
     */
    private static final long SEED = -2084670072119134328L;
    private final int tableSetSize;
    private final ClassifyAddDelTable[] tables;
    private final Random rnd = new Random(SEED);

    /**
     * Pointer to Classify table to be returned by invocation of {@link #next()} method.
     */
    private int currentTable = 0;

    ClassifyTableProviderImpl(final int tableSetSize) {
        this.tableSetSize = tableSetSize;
        tables = new ClassifyAddDelTable[tableSetSize];
        initTables(tableSetSize);
    }

    @Override
    public ClassifyAddDelTable next() {
        final ClassifyAddDelTable result = tables[currentTable];
        currentTable = (currentTable + 1) % tableSetSize;
        return result;
    }

    private void initTables(final int tableSetSize) {
        for (int i = 0; i < tableSetSize; ++i) {
            tables[i] = createTable();
        }
    }

    private ClassifyAddDelTable createTable() {
        final ClassifyAddDelTable addDelTable = new ClassifyAddDelTable();
        addDelTable.isAdd = 1;
        addDelTable.tableIndex = -1;
        addDelTable.nbuckets = 2;
        addDelTable.memorySize = 2 << 20;
        addDelTable.nextTableIndex = ~0;
        addDelTable.missNextIndex = ~0;
        addDelTable.skipNVectors = 0;
        addDelTable.matchNVectors = 1;
        addDelTable.mask = new byte[16];
        rnd.nextBytes(addDelTable.mask);
        return addDelTable;
    }
}
