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
package org.onosproject.inbandtelemetry.impl;


<<<<<<< HEAD
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
=======
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.ImmutableList;
>>>>>>> origin/master
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
<<<<<<< HEAD
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.inbandtelemetry.IntObjective;
import org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable;
import org.onosproject.net.behaviour.inbandtelemetry.IntReportConfig;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
=======
import org.onlab.packet.TpPort;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.behaviour.inbandtelemetry.IntReportConfig;
import org.onosproject.net.behaviour.inbandtelemetry.IntDeviceConfig;
import org.onosproject.net.config.ConfigApplyDelegate;
>>>>>>> origin/master
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceService;
<<<<<<< HEAD
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
=======
>>>>>>> origin/master
import org.onosproject.net.host.HostService;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestStorageService;

<<<<<<< HEAD
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
=======
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
>>>>>>> origin/master
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
<<<<<<< HEAD
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable.IntFunctionality.POSTCARD;
import static org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable.IntFunctionality.SINK;
import static org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable.IntFunctionality.SOURCE;
import static org.onosproject.net.behaviour.inbandtelemetry.IntProgrammable.IntFunctionality.TRANSIT;
=======
import static org.junit.Assert.assertEquals;
import static org.onlab.junit.TestUtils.setField;
>>>>>>> origin/master

public class SimpleIntManagerTest {
    private static final String APP_NAME = "org.onosproject.inbandtelemetry";
    private static final ApplicationId APP_ID = new TestApplicationId(APP_NAME);
    private static final IpAddress COLLECTOR_IP = IpAddress.valueOf("10.0.0.1");
<<<<<<< HEAD
    private static final TpPort COLLECTOR_PORT = TpPort.tpPort(32766);
    private static final int MIN_FLOW_HOP_LATENCY_CHANGE_NS = 32;
    private static final String INT_REPORT_CONFIG_KEY = "report";
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("device:leaf1");
    private static final TrafficSelector FLOW_SELECTOR1 = DefaultTrafficSelector.builder()
            .matchIPDst(IpPrefix.valueOf("192.168.10.0/24"))
            .matchVlanId(VlanId.vlanId((short) 10))
            .build();
    private static final TrafficSelector FLOW_SELECTOR2 = DefaultTrafficSelector.builder()
            .matchIPDst(IpPrefix.valueOf("192.168.20.0/24"))
            .matchVlanId(VlanId.vlanId((short) 20))
            .build();
    private static final Device DEFAULT_DEVICE =
            new DefaultDevice(null, DEVICE_ID, Device.Type.SWITCH, "", "", "", "", null);
    private static final List<Port> DEVICE_PORTS = ImmutableList.of(
            new DefaultPort(DEFAULT_DEVICE, PortNumber.portNumber(1), true),
            new DefaultPort(DEFAULT_DEVICE, PortNumber.portNumber(2), true)
    );
    private static final Host HOST1 =
            new DefaultHost(null, HostId.hostId("00:00:00:00:00:01/None"), null,
                    VlanId.NONE, ImmutableSet.of(), ImmutableSet.of(), true);
    private static final Host HOST2 =
            new DefaultHost(null, HostId.hostId("00:00:00:00:00:02/None"), null,
                    VlanId.NONE, ImmutableSet.of(), ImmutableSet.of(), true);
    private static final Map<ConnectPoint, Host> HOSTS = ImmutableMap.of(
            ConnectPoint.fromString("device:leaf1/1"), HOST1,
            ConnectPoint.fromString("device:leaf1/2"), HOST2
    );
=======
    private static final TpPort COLLECTOR_PORT = TpPort.tpPort(5500);
    private static final int MIN_FLOW_HOP_LATENCY_CHANGE_NS = 16;
>>>>>>> origin/master

    private SimpleIntManager manager;
    private StorageService storageService;
    private MastershipService mastershipService;
    private CoreService coreService;
    private HostService hostService;
    private DeviceService deviceService;
    private NetworkConfigRegistry networkConfigRegistry;
    private NetworkConfigService networkConfigService;
    private NetworkConfigListener networkConfigListener;


