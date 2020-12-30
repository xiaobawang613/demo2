/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.provider.of.message.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.DefaultControlMessage;
import org.onosproject.cpman.message.ControlMessageProviderService;
import org.onosproject.net.DeviceId;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.onosproject.provider.of.message.impl.OpenFlowControlMessageMapper.lookupControlMessageType;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Collects the OpenFlow messages and aggregates using MetricsService.
 */
public class OpenFlowControlMessageAggregator implements Runnable {

    private final Logger log = getLogger(getClass());

    private static final Set<OFType> OF_TYPE_SET =
            ImmutableSet.of(OFType.PACKET_IN, OFType.PACKET_OUT, OFType.FLOW_MOD,
                    OFType.FLOW_REMOVED, OFType.STATS_REQUEST, OFType.STATS_REPLY);

    //private final Map<OFType, Long> rateMeterMap = new HashMap<OFType, Long>();
    private final Map<OFType, Long> countMeterMap = new HashMap<OFType, Long>();
    private Map<Long,Long> startTimes = Maps.newHashMap();
    private long totalLatency = 0;

    private final DeviceId deviceId;
    private final ControlMessageProviderService providerService;

    private static final String RATE_NAME = "rate";
    private static final String COUNT_NAME = "count";

    private Set<ControlMessage> controlMessages = Sets.newConcurrentHashSet();

    // TODO: this needs to be configurable
    private static final int EXECUTE_PERIOD_IN_SECOND = 1;


    /*
    public OpenFlowControlMessageAggregator(MetricsService metricsService,
                                            ControlMessageProviderService providerService,
                                            DeviceId deviceId) {
        MetricsComponent mc = metricsService.registerComponent(deviceId.toString());

        OF_TYPE_SET.forEach(type -> {
            MetricsFeature metricsFeature = mc.registerFeature(type.toString());
            Meter rateMeter = metricsService.createMeter(mc, metricsFeature, RATE_NAME);
            Meter countMeter = metricsService.createMeter(mc, metricsFeature, COUNT_NAME);

            rateMeterMap.put(type, rateMeter);
            countMeterMap.put(type, countMeter);
        });
        this.deviceId = deviceId;
        this.providerService = providerService;
        metricsService.notifyReporters();
    }
*/
    public OpenFlowControlMessageAggregator(ControlMessageProviderService providerService, DeviceId deviceId){
        OF_TYPE_SET.forEach(type -> {
            countMeterMap.put(type, 0L);
        });
        /*
        OF_TYPE_SET.forEach(type -> {
            rateMeterMap.put(type, 0L);
        });

         */
        this.deviceId = deviceId;
        this.providerService = providerService;

    }
    /**
     * Increments the meter rate by n, and the meter count by 1.
     *
     * @param msg OpenFlow message
     */
    public void increment(OFMessage msg) {
        countMeterMap.put(msg.getType(), countMeterMap.get(msg.getType())+1);
        //rateMeterMap.get(msg.getType()).mark(msg.toString().length());
       // countMeterMap.get(msg.getType()).mark(1);
    }

    public void addLatency(long latency){
        totalLatency+=latency;
    }

    @Override
    public void run() {
        // update 1 minute statistic information of all control messages

        for(OFType type:OF_TYPE_SET){
            controlMessages.add(
                    new DefaultControlMessage(lookupControlMessageType(type),
                            deviceId, getLoad(type), getRate(type), getCount(type),
                            System.currentTimeMillis()));
        }


        /*
        OF_TYPE_SET.forEach(type -> controlMessages.add(
                new DefaultControlMessage(lookupControlMessageType(type),
                        deviceId, getLoad(type), getRate(type), getCount(type),
                        System.currentTimeMillis())));
        */
        log.debug("sent aggregated control message");
        providerService.updateStatsInfo(deviceId, ImmutableSet.copyOf(controlMessages));
        controlMessages.clear();
        for(OFType type:countMeterMap.keySet()){
            countMeterMap.put(type, 0L);
        }
        /*
        for(OFType type:rateMeterMap.keySet()){
            rateMeterMap.put(type, 0L);
        }

         */

    }

    /**
     * Returns the average load value.
     *
     * @param type OpenFlow message type
     * @return load value
     */
    private long getLoad(OFType type) {
        /*
        if (countMeterMap.get(type).getOneMinuteRate() == 0D) {
            return 0L;
        }
        return (long) (rateMeterMap.get(type).getOneMinuteRate() /
                       countMeterMap.get(type).getOneMinuteRate());

         */
        return 0;
    }

    /**
     * Returns the average meter rate within recent 1 minute.
     *
     * @param type OpenFlow message type
     * @return rate value
     */
    private long getRate(OFType type) {
        //return (long) rateMeterMap.get(type).getOneMinuteRate();
        return  (long) countMeterMap.get(type)/2;
    }

    /**
     * Returns the average meter count within recent 1 minute.
     *
     * @param type OpenFlow message type
     * @return count value
     */
    private long getCount(OFType type) {
        /*
        return (long) (countMeterMap.get(type).getOneMinuteRate()
                * EXECUTE_PERIOD_IN_SECOND);

         */
        return countMeterMap.get(type);
    }

    public long getTotalLatency() {
        return totalLatency;
    }

    public Map<Long, Long> getStartTimes() {
        return startTimes;
    }
}
