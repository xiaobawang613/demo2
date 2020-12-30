package org.onosproject.cpman.impl;

import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onlab.util.GeoLocation;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.TopologyEdge;

import java.util.List;
import java.util.Map;

import static java.lang.Double.MAX_VALUE;

public class GeoLinkWeighter implements LinkWeigher {
    private static final double MAX_KM = 40_075 / 2.0;

    private Map<String, List<Double>> geoMap;

    private DeviceService deviceService;
    /**
     * Creates a new link-weight with access to the specified device service.
     *
     *
     */
    public GeoLinkWeighter(DeviceService deviceService) {
        this.geoMap = Constants.GEO_MAP;
        this.deviceService = deviceService;
    }

    @Override
    public Weight getInitialWeight() {
        return ScalarWeight.toWeight(0.0);
    }

    @Override
    public Weight getNonViableWeight() {
        return ScalarWeight.NON_VIABLE_WEIGHT;
    }

    @Override
    public Weight weight(TopologyEdge edge) {
        GeoLocation src = getLocation(edge.link().src().deviceId());
        GeoLocation dst = getLocation(edge.link().dst().deviceId());
        return ScalarWeight.toWeight(src != null && dst != null ? src.kilometersTo(dst) : MAX_KM);
    }

    private GeoLocation getLocation(DeviceId deviceId) {

        double latitude = geoMap.get(deviceId.toString()).get(0);
        double longitude = geoMap.get(deviceId.toString()).get(1);
        return latitude == MAX_VALUE || longitude == MAX_VALUE ? null :
                new GeoLocation(latitude, longitude);
    }
}