    @Before
<<<<<<< HEAD
    public void setup() throws IOException {
=======
    public void setup() {
>>>>>>> origin/master
        storageService = new TestStorageService();
        mastershipService = createNiceMock(MastershipService.class);
        coreService = createNiceMock(CoreService.class);
        hostService = createNiceMock(HostService.class);
        deviceService = createNiceMock(DeviceService.class);
        expect(deviceService.getDevices()).andReturn(ImmutableList.of()).anyTimes();
        networkConfigRegistry = createNiceMock(NetworkConfigRegistry.class);
        networkConfigService = createNiceMock(NetworkConfigService.class);

        manager = new SimpleIntManager();
<<<<<<< HEAD
        manager.coreService = coreService;
        manager.deviceService = deviceService;
        manager.storageService = storageService;
        manager.mastershipService = mastershipService;
        manager.hostService = hostService;
        manager.netcfgService = networkConfigService;
        manager.netcfgRegistry = networkConfigRegistry;

        expect(coreService.registerApplication(APP_NAME))
                .andReturn(APP_ID).anyTimes();
=======
        setField(manager, "coreService", coreService);
        setField(manager, "deviceService", deviceService);
        setField(manager, "storageService", storageService);
        setField(manager, "mastershipService", mastershipService);
        setField(manager, "hostService", hostService);
        setField(manager, "netcfgService", networkConfigService);
        setField(manager, "netcfgRegistry", networkConfigRegistry);

        expect(coreService.registerApplication(APP_NAME)).andReturn(APP_ID).once();
>>>>>>> origin/master
        networkConfigRegistry.registerConfigFactory(anyObject());
        expectLastCall().once();

        Capture<NetworkConfigListener> capture = newCapture();
        networkConfigService.addListener(EasyMock.capture(capture));
        expectLastCall().once();
<<<<<<< HEAD
        IntReportConfig config = getIntReportConfig("/report-config.json");
        expect(networkConfigService.getConfig(APP_ID, IntReportConfig.class))
                .andReturn(config)
                .anyTimes();
=======
>>>>>>> origin/master
        replay(mastershipService, deviceService, coreService,
                hostService, networkConfigRegistry, networkConfigService);
        manager.activate();
        networkConfigListener = capture.getValue();
    }

    @After
    public void teardown() {
        manager.deactivate();
    }

    @Test
<<<<<<< HEAD
    public void testPushIntAppConfig() throws IOException {
        IntReportConfig config = getIntReportConfig("/report-config.json");
=======
    public void testPushIntAppConfig() {
        IntReportConfig config = new IntReportConfig();
        ObjectMapper mapper = new ObjectMapper();
        ConfigApplyDelegate delegate = configApply -> { };
        config.init(APP_ID, "report", JsonNodeFactory.instance.objectNode(), mapper, delegate);
        config.setCollectorIp(COLLECTOR_IP)
                .setCollectorPort(COLLECTOR_PORT)
                .setMinFlowHopLatencyChangeNs(MIN_FLOW_HOP_LATENCY_CHANGE_NS);

>>>>>>> origin/master
        NetworkConfigEvent event =
                new NetworkConfigEvent(NetworkConfigEvent.Type.CONFIG_ADDED, APP_ID,
                        config, null, IntReportConfig.class);
        networkConfigListener.event(event);

        // We expected that the manager will store the device config which
        // converted from the app config.
<<<<<<< HEAD
        IntDeviceConfig expectedConfig = createIntDeviceConfig();
        IntDeviceConfig actualConfig = manager.getConfig();
        assertEquals(expectedConfig, actualConfig);
    }

    @Test
    public void testConfigNonIntDevice() {
        reset(deviceService);
        Device device = getMockDevice(false, DEVICE_ID);
        expect(deviceService.getDevice(DEVICE_ID))
                .andReturn(device)
                .anyTimes();
        expect(deviceService.getDevices())
                .andReturn(ImmutableSet.of(device))
                .anyTimes();
        replay(deviceService, device);
        assertTrue(manager.configDevice(DEVICE_ID));
        verify();
    }

    @Test
    public void testConfigSourceDevice() {
        reset(deviceService, hostService);
        Device device = getMockDevice(true, DEVICE_ID);
        IntProgrammable intProg = getMockIntProgrammable(true, false, false, false);
        setUpDeviceTest(device, intProg, true, false);
        IntObjective intObj = IntObjective.builder()
                .withSelector(FLOW_SELECTOR2)
                .build();
        expect(intProg.addIntObjective(eq(intObj)))
                .andReturn(true)
                .once();
        expect(intProg.setSourcePort(PortNumber.portNumber(1))).andReturn(true).once();
        expect(intProg.setSourcePort(PortNumber.portNumber(2))).andReturn(true).once();
        replay(deviceService, hostService, device, intProg);
        installTestIntents();
        assertTrue(manager.configDevice(DEVICE_ID));
        verify(intProg);
    }

    @Test
    public void testConfigTransitDevice() {
        reset(deviceService, hostService);
        Device device = getMockDevice(true, DEVICE_ID);
        IntProgrammable intProg = getMockIntProgrammable(false, true, false, false);
        setUpDeviceTest(device, intProg, false, false);
        replay(deviceService, hostService, device, intProg);
        installTestIntents();
        assertTrue(manager.configDevice(DEVICE_ID));
        verify(intProg);
    }

    @Test
    public void testConfigSinkDevice() {
        reset(deviceService, hostService);
        Device device = getMockDevice(true, DEVICE_ID);
        IntProgrammable intProg = getMockIntProgrammable(false, false, true, false);
        setUpDeviceTest(device, intProg, true, true);
        expect(intProg.setSinkPort(PortNumber.portNumber(1))).andReturn(true).once();
        expect(intProg.setSinkPort(PortNumber.portNumber(2))).andReturn(true).once();
        replay(deviceService, hostService, device, intProg);
        installTestIntents();
        assertTrue(manager.configDevice(DEVICE_ID));
        verify(intProg);
    }

    @Test
    public void testConfigPostcardOnlyDevice() {
        reset(deviceService, hostService);
        Device device = getMockDevice(true, DEVICE_ID);
        IntProgrammable intProg = getMockIntProgrammable(false, false, false, true);
        setUpDeviceTest(device, intProg, true, true);
        IntObjective intObj = IntObjective.builder()
                .withSelector(FLOW_SELECTOR1)
                .build();
        expect(intProg.addIntObjective(eq(intObj)))
                .andReturn(true)
                .once();
        replay(deviceService, hostService, device, intProg);
        installTestIntents();
        assertTrue(manager.configDevice(DEVICE_ID));
        verify(intProg);
    }

    /*
     * Utilities
     */
    private void installTestIntents() {
        // Pre-install an INT intent to the manager.
        IntIntent.Builder intentBuilder = IntIntent.builder()
                .withHeaderType(IntIntent.IntHeaderType.HOP_BY_HOP)
                .withReportType(IntIntent.IntReportType.TRACKED_FLOW);

        IntIntent postcardIntent = intentBuilder
                .withTelemetryMode(IntIntent.TelemetryMode.POSTCARD)
                .withSelector(FLOW_SELECTOR1)
                .build();
        IntIntent nonPoscardIntent = intentBuilder
                .withTelemetryMode(IntIntent.TelemetryMode.INBAND_TELEMETRY)
                .withSelector(FLOW_SELECTOR2)
                .build();
        manager.installIntIntent(nonPoscardIntent);
        manager.installIntIntent(postcardIntent);
    }

    private void setUpDeviceTest(Device device, IntProgrammable intProg,
                                 boolean hostConnected, boolean setupIntConfig) {
        expect(device.as(IntProgrammable.class))
                .andReturn(intProg)
                .anyTimes();
        expect(deviceService.getDevice(eq(DEVICE_ID)))
                .andReturn(device)
                .anyTimes();
        expect(deviceService.getDevices())
                .andReturn(ImmutableList.of(device))
                .anyTimes();
        if (setupIntConfig) {
            IntDeviceConfig expectedConfig = createIntDeviceConfig();
            expect(intProg.setupIntConfig(eq(expectedConfig)))
                    .andReturn(true)
                    .atLeastOnce();
        }
        expect(deviceService.getPorts(DEVICE_ID))
                .andReturn(DEVICE_PORTS)
                .anyTimes();

        if (hostConnected) {
            HOSTS.forEach((cp, host) -> {
                expect(hostService.getConnectedHosts(eq(cp)))
                        .andReturn(ImmutableSet.of(host))
                        .anyTimes();
            });
            expect(hostService.getConnectedHosts(eq(DEVICE_ID)))
                    .andReturn(Sets.newHashSet(HOSTS.values()));
        } else {
            expect(hostService.getConnectedHosts(eq(DEVICE_ID)))
                    .andReturn(ImmutableSet.of())
                    .anyTimes();
        }
    }

    private IntReportConfig getIntReportConfig(String fileName) throws IOException {
        IntReportConfig config = new IntReportConfig();
        InputStream jsonStream = getClass().getResourceAsStream(fileName);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        config.init(APP_ID, INT_REPORT_CONFIG_KEY, jsonNode, mapper, c -> {
        });
        return config;
    }

    private Device getMockDevice(boolean supportInt, DeviceId deviceId) {
        Device device = createNiceMock(Device.class);
        expect(device.is(IntProgrammable.class))
                .andReturn(supportInt)
                .anyTimes();
        expect(device.id())
                .andReturn(deviceId)
                .anyTimes();
        return device;
    }

    private IntProgrammable getMockIntProgrammable(boolean supportSource, boolean supportTransit, boolean supportSink,
                                                   boolean supportPostcard) {
        IntProgrammable intProg = createNiceMock(IntProgrammable.class);
        if (supportSource) {
            expect(intProg.supportsFunctionality(SOURCE))
                    .andReturn(true).anyTimes();
        }
        if (supportTransit) {
            expect(intProg.supportsFunctionality(TRANSIT))
                    .andReturn(true).anyTimes();
        }
        if (supportSink) {
            expect(intProg.supportsFunctionality(SINK))
                    .andReturn(true).anyTimes();
        }
        if (supportPostcard) {
            expect(intProg.supportsFunctionality(POSTCARD))
                    .andReturn(true).anyTimes();
        }
        expect(intProg.init())
                .andReturn(true)
                .anyTimes();
        return intProg;
    }

    private IntDeviceConfig createIntDeviceConfig() {
        return IntDeviceConfig.builder()
                .withMinFlowHopLatencyChangeNs(MIN_FLOW_HOP_LATENCY_CHANGE_NS)
                .withCollectorPort(COLLECTOR_PORT)
                .withCollectorIp(COLLECTOR_IP)
                .enabled(true)
                .build();
=======
        IntDeviceConfig deviceConfig = manager.getConfig();
        assertEquals(COLLECTOR_IP, deviceConfig.collectorIp());
        assertEquals(COLLECTOR_PORT, deviceConfig.collectorPort());
        assertEquals(MIN_FLOW_HOP_LATENCY_CHANGE_NS, deviceConfig.minFlowHopLatencyChangeNs());
>>>>>>> origin/master
    }
}
