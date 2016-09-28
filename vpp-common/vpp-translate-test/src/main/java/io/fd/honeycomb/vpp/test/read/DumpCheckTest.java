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

package io.fd.honeycomb.vpp.test.read;


import io.fd.honeycomb.translate.util.read.cache.EntityDumpNonEmptyCheck;
import io.fd.honeycomb.translate.util.read.cache.exceptions.check.DumpCheckFailedException;
import io.fd.honeycomb.translate.util.read.cache.exceptions.check.i.DumpEmptyException;
import org.junit.Before;
import org.junit.Test;

/**
 * Generic test for classes implementing {@link EntityDumpNonEmptyCheck}
 *
 * @param <T> implementations of {@link EntityDumpNonEmptyCheck}, that will be tested
 * @param <U> data node handled by {@link EntityDumpNonEmptyCheck}
 */
public abstract class DumpCheckTest<T extends EntityDumpNonEmptyCheck<U>, U> {

    private T check;
    private U emptyData;
    private U validData;

    @Before
    public void setupParent() {
        this.check = initCheck();
        this.emptyData = initEmptyData();
        this.validData = initValidData();
    }

    @Test(expected = DumpEmptyException.class)
    public void testWithNull() throws DumpCheckFailedException {
        check.assertNotEmpty(null);
    }

    @Test(expected = DumpEmptyException.class)
    public void testWithEmpty() throws DumpCheckFailedException {
        check.assertNotEmpty(emptyData);
    }

    @Test
    public void testWithValid() throws DumpCheckFailedException {
        check.assertNotEmpty(validData);
    }

    /**
     * Initialize new {@link EntityDumpNonEmptyCheck}.
     */
    protected abstract T initCheck();

    /**
     * Initialize data that should throw {@link DumpEmptyException} ,
     * while beeing processed by {@link EntityDumpNonEmptyCheck}
     */
    protected abstract U initEmptyData();

    /**
     * Initialize data that should pass without exception ,
     * while beeing processed by {@link EntityDumpNonEmptyCheck}
     */
    protected abstract U initValidData();


    protected T getCheck() {
        return this.check;
    }
}
