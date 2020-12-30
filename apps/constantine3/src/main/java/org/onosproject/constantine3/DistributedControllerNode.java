package org.onosproject.constantine3;

import org.onosproject.cluster.NodeId;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DistributedControllerNode {
    private String id;
    private NodeId nodeId;

    private Set<OpenFlowDevice> devices = new HashSet<>();

    private double dataLoad;
    private double routeLoad;
    private double stateLoad;
    private double load;

    private double L;
    private double phi;
    private Map<String, Double> sidToPhi_ = new LinkedHashMap<>();
    private Map<String, Double> sidToZeta = new LinkedHashMap<>();

    public DistributedControllerNode(String id, NodeId nodeId) {
        this.id = id;
        this.nodeId = nodeId;
    }

    public DistributedControllerNode clone() {
        return new DistributedControllerNode(
                this.id,
                this.nodeId,
                this.devices,
                this.dataLoad,
                this.routeLoad,
                this.stateLoad,
                this.load,
                this.L,
                this.phi,
                this.sidToPhi_,
                this.sidToZeta
        );
    }

    public DistributedControllerNode(
            String id,
            NodeId nodeId,
            Set<OpenFlowDevice> devices,
            double dataLoad,
            double routeLoad,
            double stateLoad,
            double load,
            double L,
            double phi,
            Map<String, Double> sidToPhi_,
            Map<String, Double> sidToZeta
    ) {
        this.id = id;
        this.nodeId = nodeId;
        this.devices = devices;
        this.dataLoad = dataLoad;
        this.routeLoad = routeLoad;
        this.stateLoad = stateLoad;
        this.load = load;
        this.L = L;
        this.phi = phi;
        this.sidToPhi_ = sidToPhi_;
        this.sidToZeta = sidToZeta;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public double getL() {
        return L;
    }

    public Map<String, Double> getSidToPhi_() {
        return sidToPhi_;
    }

    public Map<String, Double> getSidToZeta() {
        return sidToZeta;
    }

    public void setSidToPhi_(Map<String, Double> sidToPhi_) {
        this.sidToPhi_ = sidToPhi_;
    }

    public void setSidToZeta(Map<String, Double> sidToZeta) {
        this.sidToZeta = sidToZeta;
    }

    public void setL(double l) {
        L = l;
    }

    public void setPhi(double phi) {
        this.phi = phi;
    }

    public double getPhi() {
        return phi;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setDevices(Set<OpenFlowDevice> devices) {
        this.devices = devices;
    }

    public Set<OpenFlowDevice> getDevices() {
        return devices;
    }

    public void setDataLoad(double dataLoad) {
        this.dataLoad = dataLoad;
    }

    public void setRouteLoad(double routeLoad) {
        this.routeLoad = routeLoad;
    }

    public void setStateLoad(double stateLoad) {
        this.stateLoad = stateLoad;
    }

    public void setLoad(double load) {
        this.load = load;
    }

    public double getDataLoad() {
        return dataLoad;
    }

    public double getRouteLoad() {
        return routeLoad;
    }

    public double getStateLoad() {
        return stateLoad;
    }

    public double getLoad() {
        return load;
    }
}
