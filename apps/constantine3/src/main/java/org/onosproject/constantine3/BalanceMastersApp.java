/*
 * Copyright 2020-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.constantine3;

import org.onosproject.cluster.ClusterService;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic switch migration for ONOS.
 */
@Component(immediate = true,
        service = {MastersBalanceService.class}
)
public class BalanceMastersApp
        extends AbstractListenerManager<MastershipEvent, MastershipListener>
        implements MastersBalanceService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BalanceMastersImpl balanceMastersImpl;

    ReactivePacketProcessor processor =
            new ReactivePacketProcessor();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                case PORT_ADDED:
                case PORT_REMOVED:
                case DEVICE_REMOVED:
                    balanceMastersImpl.getOpenFlowDeviceHashMap().remove(device.id().toString());
                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().type() == Device.Type.CONTROLLER;
        }
    }

    @Activate
    protected void activate() {
        coreService.registerApplication("org.onosproject.constantine3");
        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
        deviceService.addListener(deviceListener);
        balanceMastersImpl = new BalanceMastersImpl(clusterService, store);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        balanceMastersImpl.setFlag(false);
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    private void packetInRateCalculate(String str) {
        // not packetIn ever
        if (!balanceMastersImpl.getOpenFlowDeviceHashMap().containsKey(str)) {
            OpenFlowDevice openFlowDevice = new OpenFlowDevice(str, System.currentTimeMillis());
            openFlowDevice.setReceivedPacketInNum(1);
            balanceMastersImpl.getOpenFlowDeviceHashMap().put(str, openFlowDevice);
            return;
        }

        // packetIn ever
        OpenFlowDevice openFlowDevice = balanceMastersImpl.getOpenFlowDeviceHashMap().get(str);
        int packetNum = openFlowDevice.getReceivedPacketInNum();
        packetNum++;
        openFlowDevice.setReceivedPacketInNum(packetNum);
        long diffTime = System.currentTimeMillis() - openFlowDevice.getStartTime();
        double nowRate = (double) packetNum / (diffTime / 1000.0);
        openFlowDevice.setReceivedPacketInRate(nowRate);
        log.info("deviceID: " + str + "    " + "Rate: " + nowRate);
    }

    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            InboundPacket pkt = context.inPacket();
            packetInRateCalculate(pkt.receivedFrom().deviceId().toString());
           // log.info("recv PacketIn deviceId: " + pkt.receivedFrom().deviceId().toString());
        }
    }
}
