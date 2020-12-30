package org.onosproject.cpman.impl;

import org.onlab.util.GeoLocation;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.Map;

public class Tools {

    /**
     *获得两个直接相连节点之间的最短时延
     * @param geoLocation1 原节点
     * @param geoLocation2 目标节点
     * @return 最短时延ms
     */
    public static double getDelay(GeoLocation geoLocation1, GeoLocation geoLocation2){
        double distance = geoLocation1.kilometersTo(geoLocation2);
        return (distance*1000)/200000;
    }

    public static double getAvgDelay( Map<NodeId,Map<DeviceId,Double>> c_s_delay){
        int count=0;
        double sum=0;
        for(NodeId nodeId:c_s_delay.keySet()){
            Map<DeviceId,Double> temp = c_s_delay.get(nodeId);
            for(DeviceId deviceId:temp.keySet()){
                count++;
                sum+=temp.get(deviceId);
            }
        }
        return sum/count;
    }
    public static double getControllerLoadVarienceBefore(Map<NodeId, Map<DeviceId,Long>> load_map){
        int i=0;
        double[] load = new double[load_map.size()-1];
        double ave=0;
        double var=0;
        for(NodeId controllerNode:load_map.keySet()){
            if(!controllerNode.id().equals("172.20.0.5")){
                long sum=0;
                Map<DeviceId,Long> temp =load_map.get(controllerNode);
                for(DeviceId deviceId:temp.keySet()){
                    sum+=temp.get(deviceId);
                }
                // log.info("sum有"+sum);
                load[i++]=(double)sum;
            }
        }
        for(int j=0;j<load.length;j++){
            ave+=load[j];
        }
        ave/=load.length;
        //log.info("ave有"+ave);
        for(int j=0;j<load.length;j++){
            var+=Math.pow(load[j]-ave,2);
        }
        return var/load.length;
    }

    public static double getControllerLoadVarienceAfter(Map<NodeId, Map<DeviceId,Long>> load_map, double switchLoad,
                                                        NodeId source, NodeId target){
        int i=0;
        double[] load = new double[load_map.size()-1];
        double ave=0;
        double var=0;
        for(NodeId nodeId:load_map.keySet()){
            if(!nodeId.id().equals("172.20.0.5")){
                Map<DeviceId,Long> temp = load_map.get(nodeId);
                long sum = 0;
                for(DeviceId deviceId:temp.keySet()){
                    sum+=temp.get(deviceId);
                }
                if(nodeId.id().equals(source.id())){
                    sum-=switchLoad;
                }
                if(nodeId.id().equals(target.id())){
                    sum+=switchLoad;
                }
                load[i++]=sum;
            }
        }

        for(int j=0;j<load.length;j++){
            ave+=load[j];
        }
        ave/=load.length;
        for(int j=0;j<load.length;j++){
            var+=Math.pow(load[j]-ave,2);
        }
        return var/load.length;
    }



}
