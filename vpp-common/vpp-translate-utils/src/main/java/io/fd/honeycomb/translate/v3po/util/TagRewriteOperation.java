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
package io.fd.honeycomb.translate.v3po.util;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;

/**
 * Defines vlan tag rewrite config options for VPP
 *
 * TODO corresponding enum (defined in l2_vtr.h) should be defined in vpe.api
 * (does vpp's IDL support enum type definition?)
 * which would allow to generate this class in jvpp
 */
public enum TagRewriteOperation {
    disabled(0),
    push_1(0),
    push_2(0),
    pop_1(1),
    pop_2(2),
    translate_1_to_1(1),
    translate_1_to_2(1),
    translate_2_to_1(2),
    translate_2_to_2(2);

    private final static int MAX_INDEX = 3;
    private final int code;
    private final byte popTags;

    TagRewriteOperation(final int popTags) {
        this.code = this.ordinal();
        this.popTags = UnsignedBytes.checkedCast(popTags);
    }

    private static TagRewriteOperation[][] translation = new TagRewriteOperation[][] {
        {disabled, push_1, push_2},
        {pop_1, translate_1_to_1, translate_1_to_2},
        {pop_2, translate_2_to_1, translate_2_to_2}
    };

    /**
     * Returns VPP tag rewrite operation for given number of tags to pop and tags to push.
     * @param toPop number of tags to pop (0..2)
     * @param toPush number of tags to push (0..2)
     * @return vpp tag rewrite operation for given input parameters
     */
    public static TagRewriteOperation get(@Nonnegative final int toPop, @Nonnegative final int toPush) {
        Preconditions.checkElementIndex(toPop, MAX_INDEX, "Illegal number of tags to pop");
        Preconditions.checkElementIndex(toPush, MAX_INDEX, "Illegal number of tags to push");
        return translation[toPop][toPush];
    }

    /**
     * Returns VPP tag rewrite operation for given operation code.
     * @param code VPP tag rewrite operation code
     * @return vpp tag rewrite operation for given input parameter
     */
    @Nullable
    public static TagRewriteOperation get(@Nonnegative final int code) {
        for (TagRewriteOperation operation : TagRewriteOperation.values()) {
            if (code == operation.code){
                return operation;
            }
        }
        return null;
    }

    public byte getPopTags() {
        return popTags;
    }
}
