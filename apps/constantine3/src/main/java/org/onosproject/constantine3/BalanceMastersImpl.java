package org.onosproject.constantine3;

import com.google.common.collect.Sets;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;

public class BalanceMastersImpl {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final double v = 25.0;
    private final double f = 20.0;
    private final double mu = 10.0;
    private final double alpha = 1.0;
    // private final double sigma = 0.5;
    private double sigma;
    private Set<OpenFlowDevice> orphanedDevices = Sets.newHashSet();
    private final List<DistributedControllerNode> distributedControllerNodeList = new LinkedList<>();
    private final List<DistributedControllerNode> distributedCOMNodeList = new LinkedList<>();
    private final List<DistributedControllerNode> distributedCIMNodeList = new LinkedList<>();
    private double[][] Q = null;

    private boolean flag = true;

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    // TODO maybe need a lock
    private Map<String, OpenFlowDevice> openFlowDeviceHashMap = new ConcurrentHashMap<>();

    public Map<String, OpenFlowDevice> getOpenFlowDeviceHashMap() {
        return openFlowDeviceHashMap;
    }

    private ClusterService clusterService;

    private MastershipStore store;

    public BalanceMastersImpl(ClusterService clusterService, MastershipStore store) {
        this.clusterService = clusterService;
        this.store = store;
        BalanceDaemonThread rtt = new BalanceDaemonThread();
        new Thread(rtt, "balanceDaemon").start();
    }

