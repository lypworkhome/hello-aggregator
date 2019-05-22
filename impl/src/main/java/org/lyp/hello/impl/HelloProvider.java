/*
 * Copyright © 2017 lilili and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.lyp.hello.impl;

import com.google.common.util.concurrent.SettableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.GetFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.GetFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hello.rev150105.HelloService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class HelloProvider implements HelloService {

    private static final Logger LOG = LoggerFactory.getLogger(HelloProvider.class);
    private Registration topoNodeListherReg = null;

    private final DataBroker dataBroker;

    private SalFlowService salFlowService;
    public HelloProvider(final DataBroker dataBroker,final SalFlowService salFlowService) {
        this.salFlowService=salFlowService;
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {

        // Write initial flows
        LOG.info("new hello method is begin");
        PingUtil pingUtil = new PingUtil(salFlowService, dataBroker);
        pingUtil.setFlowTableId((short) 0);
        pingUtil.setFlowPriority(300);
        pingUtil.setFlowIdleTimeout(0);
        pingUtil.setFlowHardTimeout(0);
        topoNodeListherReg = pingUtil.registerAsDataChangeListener(dataBroker);
        LOG.info("the util is used");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() throws Exception {
        LOG.info("HelloProvider Closed");

        if (topoNodeListherReg!=null){
            topoNodeListherReg.close();
        }
        LOG.info("HelloProvider Closed");
    }

    @Override
    public Future<RpcResult<Void>> getFlow(GetFlowInput input) {
        SettableFuture<RpcResult<Void>> future = SettableFuture.create();


        future.set(RpcResultBuilder.success(new GetFlowInputBuilder().build()).build());
        return null;
    }
}