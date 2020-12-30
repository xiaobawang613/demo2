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
package org.onosproject.cpman.impl;

import com.google.common.collect.*;
import com.google.common.util.concurrent.Futures;
import org.onlab.util.GeoLocation;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.*;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.*;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.PortStatisticsService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.nodemetrics.NodeCpuUsage;
import org.onosproject.nodemetrics.NodeDiskUsage;
import org.onosproject.nodemetrics.NodeMemoryUsage;
import org.onosproject.nodemetrics.NodeMetricsService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.onosproject.cpman.ControlResource.*;

/**
 * Control plane monitoring service class.
 */
@Component(immediate = true, service = ControlPlaneMonitorService.class)
public class ControlPlaneMonitor implements ControlPlaneMonitorService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private MetricsDatabase cpuMetrics;
    private MetricsDatabase memoryMetrics;
    private Map<DeviceId, MetricsDatabase> controlMessageMap;
    private Map<String, MetricsDatabase> diskMetricsMap;
    private Map<String, MetricsDatabase> networkMetricsMap;
    private Map<NodeId, NodeCpuUsage> cpuStore;
    private Map<NodeId, NodeMemoryUsage> memoryStore;
    private Map<NodeId, NodeDiskUsage> diskStore;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipAdminService mastershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NodeMetricsService nodeMetricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PortStatisticsService portStatisticsService;
    private int initialDelay = 10;
    private int schedulePeriod1 = 10;
    private int schedulePeriod2 = 5;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
    private AtomicReference<Future> nextTask = new AtomicReference<>();
    private AtomicReference<Future> nextTask2 = new AtomicReference<>();
    private static final String DEFAULT_RESOURCE = "default";

    private static final Set RESOURCE_TYPE_SET =
            ImmutableSet.of(Type.CONTROL_MESSAGE, Type.DISK, Type.NETWORK);

    private static final MessageSubject CONTROL_STATS =
            new MessageSubject("control-plane-stats");

    private static final MessageSubject CONTROL_RESOURCE =
            new MessageSubject("control-plane-resources");

    private Map<ControlMetricType, Double> cpuBuf;
    private Map<ControlMetricType, Double> memoryBuf;
    private Map<String, Map<ControlMetricType, Double>> diskBuf;
    private Map<String, Map<ControlMetricType, Double>> networkBuf;
    private Map<DeviceId, Map<ControlMetricType, Double>> ctrlMsgBuf;

    private Map<Type, Set<String>> availableResourceMap;
    private Set<DeviceId> availableDeviceIdSet;

    private static final String METRIC_TYPE_NULL = "Control metric type cannot be null";
    private static final String RESOURCE_TYPE_NULL = "Control resource type cannot be null";

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder()
                    .register(KryoNamespaces.API)
                    .register(ControlMetricsRequest.class)
                    .register(ControlResourceRequest.class)
                    .register(ControlLoadSnapshot.class)
                    .register(ControlMetricType.class)
                    .register(ControlResource.Type.class)
                    .register(TimeUnit.class)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID).build());

    @Activate
    public void activate() {
        cpuMetrics = genMDbBuilder(DEFAULT_RESOURCE, Type.CPU, CPU_METRICS);
        memoryMetrics = genMDbBuilder(DEFAULT_RESOURCE, Type.MEMORY, MEMORY_METRICS);
        controlMessageMap = Maps.newConcurrentMap();
        diskMetricsMap = Maps.newConcurrentMap();
        networkMetricsMap = Maps.newConcurrentMap();

        cpuBuf = Maps.newConcurrentMap();
        memoryBuf = Maps.newConcurrentMap();
        diskBuf = Maps.newConcurrentMap();
        networkBuf = Maps.newConcurrentMap();
        ctrlMsgBuf = Maps.newConcurrentMap();

        availableResourceMap = Maps.newConcurrentMap();
        availableDeviceIdSet = Sets.newConcurrentHashSet();

        communicationService.<ControlMetricsRequest, ControlLoadSnapshot>addSubscriber(CONTROL_STATS,
                SERIALIZER::decode, this::handleMetricsRequest, SERIALIZER::encode);

        communicationService.<ControlResourceRequest, Set<String>>addSubscriber(CONTROL_RESOURCE,
                SERIALIZER::decode, this::handleResourceRequest, SERIALIZER::encode);
        log.info("Started");
        initialPlacement();
        if(clusterService.getLocalNode().id().id().equals("172.20.0.5")){
            monitorLoad();
        }

    }

    @Deactivate
    public void deactivate() {

        // TODO: need to handle the mdb close.
        cpuBuf.clear();
        memoryBuf.clear();
        diskBuf.clear();
        networkBuf.clear();
        ctrlMsgBuf.clear();

        communicationService.removeSubscriber(CONTROL_STATS);
        communicationService.removeSubscriber(CONTROL_RESOURCE);
        executorService.shutdown();
        log.info("Stopped");
    }

    /**
     * 设置控制器与交换机的初始映射关系
     */
    public void initialPlacement(){

        List<CompletableFuture<Void>> futures = Lists.newLinkedList();

        for(String uri:Constants.INITIAL_PLACEMENT.keySet()){
            futures.add(mastershipAdminService.setRole(new NodeId(Constants.INITIAL_PLACEMENT.get(uri)),
                    DeviceId.deviceId(uri), MastershipRole.MASTER));
        }
        CompletableFuture<Void> future =
                allOf(futures.toArray(new CompletableFuture[futures.size()]));

        Futures.getUnchecked(future);

    }

    /**
     * 每隔10秒监测各个控制器的负载
     */
    public void monitorLoad(){
        executorService.scheduleAtFixedRate(new LoadTask(),
                initialDelay, schedulePeriod2, TimeUnit.SECONDS);
        /*
        if (!nextTask.compareAndSet(null, task)) {
            task.cancel(false);
        }
         */
    }

    public class LoadTask implements Runnable{
        @Override
        public void run() {
           // nextTask.set(null);

            Map<NodeId,Map<DeviceId,Long>> loads = null;
            loads = collectLoad();
            NodeId overIndex = new NodeId("null");
            long max=0;
            long average = 0;
            String result="ControllerLoad ";
            for(NodeId nodeId:loads.keySet()){
                Map<DeviceId,Long> temp = loads.get(nodeId);
                long sum = 0;
                for(DeviceId deviceId:temp.keySet()){
                    sum+=temp.get(deviceId);
                    if(sum>=max){
                        max=sum;
                        overIndex=nodeId;
                    }
                }
                result+="Controller"+nodeId.id()+":"+sum+" ";
                if(!nodeId.id().equals("172.20.0.5")){
                    average+=sum;
                }
            }
            average=average/(loads.size()-1);
            log.info(result);



            if(max>0){
                Alternative bestChoice = makeDecision(loads,overIndex,max-average,getEdgeSwitches(overIndex));
                if(bestChoice != null) {
                    List<CompletableFuture<Void>> futures = Lists.newLinkedList();
                    for (DeviceId deviceId : bestChoice.mig_switch) {
                        futures.add(mastershipAdminService.setRole(new NodeId(bestChoice.tar_controller.id()),
                                DeviceId.deviceId(deviceId.toString()), MastershipRole.MASTER));
                    }
                    CompletableFuture<Void> future = allOf(futures.toArray(new CompletableFuture[futures.size()]));
                    Futures.getUnchecked(future);
                }
            }




             /*
            for(NodeId nodeId:loads.keySet()){
                Map<DeviceId,Long> temp = loads.get(nodeId);
                for(DeviceId deviceId:temp.keySet()){
                    Map<ConnectPoint, SummaryFlowEntryWithLoad> map= flowStatisticService.loadSummary(deviceService.getDevice(deviceId));
                    for(ConnectPoint cp:map.keySet()){
                        SummaryFlowEntryWithLoad load = map.get(cp);
                        log.info("交换机"+deviceId.toString()+"的"+cp.port().name()+"端口load为"+load.totalLoad().latest());
                    }
                }
            }


              */
            /*
            for(NodeId nodeId:loads.keySet()){
                Map<DeviceId,Set<NodeId>> map = getEdgeSwitches(nodeId);
                for(DeviceId deviceId:map.keySet()){
                    Set<NodeId> set = map.get(deviceId);
                    String rr ="";
                    for(NodeId n:set){
                        rr+=n.id()+" ";
                    }
                    log.info("控制器"+nodeId+"的边缘交换机有"+deviceId.toString()+",其邻近控制器为:"+rr);

                }
            }
             */
            /*
            log.info("LoadVarience:"+Tools.getControllerLoadVarienceBefore(loads));
            log.info("Controller "+overIndex.id()+" overloaded!");
            Map<NodeId,Map<DeviceId,Double>> c_s_delay = getCSDelay(loads);
            log.info("AverageDelay:"+Tools.getAvgDelay(c_s_delay));
            cpuStore = nodeMetricsService.cpu();
            String cpuRe = "CpuUsage ";
            for(NodeId id : cpuStore.keySet()){
                cpuRe+="Controller"+id.id()+":"+cpuStore.get(id).usage()+" ";
            }
            log.info(cpuRe);
            memoryStore = nodeMetricsService.memory();
            String memRe = "MemUsage ";
            for(NodeId id : memoryStore.keySet()){
                memRe+="Controller"+id.id()+":"+memoryStore.get(id).usage()+" ";
            }
            log.info(memRe);
            String diskRe = "DiskUsage ";
            diskStore = nodeMetricsService.disk();
            for(NodeId id : diskStore.keySet()){
                diskRe+="Controller"+id.id()+":"+diskStore.get(id).usage()+" ";
            }
            log.info(diskRe);


            if(max>0){
                //Alternative bestchoice = makeDecision(loads,overIndex);
                //Alternative bestchoice = makeDecisionByElasticon(overIndex, getDelays());
                Alternative bestchoice = makeDecisionByDALB(overIndex,loads);
                        Futures.getUnchecked(mastershipAdminService.setRole(new NodeId(bestchoice.tar_controller.id()),
                        DeviceId.deviceId(bestchoice.mig_switch.get(0).toString()), MastershipRole.MASTER));

            }

             */
            log.info("LoadVarience:"+Tools.getControllerLoadVarienceBefore(loads));
            log.info("Controller "+overIndex.id()+" overloaded!");
            Map<NodeId,Map<DeviceId,Double>> c_s_delay = getCSDelay(loads);
            log.info("AverageDelay:"+Tools.getAvgDelay(c_s_delay));
            log.info("ALL Completed!");

        }
    }

    /**
     * 收集各控制器各交换机单位时间内packet-in数量
     * @return
     */
    public Map<NodeId,Map<DeviceId,Long>> collectLoad() {
        List<ControllerNode> nodes = Lists.newArrayList(clusterService.getNodes());
        //log.info(nodes.size()+"");
        Map<NodeId,Map<DeviceId,Long>> load_map = new HashMap<>();
        for(ControllerNode node:nodes){
            List<DeviceId> devices= Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
           // log.info(devices.size()+"");
            Map<DeviceId,Long>  temp = new HashMap<>();
            Map<DeviceId,CompletableFuture<ControlLoadSnapshot>> futures = new HashMap<>();
            for(DeviceId id:devices){
                futures.put(id,getLoad(node.id(),
                        ControlMetricType.INBOUND_PACKET, Optional.of(id)));
               /*
                if(controlLoadSnapshot != null){
                    log.info("控制器"+node.id().id()+"下有交换机" +id+",其load为"+controlLoadSnapshot.latest());
                    temp.put(id,controlLoadSnapshot.latest());
                }
                else{
                    log.info("我好菜呀，获取不到交换机"+id.toString()+"的负载呢");
                }

                */
            }
            for(DeviceId id:futures.keySet()){
                ControlLoadSnapshot controlLoadSnapshot = null;
                try {
                    controlLoadSnapshot = futures.get(id).get();
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    log.error("获取交换机负载失败。。");
                } catch (ExecutionException e) {
                    //e.printStackTrace();
                    log.error("获取交换机负载失败。。");
                }
                if(controlLoadSnapshot != null){
                   // log.info("控制器"+node.id().id()+"下有交换机" +id+",其load为"+controlLoadSnapshot.latest());
                    temp.put(id,controlLoadSnapshot.latest());
                }
                /*
                else{
                    log.info("我好菜呀，获取不到交换机"+id.toString()+"的负载呢");
                }
                 */
            }
            load_map.put(node.id(),temp);
        }
        return load_map;
    }

    /**
     * 根据DALB方案作出最优迁移决策
     * @param index 过载控制器的NodeId
     * @param loads 各控制器掌管交换机的负载
     * @return 最优迁移决策
     */
    public Alternative makeDecisionByDALB(NodeId index, Map<NodeId,Map<DeviceId,Long>> loads){

        long min = Long.MAX_VALUE;
        NodeId nodeId =null;
        DeviceId deviceId_mgr = null;
        for(NodeId controllerNode:loads.keySet()){
            if(!controllerNode.id().equals("172.20.0.5")){
                if(controllerNode.id().equals(index.id())){
                    long max = 0;
                    Map<DeviceId,Long> temp =loads.get(controllerNode);
                    for(DeviceId deviceId:temp.keySet()){
                        long load = temp.get(deviceId);
                        if(load>max){
                            max=load;
                            deviceId_mgr=deviceId;
                        }
                    }
                }
                else{
                    long sum=0;
                    Map<DeviceId,Long> temp =loads.get(controllerNode);
                    for(DeviceId deviceId:temp.keySet()){
                        sum+=temp.get(deviceId);
                    }
                    // log.info("sum有"+sum);
                    if(sum<min){
                        min=sum;
                        nodeId=controllerNode;
                    }
                }
            }

        }
        return new Alternative(Arrays.asList(deviceId_mgr),nodeId,index);
    }

    /**
     * 根据ElastiCon方案作出最优决策
     * @param index 过载控制器的NodeId
     * @param delays 各节点之间的传输时延
     * @return 最优迁移决策
     */
    public Alternative makeDecisionByElasticon(NodeId index, Map<DeviceId,
            Map<DeviceId,Double>> delays){
        Random rd = new Random();
        List<DeviceId> deviceIds = Lists.newArrayList(mastershipService.getDevicesOf(index));
        Set<ControllerNode> controllerNodes = clusterService.getNodes();
        DeviceId deviceId = deviceIds.get(rd.nextInt(deviceIds.size()));
        double min = Double.MAX_VALUE;
        NodeId nodeId = null;
        for(ControllerNode controllerNode:controllerNodes){
            if(!controllerNode.id().id().equals(index.id()) && !controllerNode.id().id().equals("172.20.0.5")){
                double delay = delays.get(deviceId).
                        get(Constants.CONTROLLER_SWITCH_MAPPING.get(controllerNode.id().id()));
                if(delay<min){
                    min=delay;
                    nodeId=controllerNode.id();
                }
            }
        }
        Alternative bestchoice = new Alternative(Arrays.asList(deviceId),nodeId,index);
        return bestchoice;

    }

    /**
     * 根据Entropy-Topsis方案作出最优决策
     * @param list 迁移备选方案列表
     * @param index 过载控制器的NodeId
     * @param delays 各节点之间的传输时延
     * @param load_map 各控制器掌管的交换机的负载
     */
    public void makeDecisionByTopsis(List<Alternative> list, NodeId index, Map<DeviceId,
            Map<DeviceId,Double>> delays, Map<NodeId,Map<DeviceId,Long>> load_map){
        Set<DeviceId> deviceIds = mastershipService.getDevicesOf(index);
        Set<ControllerNode> controllerNodes = clusterService.getNodes();

        log.info(controllerNodes.size()+"");
        log.info(deviceIds.size()+"");
        /*
        log.info(load_map.size()+"");
        for(NodeId nodeId:load_map.keySet()){
            Map<DeviceId,Long> temp = load_map.get(nodeId);
            for(DeviceId deviceId:temp.keySet()){
                log.info(temp.get(deviceId)+" ");
            }
        }
         */
        for(DeviceId deviceId:deviceIds){
            for(ControllerNode controllerNode:controllerNodes){
                if(!controllerNode.id().id().equals(index.id()) && !controllerNode.id().id().equals("172.20.0.5")){
                      //  log.info("迁移交换机为"+deviceId.toString());
                       // log.info("源控制器为"+index.id());
                       // log.info("目标控制器为"+controllerNode.id().id());
                      //  log.info("交换机的load为"+load_map.get(index).get(deviceId));
                    /*
                        int count1=0;
                        for(NodeId nodeId:load_map.keySet()){
                          Map<DeviceId,Long> temp = load_map.get(nodeId);
                          for(DeviceId ii:temp.keySet()) {
                              log.info(temp.get(ii) + " ");
                              count1++;
                          }
                    }
                        log.info("load_map completed,总计"+count1);


                     */
                      //  log.info("load size 为"+load_map.size());
                    //log.info("交换机与目标控制器距离为"+delays.get(Constants.CONTROLLER_SWITCH_MAPPING.get(controllerNode.id().id())
                    //).get(deviceId));
                        double varienceAfter = Tools.getControllerLoadVarienceAfter(load_map,(double)load_map.get(index).get(deviceId),
                                index, controllerNode.id());
                        //log.info("迁移后的负载方差为"+varienceAfter);
                        double[] d ={varienceAfter,delays.get(Constants.CONTROLLER_SWITCH_MAPPING.get(controllerNode.id().id())
                        ).get(deviceId)};
                        list.add(new Alternative(Arrays.asList(deviceId), index, controllerNode.id(),d ));
                        //log.info("备选策略添加成功");
                }
            }
        }
        Topsis.setAl(list);
        Topsis.setWeight();
        Topsis.standardData();
        Topsis.cal();
        log.info("Topsis completed!");
    }

    /**
     * 根据排好序的迁移备选方案列表选出与理想解相似度最高的方案
     * @param load_map 过载控制器的NodeId
     * @param overLoadId 过载控制器的NodeId
     * @return 最优迁移策略
     */
    public Alternative makeDecision(Map<NodeId,Map<DeviceId,Long>> load_map, NodeId overLoadId,
                                    double threshold,Map<DeviceId,Set<NodeId>> map){
        /*
        for(NodeId nodeId:c_s_delay.keySet()){
            Map<DeviceId,Double> temp = c_s_delay.get(nodeId);
            for(DeviceId deviceId: temp.keySet()){
                log.info("交换机"+deviceId.toString()+"与控制器"+nodeId.id()+"的距离为"+temp.get(deviceId));
            }
        }
         */
        Topology topology = topologyService.currentTopology();

        Map<DeviceId, Map<DeviceId,Double>> delays = getDelays();
        /*
        for(DeviceId deviceId:delays.keySet()){
            Map<DeviceId,Double> temp = delays.get(deviceId);
            for(DeviceId id: temp.keySet()){
                log.info("交换机"+deviceId.toString()+"与交换机"+id.toString()+"的距离为"+temp.get(id));
            }
        }
         */


        List<Alternative> candidates = new LinkedList<>();
        //makeDecisionByTopsis(candidates, overLoadId, delays, load_map);
       // log.info("备选策略有"+Topsis.getAl().size()+"个");
        searchAndCalAlternative(candidates,map,overLoadId,load_map,threshold,delays);
       // log.info("备选策略有"+Topsis.getAl().size()+"个");
        if(Topsis.getAl().size() == 0){
            return null;
        }
        Alternative bestchoice = Topsis.getAl().get(0);
    //    log.info("最佳策略:迁移交换机为"+bestchoice.mig_switch.toString()+",原控制器为:"+bestchoice.sou_controller.id()+",目标控制器为:"+
               // bestchoice.tar_controller.id()+",负载方差为:"+bestchoice.attribute[0]+"延迟为:"+bestchoice.attribute[1]);


        return bestchoice;
    }



    /**
     * 获得控制器与其直连交换机的delay
     * @param load_map 各交换机的负载
     * @return 控制器与其直连交换机的传输时延
     */
    public Map<NodeId,Map<DeviceId,Double>> getCSDelay(Map<NodeId,Map<DeviceId,Long>> load_map){
        Map<NodeId,Map<DeviceId,Double>> c_s_delay = new HashMap<>();
        for(NodeId node:load_map.keySet()){
            Map<DeviceId,Double> temp = new HashMap<>();
            for(DeviceId deviceId :load_map.get(node).keySet()){
                double lan1 = Constants.GEO_MAP.get(deviceId.toString()).get(0);
                double lon1 = Constants.GEO_MAP.get(deviceId.toString()).get(1);
                double lan2 = Constants.GEO_MAP.get(Constants.CONTROLLER_SWITCH_MAPPING.get(node.id()).toString()).get(0);
                double lon2 = Constants.GEO_MAP.get(Constants.CONTROLLER_SWITCH_MAPPING.get(node.id()).toString()).get(1);
                temp.put(deviceId, Tools.getDelay(new GeoLocation(lan1, lon1), new GeoLocation(lan2,lon2)));
                // log.info(temp.get(deviceId)+"");
            }
            c_s_delay.put(node, temp);
        }
        return c_s_delay;
    }

    /**
     * 获得各交换机节点之间的delay
     * @return 获得各交换机节点之间的delay
     */
    public Map<DeviceId, Map<DeviceId,Double>> getDelays(){
        Map<DeviceId, Map<DeviceId,Double>> delays = new HashMap<>();
        List<Device> devices = Lists.newArrayList(deviceService.getAvailableDevices());
        //int count=0;
        for(Device d1:devices){
            Map<DeviceId,Double> temp = new HashMap<>();
            /*
            double lan1 = Constants.GEO_MAP.get(d1.id().toString().trim()).get(0);
            double lon1 = Constants.GEO_MAP.get(d1.id().toString().trim()).get(1);

             */
            for(Device d2:devices){
                //count++;
                //log.info(count+"");
                /*
                double lan2 = Constants.GEO_MAP.get(d2.id().toString().trim()).get(0);
                double lon2 = Constants.GEO_MAP.get(d2.id().toString().trim()).get(1);
                temp.put(d2.id(),Tools.getDelay(new GeoLocation(lan1, lon1), new GeoLocation(lan2,lon2)));
                 */
                // log.info("距离为"+temp.get(d2.id()));
                double delay = 0;
                if(!d1.id().toString().equals(d2.id().toString())){
                    Set<Path> path =  topologyService.getKShortestPaths(topologyService.currentTopology(), d1.id(), d2.id(),
                            new GeoLinkWeighter(deviceService),1);
                    //log.info("path条数为"+path.size());
                    for(Path p:path){
                        delay=p.cost()*1000/200000;
                       // log.info("距离为"+delay);
                    }
                }
                temp.put(d2.id(),delay);

            }
            delays.put(d1.id(),temp);
        }
        return delays;
    }

    /**
     * 找出过载域与别的控制域直接相连的交换机
     * @param overLoadController 过载控制器
     * @return 边缘交换机与邻近控制器组的映射关系
     */
    public Map<DeviceId,Set<NodeId>> getEdgeSwitches(NodeId overLoadController){
        List<DeviceId> deviceIds = new ArrayList<>(mastershipService.getDevicesOf(overLoadController));
        Map<DeviceId,Set<NodeId>> map= Maps.newHashMap();
        for(DeviceId deviceId:deviceIds){
            Set<Link> links = linkService.getDeviceEgressLinks(deviceId);
            for(Link link:links){
                if(link.dst().elementId() instanceof DeviceId && !mastershipService.getMasterFor
                        (link.dst().deviceId()).id().equals(overLoadController.id())){
                    if(!map.containsKey(deviceId)){
                        map.put(deviceId,new HashSet<>(){{add(mastershipService.getMasterFor
                                (link.dst().deviceId()));}});
                    }else{
                        map.get(deviceId).add(mastershipService.getMasterFor
                                (link.dst().deviceId()));
                    }
                }
            }
        }
        return map;
    }

    public void searchAndCalAlternative(List<Alternative> list, Map<DeviceId,Set<NodeId>> map, NodeId overLoadController,
                                  Map<NodeId,Map<DeviceId,Long>> load_map,double threshold,
                                        Map<DeviceId, Map<DeviceId,Double>> delays){
        Set<DeviceId> deviceIds = map.keySet();
      //  log.info("边缘交换机有:"+deviceIds.size()+"个");
      //  log.info("阈值为:"+threshold);
        Map<DeviceId, ArrayList<DeviceId>> candidateSwitches = new HashMap<>();
        for(DeviceId deviceId:deviceIds){
            ArrayList<DeviceId> temp = new ArrayList<>();
            temp.add(deviceId);
            candidateSwitches.put(deviceId,temp);
        }
        int count1=1;
        for(DeviceId deviceId:deviceIds){
        //    log.info("这是第"+count1+"个边缘交换机");
            ArrayList<DeviceId> switchGroups = candidateSwitches.get(deviceId);
        //    log.info("switchGroups个数:"+switchGroups.size());
            long sum = load_map.get(overLoadController).get(switchGroups.get(0));
        //    log.info("初始节点load为:"+sum);
            while(sum<=threshold){
                Set<Link> links = linkService.getDeviceEgressLinks(switchGroups.get(switchGroups.size()-1));
                Set<Link> neighbourSwitches = Sets.newHashSet();
                for(Link link:links){
                    if(link.dst().elementId() instanceof DeviceId && !switchGroups.contains(link.dst().deviceId())
                    && mastershipService.getMasterFor(link.dst().deviceId()).id().equals(overLoadController.id())){
                        neighbourSwitches.add(link);
                    }
                }
                if(neighbourSwitches.size() == 0){
                    break;
                }
        //        log.info("有"+neighbourSwitches.size()+"个邻居交换机");
                long max = Long.MIN_VALUE;
                DeviceId mostRelatedSwitch = DeviceId.deviceId("");
                for(Link link:neighbourSwitches){
        //            log.info("开始！");
                    long relativity = calculateFlowRelativity(switchGroups,link);
        //            log.info("relativity:"+relativity);
                    if(relativity != -1 && relativity>max){
                        max=relativity;
                        mostRelatedSwitch = link.dst().deviceId();
        //                log.info("mostRelatedSwitch"+mostRelatedSwitch.toString());
                    }
         //           log.info("结束！");
                }
        //        log.info("hello");
                try{
                    candidateSwitches.get(deviceId).add(mostRelatedSwitch);
                }catch (Exception e){
                    e.printStackTrace();
                    log.info(e.getMessage());
                }

         //       log.info("switchGroups个数:"+candidateSwitches.get(deviceId).size());
          //      print(candidateSwitches.get(deviceId));
                if(mostRelatedSwitch.toString().equals("")){
                    break;
                }
                sum+=load_map.get(overLoadController).get(mostRelatedSwitch);
        //        log.info("sum:"+sum);

                Set<NodeId> targetControllers = map.get(deviceId);
                for(NodeId nodeId:targetControllers){
                    double d1 = Tools.getControllerLoadVarienceAfter(load_map,sum,overLoadController,nodeId);
                    double d2 = 0;
                    for(DeviceId dd:switchGroups){
                        d2+=delays.get(dd).get(Constants.CONTROLLER_SWITCH_MAPPING.get(nodeId.id()));
                    }
                    double[] d = {d1,d2};
                    list.add(new Alternative(switchGroups,overLoadController,nodeId,d));
                }



            }
      //      log.info("第"+count1+"个边缘交换机"+"Finished");
            count1++;
        }

        if(list.size() !=0 ){
            try {
                Topsis.setAl(list);
       //         log.info("setAl Completed!");
                Topsis.setWeight();
      //          log.info("setWeight Completed!");
                Topsis.standardData();
       //         log.info("standardData Completed!");
                Topsis.cal();
      //          log.info("calculate Completed!");
            } catch (Exception e){
                e.printStackTrace();
            }

        }

    //    log.info("New stragy completed!");
    }

    public long calculateFlowRelativity(List<DeviceId> migrateSwitches, Link candidate){
        /*
        PortStatistics portStatistics = deviceService.getStatisticsForPort(
                candidate.src().deviceId(),candidate.src().port());
         */
        long relativity = -1;
        try{
             Load load= portStatisticsService.load(candidate.dst(), PortStatisticsService.MetricType.PACKETS);
             relativity = load.rate();
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            return relativity;
        }
    }

    public void print(List<DeviceId> switchGroups){
        StringBuilder sb = new StringBuilder();
        sb.append("备选方案:");
        for(DeviceId deviceId:switchGroups){
            sb.append(deviceId.toString()+" ");
        }
        log.info(sb.toString());
    }








    @Override
    public void updateMetric(ControlMetric cm, int updateIntervalInMinutes,
                             Optional<DeviceId> deviceId) {
        if (deviceId.isPresent()) {

            // insert a new device entry if we cannot find any
            ctrlMsgBuf.putIfAbsent(deviceId.get(), Maps.newConcurrentMap());

            // update control message metrics
            if (CONTROL_MESSAGE_METRICS.contains(cm.metricType())) {

                if (!availableDeviceIdSet.contains(deviceId.get())) {
                    availableDeviceIdSet.add(deviceId.get());
                }

                // we will accumulate the metric value into buffer first
                ctrlMsgBuf.get(deviceId.get()).putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getRate());

                // if buffer contains all control message metrics,
                // we simply set and update the values into MetricsDatabase.
                if (ctrlMsgBuf.get(deviceId.get()).keySet()
                        .containsAll(CONTROL_MESSAGE_METRICS)) {
                    updateControlMessages(ctrlMsgBuf.get(deviceId.get()), deviceId.get());
                    ctrlMsgBuf.clear();
                }
            }
        } else {

            // update cpu metrics
            if (CPU_METRICS.contains(cm.metricType())) {
                cpuBuf.putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getCount());
                if (cpuBuf.keySet().containsAll(CPU_METRICS)) {
                    cpuMetrics.updateMetrics(convertMap(cpuBuf));
                    cpuBuf.clear();
                }
            }

            // update memory metrics
            if (MEMORY_METRICS.contains(cm.metricType())) {
                memoryBuf.putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getCount());
                if (memoryBuf.keySet().containsAll(MEMORY_METRICS)) {
                    memoryMetrics.updateMetrics(convertMap(memoryBuf));
                    memoryBuf.clear();
                }
            }
        }
    }

    @Override
    public void updateMetric(ControlMetric cm, int updateIntervalInMinutes,
                             String resourceName) {
        // update disk metrics
        if (DISK_METRICS.contains(cm.metricType())) {
            diskBuf.putIfAbsent(resourceName, Maps.newConcurrentMap());

            availableResourceMap.putIfAbsent(Type.DISK, Sets.newHashSet());
            availableResourceMap.computeIfPresent(Type.DISK, (k, v) -> {
                v.add(resourceName);
                return v;
            });

            diskBuf.get(resourceName).putIfAbsent(cm.metricType(),
                    (double) cm.metricValue().getLoad());
            if (diskBuf.get(resourceName).keySet().containsAll(DISK_METRICS)) {
                updateDiskMetrics(diskBuf.get(resourceName), resourceName);
                diskBuf.clear();
            }
        }

        // update network metrics
        if (NETWORK_METRICS.contains(cm.metricType())) {
            networkBuf.putIfAbsent(resourceName, Maps.newConcurrentMap());

            availableResourceMap.putIfAbsent(Type.NETWORK, Sets.newHashSet());
            availableResourceMap.computeIfPresent(Type.NETWORK, (k, v) -> {
                v.add(resourceName);
                return v;
            });

            networkBuf.get(resourceName).putIfAbsent(cm.metricType(),
                    (double) cm.metricValue().getLoad());
            if (networkBuf.get(resourceName).keySet().containsAll(NETWORK_METRICS)) {
                updateNetworkMetrics(networkBuf.get(resourceName), resourceName);
                networkBuf.clear();
            }
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          Optional<DeviceId> deviceId) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, deviceId)));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, deviceId),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          String resourceName) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, resourceName)));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, resourceName),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          int duration, TimeUnit unit,
                                                          Optional<DeviceId> deviceId) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, deviceId), duration, unit));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, duration, unit, deviceId),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          int duration, TimeUnit unit,
                                                          String resourceName) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, resourceName), duration, unit));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, duration, unit, resourceName),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<Set<String>> availableResources(NodeId nodeId,
                                                             Type resourceType) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            Set<String> resources = getLocalAvailableResources(resourceType);
            return CompletableFuture.completedFuture(resources);
        } else {
            return communicationService.sendAndReceive(createResourceRequest(resourceType),
                    CONTROL_RESOURCE, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    /**
     * Builds and returns metric database instance with given resource name,
     * resource type and metric type.
     *
     * @param resourceName resource name
     * @param resourceType resource type
     * @param metricTypes  metric type
     * @return metric database instance
     */
    private MetricsDatabase genMDbBuilder(String resourceName,
                                          Type resourceType,
                                          Set<ControlMetricType> metricTypes) {
        MetricsDatabase.Builder builder = new DefaultMetricsDatabase.Builder();
        builder.withMetricName(resourceType.toString());
        builder.withResourceName(resourceName);
        metricTypes.forEach(type -> builder.addMetricType(type.toString()));
        return builder.build();
    }

    /**
     * Updates network metrics with given metric map and resource name.
     *
     * @param metricMap    a metric map which is comprised of metric type and value
     * @param resourceName resource name
     */
    private void updateNetworkMetrics(Map<ControlMetricType, Double> metricMap,
                                      String resourceName) {
        if (!networkMetricsMap.containsKey(resourceName)) {
            networkMetricsMap.put(resourceName, genMDbBuilder(resourceName,
                    Type.NETWORK, NETWORK_METRICS));
        }
        networkMetricsMap.get(resourceName).updateMetrics(convertMap(metricMap));
    }

    /**
     * Updates disk metrics with given metric map and resource name.
     *
     * @param metricMap    a metric map which is comprised of metric type and value
     * @param resourceName resource name
     */
    private void updateDiskMetrics(Map<ControlMetricType, Double> metricMap,
                                   String resourceName) {
        if (!diskMetricsMap.containsKey(resourceName)) {
            diskMetricsMap.put(resourceName, genMDbBuilder(resourceName,
                    Type.DISK, DISK_METRICS));
        }
        diskMetricsMap.get(resourceName).updateMetrics(convertMap(metricMap));
    }

    /**
     * Updates control message metrics with given metric map and device identifier.
     *
     * @param metricMap a metric map which is comprised of metric type and value
     * @param deviceId  device identifier
     */
    private void updateControlMessages(Map<ControlMetricType, Double> metricMap,
                                       DeviceId deviceId) {
        if (!controlMessageMap.containsKey(deviceId)) {
            controlMessageMap.put(deviceId, genMDbBuilder(deviceId.toString(),
                    Type.CONTROL_MESSAGE, CONTROL_MESSAGE_METRICS));
        }
        controlMessageMap.get(deviceId).updateMetrics(convertMap(metricMap));
    }

    /**
     * Converts metric map into a new map which contains string formatted metric type as key.
     *
     * @param metricMap metric map in which ControlMetricType is key
     * @return a new map in which string formatted metric type is key
     */
    private Map<String, Double> convertMap(Map<ControlMetricType, Double> metricMap) {
        if (metricMap == null) {
            return ImmutableMap.of();
        }
        Map newMap = Maps.newConcurrentMap();
        metricMap.forEach((k, v) -> newMap.putIfAbsent(k.toString(), v));
        return newMap;
    }

    /**
     * Handles control metric request from remote node.
     *
     * @param request control metric request
     * @return completable future object of control load snapshot
     */
    public CompletableFuture<ControlLoadSnapshot>
        handleMetricsRequest(ControlMetricsRequest request) {

        checkArgument(request.getType() != null, METRIC_TYPE_NULL);

        ControlLoad load;
        if (request.getResourceName() != null && request.getUnit() != null) {
            load = getLocalLoad(request.getType(), request.getResourceName());
        } else {
            load = getLocalLoad(request.getType(), request.getDeviceId());
        }

        long average;
        if(load != null){
            if (request.getUnit() != null) {
                average = load.average(request.getDuration(), request.getUnit());
            } else {
                average = load.average();
            }
        }
        else{
            average=0;
        }

        ControlLoadSnapshot resp = load!=null?
                new ControlLoadSnapshot(load.latest(), average, load.time()):
                new ControlLoadSnapshot(0, average, 0);
        return CompletableFuture.completedFuture(resp);
    }

    /**
     * Handles control resource request from remote node.
     *
     * @param request control resource type
     * @return completable future object of control resource set
     */
    private CompletableFuture<Set<String>>
        handleResourceRequest(ControlResourceRequest request) {

        checkArgument(request.getType() != null, RESOURCE_TYPE_NULL);

        Set<String> resources = getLocalAvailableResources(request.getType());
        return CompletableFuture.completedFuture(resources);
    }

    /**
     * Generates a control metric request.
     *
     * @param type     control metric type
     * @param deviceId device identifier
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       Optional<DeviceId> deviceId) {
        return new ControlMetricsRequest(type, deviceId);
    }

    /**
     * Generates a control metric request with given projected time range.
     *
     * @param type     control metric type
     * @param duration projected time duration
     * @param unit     projected time unit
     * @param deviceId device identifier
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       int duration, TimeUnit unit,
                                                       Optional<DeviceId> deviceId) {
        return new ControlMetricsRequest(type, duration, unit, deviceId);
    }

    /**
     * Generates a control metric request.
     *
     * @param type         control metric type
     * @param resourceName resource name
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       String resourceName) {
        return new ControlMetricsRequest(type, resourceName);
    }

    /**
     * Generates a control metric request with given projected time range.
     *
     * @param type         control metric type
     * @param duration     projected time duration
     * @param unit         projected time unit
     * @param resourceName resource name
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       int duration, TimeUnit unit,
                                                       String resourceName) {
        return new ControlMetricsRequest(type, duration, unit, resourceName);
    }

    /**
     * Generates a control resource request with given resource type.
     *
     * @param type control resource type
     * @return control resource request instance
     */
    private ControlResourceRequest createResourceRequest(ControlResource.Type type) {
        return new ControlResourceRequest(type);
    }

    /**
     * Returns a snapshot of control load.
     *
     * @param cl control load
     * @return a snapshot of control load
     */
    private ControlLoadSnapshot snapshot(ControlLoad cl) {
        if (cl != null) {
            return new ControlLoadSnapshot(cl.latest(), cl.average(), cl.time());
        }
        return null;
    }

    /**
     * Returns a snapshot of control load with given projected time range.
     *
     * @param cl       control load
     * @param duration projected time duration
     * @param unit     projected time unit
     * @return a snapshot of control load
     */
    private ControlLoadSnapshot snapshot(ControlLoad cl, int duration, TimeUnit unit) {
        if (cl != null) {

            return new ControlLoadSnapshot(cl.latest(), cl.average(duration, unit),
                    cl.time(), cl.recent(duration, unit));
        }
        return null;
    }

    /**
     * Returns local control load.
     *
     * @param type     metric type
     * @param deviceId device identifier
     * @return control load
     */
    private ControlLoad getLocalLoad(ControlMetricType type,
                                     Optional<DeviceId> deviceId) {
        if (deviceId.isPresent()) {
            // returns control message stats
            if (CONTROL_MESSAGE_METRICS.contains(type) &&
                    availableDeviceIdSet.contains(deviceId.get())) {
                return new DefaultControlLoad(controlMessageMap.get(deviceId.get()), type);
            }
        } else {
            // returns controlLoad of CPU metrics
            if (CPU_METRICS.contains(type)) {
                return new DefaultControlLoad(cpuMetrics, type);
            }

            // returns memoryLoad of memory metrics
            if (MEMORY_METRICS.contains(type)) {
                return new DefaultControlLoad(memoryMetrics, type);
            }
        }
        return null;
    }

    /**
     * Returns local control load.
     *
     * @param type         metric type
     * @param resourceName resource name
     * @return control load
     */
    private ControlLoad getLocalLoad(ControlMetricType type, String resourceName) {
        NodeId localNodeId = clusterService.getLocalNode().id();

        // returns disk I/O stats
        if (DISK_METRICS.contains(type) &&
                availableResourcesSync(localNodeId, Type.DISK).contains(resourceName)) {
            return new DefaultControlLoad(diskMetricsMap.get(resourceName), type);
        }

        // returns network I/O stats
        if (NETWORK_METRICS.contains(type) &&
                availableResourcesSync(localNodeId, Type.NETWORK).contains(resourceName)) {
            return new DefaultControlLoad(networkMetricsMap.get(resourceName), type);
        }
        return null;
    }

    /**
     * Obtains the available resource list from local node.
     *
     * @param resourceType control resource type
     * @return a set of available control resource
     */
    private Set<String> getLocalAvailableResources(Type resourceType) {
        Set<String> resources = ImmutableSet.of();
        if (RESOURCE_TYPE_SET.contains(resourceType)) {
            if (Type.CONTROL_MESSAGE.equals(resourceType)) {
                resources = ImmutableSet.copyOf(availableDeviceIdSet.stream()
                        .map(DeviceId::toString).collect(Collectors.toSet()));
            } else {
                Set<String> res = availableResourceMap.get(resourceType);
                resources = res == null ? ImmutableSet.of() : res;
            }
        }
        return resources;
    }
}
