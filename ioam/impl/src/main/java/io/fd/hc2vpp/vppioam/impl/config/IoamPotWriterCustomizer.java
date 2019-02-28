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

package io.fd.hc2vpp.vppioam.impl.config;


import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vppioam.impl.util.FutureJVppIoampotCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.ioampot.dto.PotProfileAdd;
import io.fd.jvpp.ioampot.dto.PotProfileAddReply;
import io.fd.jvpp.ioampot.dto.PotProfileDel;
import io.fd.jvpp.ioampot.dto.PotProfileDelReply;
import io.fd.jvpp.ioampot.future.FutureJVppIoampot;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSet;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IoamPotWriterCustomizer extends FutureJVppIoampotCustomizer implements
        ListWriterCustomizer<PotProfileSet,PotProfileSetKey>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IoamPotWriterCustomizer.class);

    public IoamPotWriterCustomizer(@Nonnull FutureJVppIoampot futureJVppIoampot) {
        super(futureJVppIoampot);
    }


    /**
     * Handle write operation. C from CRUD.
     *
     * @param id           Identifier(from root) of data being written
     * @param dataAfter    New data to be written
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     * @throws WriteFailedException if write was unsuccessful
     */
    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<PotProfileSet> id,
                                       @Nonnull PotProfileSet dataAfter, @Nonnull WriteContext writeContext)
            throws WriteFailedException {
        try {
            addPotProfile(dataAfter,id);
        } catch (WriteFailedException exCreate) {
            LOG.error("Add POT profile failed", exCreate);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, exCreate);
        }

        LOG.info("POT profile added iid={}, added {}", id, dataAfter);
    }

    /**
     * Handle update operation. U from CRUD.
     *
     * @param id           Identifier(from root) of data being written
     * @param dataBefore   Old data
     * @param dataAfter    New, updated data
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     * @throws WriteFailedException if update was unsuccessful
     */
    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<PotProfileSet> id,
                                        @Nonnull PotProfileSet dataBefore, @Nonnull PotProfileSet dataAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        try {
            delPotProfile(dataBefore,id);
            addPotProfile(dataAfter,id);
        } catch (WriteFailedException exUpdate) {
            LOG.error("Update POT Profile failed", exUpdate);
            throw new WriteFailedException.UpdateFailedException(id,dataBefore,dataAfter,exUpdate);
        }

        LOG.info("POT profile updated iid={}, added {}", id, dataAfter);
    }

    /**
     * Handle delete operation. D from CRUD.
     *
     * @param id           Identifier(from root) of data being written
     * @param dataBefore   Old data being deleted
     * @param writeContext Write context can be used to store any useful information and then utilized by other customizers
     * @throws WriteFailedException if delete was unsuccessful
     */
    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<PotProfileSet> id,
                                        @Nonnull PotProfileSet dataBefore, @Nonnull WriteContext writeContext)
            throws WriteFailedException {
        try {
            delPotProfile(dataBefore,id);
        } catch (WriteFailedException exDelete) {
            LOG.error("Del POT Profile failed", exDelete);
            throw new WriteFailedException.DeleteFailedException(id, exDelete);
        }

        LOG.info("POT profile deleted iid={}, added {}", id, dataBefore);
    }

    private void addPotProfile(PotProfileSet potProfileSet,
            InstanceIdentifier<PotProfileSet> id) throws WriteFailedException {
        for ( PotProfileList potProfileList : potProfileSet.getPotProfileList()) {
            writePotProfileList(potProfileList,potProfileSet.getName(),id);
        }
    }

    private PotProfileAddReply writePotProfileList(PotProfileList potProfileList, String name,
                                                   InstanceIdentifier<PotProfileSet> id) throws WriteFailedException{
        PotProfileAdd request = new PotProfileAdd();
        request.id = potProfileList.getIndex().getValue().byteValue();
        request.validator = (byte) (potProfileList.isValidator() ? 1 : 0);
        request.secretShare = potProfileList.getSecretShare().longValue();
        request.prime = potProfileList.getPrimeNumber().longValue();
        request.secretKey = potProfileList.isValidator() ? potProfileList.getValidatorKey().longValue() : 0;
        request.maxBits = potProfileList.getNumberOfBits().byteValue();
        request.lpc = potProfileList.getLpc().longValue();
        request.polynomialPublic = potProfileList.getPublicPolynomial().longValue();
        request.listNameLen = (byte) name.getBytes(StandardCharsets.UTF_8).length;
        request.listName = name.getBytes(StandardCharsets.UTF_8);

        return getReplyForWrite(getFutureJVppIoampot().potProfileAdd(request).toCompletableFuture(), id);
    }

    private PotProfileDelReply delPotProfile(PotProfileSet potProfileSet, InstanceIdentifier<PotProfileSet> id)
            throws WriteFailedException{
        PotProfileDel request = new PotProfileDel();
        request.listNameLen = (byte)potProfileSet.getName().getBytes(StandardCharsets.UTF_8).length;
        request.listName = potProfileSet.getName().getBytes(StandardCharsets.UTF_8);

        return getReplyForWrite(getFutureJVppIoampot().potProfileDel(request).toCompletableFuture(),id);
    }
}
