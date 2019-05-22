package org.lyp.hello.impl;

import com.google.common.collect.ImmutableList;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class PingUtil implements DataChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(PingUtil.class);

    private final ExecutorService initialFlowExecutor = Executors.newCachedThreadPool();
    private final SalFlowService salFlowService;
    private final DataBroker dataBroker;
    private final String FLOW_ID_PREFIX = "L2switch-";
    private final short DEFAULT_FLOW_TABLE_ID = 0;
    private final int DEFAULT_FLOW_PRIORITY = 0;
    private final int DEFAULT_FLOW_IDLE_TIMEOUT = 0;
    private final int DEFAULT_FLOW_HARD_TIMEOUT = 0;

    private AtomicLong flowIdInc = new AtomicLong();
    private AtomicLong flowCookieInc = new AtomicLong(0x2b00000000000000L);
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    public PingUtil(SalFlowService salFlowService, DataBroker dataBroker) {
        this.salFlowService = salFlowService;
        this.dataBroker = dataBroker;
    }

    public void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }


    public ListenerRegistration<DataChangeListener> registerAsDataChangeListener(DataBroker dataBroker) {
        InstanceIdentifier<Node> nodeInstanceIdentifier = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class).build();
        LOG.info("lesten is begin");

        return dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, nodeInstanceIdentifier, this, AsyncDataBroker.DataChangeScope.BASE);
    }


    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        //Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();


        Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
        if (createdData != null && !createdData.isEmpty()) {
            Set<InstanceIdentifier<?>> nodeIds = createdData.keySet();
            if (nodeIds != null && !nodeIds.isEmpty()) {
                initialFlowExecutor.submit(new InitialFlowWriterProcessor(nodeIds));
            }
        }
    }

    private class InitialFlowWriterProcessor implements Runnable {

        Set<InstanceIdentifier<?>> nodeIds = null;

        public InitialFlowWriterProcessor(Set<InstanceIdentifier<?>> nodeIds) {
            this.nodeIds = nodeIds;
        }

        @Override
        public void run() {
            if (nodeIds == null) {
                LOG.info("is null?!");
                return;
            }
            LOG.info("begin util work!!");
            for (InstanceIdentifier<?> nodeId : nodeIds) {
                LOG.info(nodeId.toString());
                if (Node.class.isAssignableFrom(nodeId.getTargetType())) {
                    InstanceIdentifier<Node> invNodeId = (InstanceIdentifier<Node>) nodeId;
                    if (invNodeId.firstKeyOf(Node.class, NodeKey.class).getId().getValue().contains("openflow:")) {
                        addInitialFlows(invNodeId);
                    }
                }
            }
        }

        /**
         * Adds a flow, which drops all packets, on the specifide node.
         *
         * @param nodeId The node to install the flow on.
         */
        public void addInitialFlows(InstanceIdentifier<Node> nodeId) {
            LOG.debug("adding initial flows for node {} ", nodeId);
            LOG.info("adding initial flows for node {} ", nodeId);
            InstanceIdentifier<Table> tableId = getTableInstanceId(nodeId);
            InstanceIdentifier<Flow> flowId = getFlowInstanceId(tableId);

            //add drop all flow
            writeFlowToController(nodeId, tableId, flowId, createDropAllFlow(flowTableId, flowPriority));

            LOG.info("Added initial flows for node {} ", nodeId);
            LOG.debug("Added initial flows for node {} ", nodeId);
        }

        private InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId) {
            // get flow table key
            TableKey flowTableKey = new TableKey(flowTableId);
            return nodeId.builder()
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey)
                    .build();
        }

        private InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId) {
            // generate unique flow key
            FlowId flowId = new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()));
            FlowKey flowKey = new FlowKey(flowId);
            return tableId.child(Flow.class, flowKey);
        }

        private Flow createDropAllFlow(Short tableId, int priority) {

            // start building flow
            FlowBuilder dropAll = new FlowBuilder() //
                    .setTableId(tableId) //
                    .setFlowName("dropall");
            try {


                // use its own hash code for id.
                dropAll.setId(new FlowId(Long.toString(dropAll.hashCode())));


                //set EthernetDestination MACAddress for match   ******
                EthernetMatch ethernetMatch = new EthernetMatchBuilder().setEthernetDestination(new EthernetDestinationBuilder().setAddress(new MacAddress("3a:6b:6c:9d:e6:67")).build()).build();
                Match match = new MatchBuilder().setEthernetMatch(ethernetMatch).build();

                Action dropAllAction = new ActionBuilder() //
                        .setOrder(0)
                        .setAction(new DropActionCaseBuilder().build())
                        .build();

                // Create an Apply Action
                ApplyActions applyActions = new ApplyActionsBuilder().setAction(ImmutableList.of(dropAllAction))
                        .build();

                // Wrap our Apply Action in an Instruction
                Instruction applyActionsInstruction = new InstructionBuilder() //
                        .setOrder(0)
                        .setInstruction(new ApplyActionsCaseBuilder()//
                                .setApplyActions(applyActions) //
                                .build()) //
                        .build();

                // Put our Instruction in a list of Instructions
                dropAll
                        .setMatch(match) //
                        .setInstructions(new InstructionsBuilder() //
                                .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                                .build()) //
                        .setPriority(priority) //
                        .setBufferId(OFConstants.OFP_NO_BUFFER) //
                        .setHardTimeout(flowHardTimeout) //
                        .setIdleTimeout(flowIdleTimeout) //
                        .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                        .setFlags(new FlowModFlags(false, false, false, false, false));
            } catch (Exception e) {
                LOG.info("createDropAllFlow has problem", e);
                LOG.info(e.toString() + e.getMessage() + e.getLocalizedMessage());

            }


            return dropAll.build();
        }

        private Future<RpcResult<AddFlowOutput>> writeFlowToController(InstanceIdentifier<Node> nodeInstanceId,
                                                                       InstanceIdentifier<Table> tableInstanceId,
                                                                       InstanceIdentifier<Flow> flowPath,
                                                                       Flow flow) {
            LOG.trace("Adding flow to node {}", nodeInstanceId.firstKeyOf(Node.class, NodeKey.class).getId().getValue());
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(flowPath));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            builder.setTransactionUri(new Uri(flow.getId().getValue()));
            return salFlowService.addFlow(builder.build());
        }
    }
}