    public class BalanceDaemonThread implements Runnable {
        public void run() {
            // wait device data
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // balance
            while (flag) {
                log.info("Balance a time");
                balanceRoles();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void balanceRoles() {
        log.info("Start the masters balancing algorithm...");
        init();
        log.info("Success init");
        /*
        getAllLoad();
        log.info("Success getAllLoad");

        getQ();
        log.info("Success getQ");
        getSigma();
        log.info("Success getSigma");
        getOMAndIM();
        log.info("Success getOMAndIM");
        getAllZeta();
        log.info("Success getAllZeta");
        getAllP();
        log.info("Success getAllP");
        for (DistributedControllerNode distributedCOMNode : this.distributedCOMNodeList) {
            DeviceId deviceId = findMaxPDeviceID(distributedCOMNode.getDevices());
            NodeId nodeId = findMaxZetaControllerNodeId(deviceId);
            migrate(distributedCOMNode.getId(), deviceId, nodeId);
        }
        balanceOrphanDevice();
         */
        clearLists();

        log.info("Finish the masters balancing algorithm!");
    }

    private DeviceId findMaxPDeviceID(Set<OpenFlowDevice> openFlowDevices) {
        double maxP = -1.0;
        DeviceId deviceId = null;
        for (OpenFlowDevice openFlowDevice : openFlowDevices) {
            if (openFlowDevice.getP() > maxP) {
                maxP = openFlowDevice.getP();
                deviceId = openFlowDevice.getDeviceId();
            }
        }
        return deviceId;
    }

    private void balanceOrphanDevice() {
        for (OpenFlowDevice openFlowDevice : orphanedDevices) {
            // getAllP
            getAllLoad();
            int size = distributedControllerNodeList.size();
            double[] p = new double[size];
            double loadAverage, sum = 0;
            for (DistributedControllerNode distributedControllerNode : this.distributedControllerNodeList) {
                sum += distributedControllerNode.getLoad();
            }
            loadAverage = sum / this.distributedControllerNodeList.size();
            for (int i = 0; i < size; i++) {
                double load = distributedControllerNodeList.get(i).getLoad();
                p[i] = 1 - (Math.abs(loadAverage-(load+openFlowDevice.getReceivedPacketInRate()))/load);
            }

            // getMaxP
            double maxP = -1;
            NodeId nodeId = null;
            for (int i = 0; i < size; i++) {
                if (maxP < p[i]) {
                    maxP = p[i];
                    nodeId = distributedControllerNodeList.get(i).getNodeId();
                }
            }

            // migrate
            setRole(nodeId, openFlowDevice.getDeviceId(), MASTER);
        }
    }

    private void clearLists() {
        distributedControllerNodeList.clear();
        distributedCOMNodeList.clear();
        distributedCIMNodeList.clear();
    }

    private void migrate(String id, DeviceId deviceId, NodeId nodeId) {
        // clear cIM
        int size = this.distributedCIMNodeList.size();
        for(int i = size - 1; i >= 0; i--) {
            DistributedControllerNode distributedControllerNode = this.distributedCIMNodeList.get(i);
            if (distributedControllerNode.getNodeId().equals(nodeId)) {
                this.distributedCIMNodeList.remove(distributedControllerNode);
                break;
            }
        }

        // if COMNode = CIMNode
        if (id.equals(nodeId.id())) {
            return;
        }

        // resetRole
        setRole(nodeId, deviceId, MASTER);

        // clearDevice
        for (DistributedControllerNode distributedControllerNode : this.distributedCOMNodeList) {
            if (distributedControllerNode.getId().equals(id)) {
                for (OpenFlowDevice openFlowDevice : distributedControllerNode.getDevices()) {
                    if (openFlowDevice.getDeviceId().equals(deviceId)) {
                        distributedControllerNode.getDevices().remove(openFlowDevice);
                        // update
                        getAllLoad();
                        getAllZeta();
                        getAllP();
                        return;
                    }
                }
            }
        }
    }

    private void setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        switch (role) {
            case MASTER:
                store.setMaster(nodeId, deviceId);
                break;
            case STANDBY:
                store.setStandby(nodeId, deviceId);
                break;
            case NONE:
                store.relinquishRole(nodeId, deviceId);
                break;
            default:
                log.info("Unknown role; ignoring");
        }
    }

    private NodeId findMaxZetaControllerNodeId(DeviceId deviceId) {
        NodeId nodeId = null;
        double maxZeta = -1.0;
        for (DistributedControllerNode distributedCIMNode : this.distributedCIMNodeList) {
            if (distributedCIMNode.getSidToZeta().get(deviceId.toString()) > maxZeta) {
                maxZeta = distributedCIMNode.getSidToZeta().get(deviceId.toString());
                nodeId = distributedCIMNode.getNodeId();
            }
        }
        return nodeId;
    }

    private void init() {
        orphanedDevices.clear();
        List<ControllerNode> nodes = newArrayList(clusterService.getNodes());
        for (ControllerNode node : nodes) {
            Set<DeviceId> devicesOf = new HashSet<>(getDevicesOf(node.id()));
            // active
            if (clusterService.getState(node.id()).isActive()) {
                log.info("Node {} has {} devices.    {}", node.id(), devicesOf.size(), devicesOf);
                DistributedControllerNode distributedControllerNode = new DistributedControllerNode(
                        node.id().id(), node.id());
                log.info("new a DistributedControllerNode, id is: " + node.id().id());
                for (DeviceId deviceId : devicesOf) {
                    if (openFlowDeviceHashMap.containsKey(deviceId.toString())) {
                        OpenFlowDevice openFlowDevice = openFlowDeviceHashMap.get(deviceId.toString());
                        openFlowDevice.setDeviceId(deviceId);
                        distributedControllerNode.getDevices().add(openFlowDevice);
                    } else {
                        OpenFlowDevice openFlowDevice = new OpenFlowDevice(deviceId.toString(), deviceId);
                        distributedControllerNode.getDevices().add(openFlowDevice);
                        openFlowDeviceHashMap.put(deviceId.toString(),openFlowDevice);
                        log.info("add a device");
                    }
                    log.info("new a openFlowDevice, id is: " + deviceId.toString());
                }
                this.distributedControllerNodeList.add(distributedControllerNode);
                log.info("list: " + this.distributedControllerNodeList);
            // not active
            } else if (!devicesOf.isEmpty()) {
                log.warn("Inactive node {} has {} orphaned devices.", node.id(), devicesOf.size());
                for (DeviceId deviceId : devicesOf) {
                    if (openFlowDeviceHashMap.containsKey(deviceId.toString())) {
                        OpenFlowDevice openFlowDevice = openFlowDeviceHashMap.get(deviceId.toString());
                        openFlowDevice.setDeviceId(deviceId);
                        orphanedDevices.add(openFlowDevice);
                    } else {
                        OpenFlowDevice openFlowDevice = new OpenFlowDevice(deviceId.toString(), deviceId);
                        orphanedDevices.add(openFlowDevice);
                    }
                }
            }
        }
    }

    private void getAllLoad() {
        for (DistributedControllerNode distributedControllerNode : this.distributedControllerNodeList) {
            // setDataLoad
            distributedControllerNode.setDataLoad(distributedControllerNode.getDevices().size() * this.v);

            // setRouteLoad
            double lambdaSum = 0.0;
            for (OpenFlowDevice openFlowDevice : distributedControllerNode.getDevices()) {
                lambdaSum += openFlowDevice.getReceivedPacketInRate();
            }
            distributedControllerNode.setRouteLoad(this.f * lambdaSum);

            // setStateLoad
            distributedControllerNode.setStateLoad(this.mu * (distributedControllerNodeList.size()-1));

            // setLoad
            distributedControllerNode.setLoad(
                    distributedControllerNode.getDataLoad()+
                            distributedControllerNode.getRouteLoad()+
                            distributedControllerNode.getStateLoad()
            );
            log.info("NodeId: " + distributedControllerNode.getId() +
                    "    " + "load: " + distributedControllerNode.getLoad());
        }
    }

    private void getQ() {
        int size = distributedControllerNodeList.size();
        this.Q = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Q[i][j] = this.distributedControllerNodeList.get(i).getLoad()/
                        this.distributedControllerNodeList.get(j).getLoad();
            }
        }
    }

