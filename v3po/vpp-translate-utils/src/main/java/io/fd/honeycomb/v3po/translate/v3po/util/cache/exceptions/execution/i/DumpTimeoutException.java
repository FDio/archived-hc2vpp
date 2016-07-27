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

package io.fd.honeycomb.v3po.translate.v3po.util.cache.exceptions.execution.i;

import io.fd.honeycomb.v3po.translate.v3po.util.cache.exceptions.execution.DumpExecutionFailedException;
import java.util.concurrent.TimeoutException;

/**
 * Exception thrown when dump call ends in timeout
 */
public class DumpTimeoutException extends DumpExecutionFailedException {

    public DumpTimeoutException(String message, TimeoutException cause) {
        super(message, cause);

    }

    public static final DumpTimeoutException wrapTimeoutException(String message, TimeoutException cause) {
        return new DumpTimeoutException(message, cause);
    }
}
