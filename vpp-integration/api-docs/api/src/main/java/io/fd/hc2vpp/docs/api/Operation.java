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

package io.fd.hc2vpp.docs.api;

import java.util.Objects;

/**
 * Reference to single crud operation
 */
public class Operation {

    /**
     * Git link to class that performs referenced operation
     */
    private final String link;
    //TODO - investigate option to link directly to line number

    /**
     * Type of crud operation
     */
    private final CrudOperation operation;

    public Operation(final String link, final CrudOperation operation) {
        this.link = link;
        this.operation = operation;
    }

    public String getLink() {
        return link;
    }

    public CrudOperation getOperation() {
        return operation;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Operation that = (Operation) o;

        return Objects.equals(this.link, that.link) &&
                Objects.equals(this.operation, that.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(link, operation);
    }

    public enum CrudOperation {
        WRITE("Write", "writeCurrentAttributes"),
        UPDATE("Update", "updateCurrentAttributes"),
        DELETE("Delete", "deleteCurrentAttributes"),
        READ_ALL("Read all", "getAllIds"),
        READ_ONE("Read details", "readCurrentAttributes");

        private final String displayName;
        private final String methodReference;

        CrudOperation(final String displayName, final String methodReference) {
            this.displayName = displayName;
            this.methodReference = methodReference;
        }

        public String getMethodReference() {
            return methodReference;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
