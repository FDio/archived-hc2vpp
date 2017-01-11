/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.common.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.annotation.Nonnull;

public interface CommonTests {

    default void verifyExceptionalCase(@Nonnull final Performer negativeCase,
                                       @Nonnull final Class<? extends Exception> expectedExceptionType) {
        try {
            negativeCase.perform();
        } catch (Exception e) {
            assertEquals(e.getLocalizedMessage(), e.getClass(), expectedExceptionType);
            return;
        }
        fail("Test should have thrown exception");
    }

    /**
     * Used to just perform test, without consuming or supplying anything
     */
    @FunctionalInterface
    interface Performer {
        void perform();
    }
}