    private void getSigma() {
        double max = Q[0][0];
        double min = Q[0][0];
        int size = distributedControllerNodeList.size();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (Q[i][j] > max) {
                    max = Q[i][j];
                }
                if (Q[i][j] < min) {
                    min = Q[i][j];
                }
            }
        }
        this.sigma = (max - min) / max;
        log.info("sigma:" + this.sigma);
    }

    private void getOMAndIM() {
        int size = distributedControllerNodeList.size();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) continue;
                if ((Q[i][j] - Q[j][i]) > this.sigma) {
                    this.distributedCOMNodeList.add(distributedControllerNodeList.get(i));
                    this.distributedCIMNodeList.add(distributedControllerNodeList.get(j));
                }
            }
        }
    }

    private void getAllZeta() {
        for (DistributedControllerNode distributedCIMNode : this.distributedCIMNodeList) {
            // setL
            distributedCIMNode.setL(this.f * distributedCIMNode.getDevices().size());

            // setPhi
            double loadVariance = getLoadVariance(this.distributedControllerNodeList);
            distributedCIMNode.setPhi(this.alpha - loadVariance);

            // setSToPhi_
            for (DistributedControllerNode distributedCOMNode : this.distributedCOMNodeList) {
                for (OpenFlowDevice openFlowDevice : distributedCOMNode.getDevices()) {

                    // clone
                    List<DistributedControllerNode> changeDistributedControllerNodeList = new LinkedList<>();
                    for (DistributedControllerNode distributedControllerNode :
                            this.distributedControllerNodeList) {
                        changeDistributedControllerNodeList.add(distributedControllerNode.clone());
                    }

                    for (DistributedControllerNode distributedControllerNode :
                            changeDistributedControllerNodeList) {
                        double preLoad = distributedControllerNode.getLoad();
                        if (distributedControllerNode.getId().equals(distributedCIMNode.getId())) {
                            distributedControllerNode.setLoad(preLoad +
                                    openFlowDevice.getReceivedPacketInRate());
                        } else if (distributedControllerNode.getId().equals(distributedCOMNode.getId())) {
                            distributedControllerNode.setLoad(preLoad -
                                    openFlowDevice.getReceivedPacketInRate());
                        }
                    }
                    double loadVariance_ = getLoadVariance(changeDistributedControllerNodeList);
                    double phi_ = this.alpha - loadVariance_;
                    distributedCIMNode.getSidToPhi_().put(openFlowDevice.getStr(), phi_);
                }
            }

            // setStoZeta
            for (Map.Entry<String, Double> entry : distributedCIMNode.getSidToPhi_().entrySet()) {
                String str = entry.getKey();
                double phi_ = entry.getValue();
                double phi = distributedCIMNode.getPhi();
                double L = distributedCIMNode.getL();
                double zeta;
                log.info("L: " + L);
                if (Math.abs(L) < 0.00001) {
                    zeta = Double.POSITIVE_INFINITY;
                } else {
                    zeta = Math.abs(phi_-phi) / L;
                }
                log.info("zeta: " + zeta);
                distributedCIMNode.getSidToZeta().put(str, zeta);
            }
        }
    }

    private void getAllP() {
        double sum = 0.0;
        double loadAverage;
        for (DistributedControllerNode distributedControllerNode : this.distributedControllerNodeList) {
            sum += distributedControllerNode.getLoad();
        }
        loadAverage = sum / this.distributedControllerNodeList.size();
        for (DistributedControllerNode distributedCOMNode : this.distributedCOMNodeList) {
            for (OpenFlowDevice openFlowDevice : distributedCOMNode.getDevices()) {
                double nextLoad = (distributedCOMNode.getLoad() - openFlowDevice.getReceivedPacketInRate());
                openFlowDevice.setP(1 - (Math.abs(loadAverage-nextLoad)/distributedCOMNode.getLoad()));
            }
        }
    }

    private double getLoadVariance(List<DistributedControllerNode> distributedControllerNodeList) {
        double sum = 0.0;
        double loadAverage;
        for (DistributedControllerNode distributedControllerNode : this.distributedControllerNodeList) {
            sum += distributedControllerNode.getLoad();
        }
        loadAverage = sum / distributedControllerNodeList.size();
        sum = 0.0;
        for (DistributedControllerNode distributedControllerNode : this.distributedControllerNodeList) {
            sum += Math.pow(distributedControllerNode.getLoad()-loadAverage, 2);
        }
        return sum / distributedControllerNodeList.size();
    }

    private Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(nodeId, NODE_ID_NULL);
        return store.getDevices(nodeId);
    }
}
