package org.onosproject.cpman.impl;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.List;

public class Alternative implements Comparable<Alternative>{
    int comp;
    // String num;
    List<DeviceId> mig_switch;
    NodeId tar_controller;
    NodeId sou_controller;
    double[] attribute=new double[Topsis.D];	//指标
    double bestdis,worsedis,c;	//与正、负理想解的欧式距离,c为贴进度

    public Alternative(List<DeviceId> mig_switch, NodeId tar_controller, NodeId sou_controller) {
        this.mig_switch = mig_switch;
        this.tar_controller = tar_controller;
        this.sou_controller = sou_controller;
    }

    public Alternative(List<DeviceId> mig_switch, NodeId sou_controller, NodeId tar_controller, double[] d) {
        this.mig_switch=mig_switch;
        this.sou_controller=sou_controller;
        this.tar_controller=tar_controller;
        for(int i = 0; i< Topsis.D; i++){
            this.attribute[i]=d[i];
        }
    }
    public void weighted(){		//赋权
        for(int i = 0; i< Topsis.D; i++){
            this.attribute[i]*= Topsis.weight[i];
        }
    }

    @Override
    public int compareTo(Alternative a) {
        return (int) (this.attribute[comp]-a.attribute[comp]);
    }
}
