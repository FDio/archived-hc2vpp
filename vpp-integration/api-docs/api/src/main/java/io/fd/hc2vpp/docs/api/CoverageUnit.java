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

import java.util.List;
import java.util.Objects;

/**
 * Represents mapping between single supported VPP binary api and its binding
 */
public class CoverageUnit {

    /**
     * VPP binary api reference
     */
    private final VppApiMessage vppApi;

    /**
     * Java equivalent of VPP binary api
     */
    private final JavaApiMessage javaApi;

    /**
     * Yang types used to bind this request
     */
    private final List<YangType> yangTypes;

    /**
     * Operations supported for this api
     */
    private final List<Operation> supportedOperations;

    private CoverageUnit(final VppApiMessage vppApi, final JavaApiMessage javaApi,
                         final List<YangType> yangTypes,
                         final List<Operation> supportedOperations) {
        this.vppApi = vppApi;
        this.javaApi = javaApi;
        this.yangTypes = yangTypes;
        this.supportedOperations = supportedOperations;
    }

    public VppApiMessage getVppApi() {
        return vppApi;
    }

    public JavaApiMessage getJavaApi() {
        return javaApi;
    }

    public List<YangType> getYangTypes() {
        return yangTypes;
    }

    public List<Operation> getSupportedOperations() {
        return supportedOperations;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CoverageUnit that = (CoverageUnit) o;

        return Objects.equals(this.vppApi, that.vppApi) &&
                Objects.equals(this.javaApi, that.javaApi) &&
                Objects.equals(this.yangTypes, that.yangTypes) &&
                Objects.equals(this.supportedOperations, that.supportedOperations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vppApi, javaApi, yangTypes, supportedOperations);
    }

    public static class CoverageUnitBuilder {
        private VppApiMessage vppApi;
        private JavaApiMessage javaApi;
        private List<YangType> yangTypes;
        private List<Operation> supportedOperations;

        public CoverageUnitBuilder setVppApi(final VppApiMessage vppApi) {
            this.vppApi = vppApi;
            return this;
        }

        public CoverageUnitBuilder setJavaApi(final JavaApiMessage javaApi) {
            this.javaApi = javaApi;
            return this;
        }

        public CoverageUnitBuilder setYangTypes(final List<YangType> yangTypes) {
            this.yangTypes = yangTypes;
            return this;
        }

        public CoverageUnitBuilder setSupportedOperations(final List<Operation> supportedOperations) {
            this.supportedOperations = supportedOperations;
            return this;
        }

        public CoverageUnit build() {
            return new CoverageUnit(vppApi, javaApi, yangTypes, supportedOperations);
        }
    }
}
