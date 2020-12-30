package org.onosproject.mcb;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.util.GeoLocation;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.*;
import org.onosproject.cpman.impl.ControlPlaneMonitor;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.*;
import org.onosproject.nodemetrics.NodeCpuUsage;
import org.onosproject.nodemetrics.NodeMetricsService;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(immediate = true)
public class MultiControllersBalancing {

    private final Logger log = getLogger(getClass());
    private int initialDelay = 15;
    private int schedulePeriod = 60;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("MultiControllersBalancing", "%d", log));
    private AtomicReference<Future> nextTask = new AtomicReference<>();
    private Map<NodeId, NodeCpuUsage> cpuStore = Maps.newConcurrentMap();
    private Set<String> cutNodes = new HashSet<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ControlPlaneMonitorService controlPlaneMonitorService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterCommunicationService communicationService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NodeMetricsService nodeMetricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    private ControlPlaneMonitor controlPlaneMonitor;

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

    private static final MessageSubject CONTROL_STATS =
            new MessageSubject("control-plane-stats");

    @Activate
    public void activate(){
        log.info("MultiControllersBalancing started!");
        communicationService.<ControlMetricsRequest, ControlLoadSnapshot>addSubscriber(CONTROL_STATS,
                SERIALIZER::decode, controlPlaneMonitor::handleMetricsRequest, SERIALIZER::encode);
         monitorLoad();
        //List<Node> cugetCutPoint(topologyService.getGraph(topologyService.currentTopology()));

    }

    @Deactivate
    public void deactivate(){
        executorService.shutdown();
        communicationService.removeSubscriber(CONTROL_STATS);
        log.info("MultiControllersBalancing ended!");
    }

    public void monitorLoad(){
        Future task = executorService.scheduleAtFixedRate(new LoadTask(),
                initialDelay, schedulePeriod, TimeUnit.SECONDS);
        if (!nextTask.compareAndSet(null, task)) {
            task.cancel(false);
        }
    }

    public class LoadTask implements Runnable{
        @Override
        public void run() {
            nextTask.set(null);
            /*
            cpuStore = nodeMetricsService.cpu();
            for(NodeId id:cpuStore.keySet()){

                if(cpuStore.get(id).usage()>3){
                    log.info("控制器"+id.id()+"过载啦！");
                   // makeDecision(collectLoad(), id);
                    //getCutPoint(topologyService.getGraph(topologyService.currentTopology()));
                }

            }
             */
            Map<NodeId,Map<DeviceId,Long>> loads = null;
            loads = collectLoad();
            for(NodeId nodeId:loads.keySet()){
                Map<DeviceId,Long> temp = loads.get(nodeId);
                long sum = 0;
                for(DeviceId deviceId:temp.keySet()){
                    sum+=temp.get(deviceId);
                }
                log.info("控制器"+nodeId.id()+"的负载为"+sum);
            }
            log.info("Completed!");

        }
    }
    public Map<NodeId,Map<DeviceId,Long>> collectLoad() {
        List<ControllerNode> nodes = Lists.newArrayList(clusterService.getNodes());
        log.info(nodes.size()+"");
        Map<NodeId,Map<DeviceId,Long>> load_map = new HashMap<>();
        for(ControllerNode node:nodes){
            List<DeviceId> devices= Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
            log.info(devices.size()+"");
            Map<DeviceId,Long>  temp = new HashMap<>();
            for(DeviceId id:devices){
                ControlLoadSnapshot controlLoadSnapshot = null;
                controlLoadSnapshot = controlPlaneMonitorService.getLoadSync(clusterService.getLocalNode().id(),
                                ControlMetricType.INBOUND_PACKET, Optional.of(id));
                if(controlLoadSnapshot != null){
                    log.info("控制器"+node.id().id()+"下有交换机" +id+",其load为"+controlLoadSnapshot.latest());
                    temp.put(id,controlLoadSnapshot.latest());
                }
                else{
                    log.info("我好菜呀，获取不到交换机"+id.toString()+"的负载呢");
                }
            }
            load_map.put(node.id(),temp);
        }
        return load_map;
    }
    public void makeDecision(Map<NodeId,Map<DeviceId,Long>> load_map, NodeId overLoadId){
        Map<NodeId,Map<DeviceId,Double>> c_s_delay = getCSDelay(load_map);
        Topology topology = topologyService.currentTopology();
        Map<DeviceId, Map<DeviceId,Double>> delays = getDelays();
        List<Alternative> candidates = new LinkedList<>();
        makeDecisionByTopsis(candidates, overLoadId, delays, load_map);
        Alternative bestchoice = Topsis.getAl().get(0);
        log.info("最佳策略:迁移交换机为"+bestchoice.mig_switch+",原控制器为:"+bestchoice.sou_controller+",目标控制器为:"+
        bestchoice.tar_controller+",负载方差为:"+bestchoice.attribute[0]+"延迟为:"+bestchoice.attribute[1]);

    }

    /**
     * 获得控制器与其直连交换机的delay
     * @param load_map
     * @return
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
                temp.put(deviceId,Tools.getDelay(new GeoLocation(lan1, lon1), new GeoLocation(lan2,lon2)));
                // log.info(temp.get(deviceId)+"");
            }
            c_s_delay.put(node, temp);
        }
        return c_s_delay;
    }

    /**
     * 获得各交换机节点之间的delay
     * @return
     */
    public Map<DeviceId, Map<DeviceId,Double>> getDelays(){
        Map<DeviceId, Map<DeviceId,Double>> delays = new HashMap<>();
        List<Device> devices = Lists.newArrayList(deviceService.getAvailableDevices());
        for(Device d1:devices){
            Map<DeviceId,Double> temp = new HashMap<>();
            /*
            double lan1 = Constants.GEO_MAP.get(d1.id().toString().trim()).get(0);
            double lon1 = Constants.GEO_MAP.get(d1.id().toString().trim()).get(1);

             */
            for(Device d2:devices){
                /*
                double lan2 = Constants.GEO_MAP.get(d2.id().toString().trim()).get(0);
                double lon2 = Constants.GEO_MAP.get(d2.id().toString().trim()).get(1);
                temp.put(d2.id(),Tools.getDelay(new GeoLocation(lan1, lon1), new GeoLocation(lan2,lon2)));
                 */
               // log.info("距离为"+temp.get(d2.id()));
                double delay = 0;
                if(!d1.id().toString().equals(d2.id().toString())){
                    Set<Path> path =  topologyService.getKShortestPaths(topologyService.currentTopology(), d1.id(), d2.id(),
                            new GeoDistanceLinkWeight(deviceService),1);
                    for(Path p:path){
                        delay=p.cost()*1000/200000;
                        log.info("距离为"+delay);
                    }
                }
               temp.put(d2.id(),delay);

            }
            delays.put(d1.id(),temp);
        }
        return delays;
    }

    /**
     * 根据Topsis模型构建候选策略
     * @param list 候选策略list
     * @param index 过载控制器的NodeId
     * @param delays 各交换机节点之间的时延
     */
    public void makeDecisionByTopsis(List<Alternative> list, NodeId index, Map<DeviceId,
            Map<DeviceId,Double>> delays, Map<NodeId,Map<DeviceId,Long>> load_map){
        Set<DeviceId> deviceIds = mastershipService.getDevicesOf(index);
        Set<ControllerNode> controllerNodes = clusterService.getNodes();
        /*
        log.info(controllerNodes.size()+"");
        log.info(delays.size()+"");
        log.info(load_map.size()+"");
        for(NodeId nodeId:load_map.keySet()){
            Map<DeviceId,Long> temp = load_map.get(nodeId);
            for(DeviceId deviceId:temp.keySet()){
                log.info(temp.get(deviceId)+" ");
            }
        }
         */
        double varienceBefore = Tools.getControllerLoadVarienceBefore(load_map);
        log.info("迁移前负载方差为"+varienceBefore);
        cpuStore=nodeMetricsService.cpu();
        for(DeviceId deviceId:deviceIds){
            for(ControllerNode controllerNode:controllerNodes){
                if(!controllerNode.id().id().equals(index.id())){
                    if(cpuStore.get(index).usage()>cpuStore.get(controllerNode.id()).usage()){
                        double varienceAfter = Tools.getControllerLoadVarienceAfter(load_map,(double)load_map.get(index).get(deviceId),
                                index, controllerNode.id());
                        double[] d ={varienceAfter,delays.get(Constants.CONTROLLER_SWITCH_MAPPING.get(controllerNode.id().id())
                        ).get(deviceId)};
                        list.add(new Alternative(deviceId.toString(), index.id(), controllerNode.id().id(),d ));
                    }
                }
                else{
                    log.info("重复啦！");
                }
            }
        }
        Topsis.setAl(list);
        Topsis.setWeight();
        Topsis.standardData();
        Topsis.cal();
    }

    public List<Node> getCutPoint(TopologyGraph topologyGraph){
        Map<String,Node> nodes = new HashMap<>();
        Set<TopologyVertex> vertices = topologyGraph.getVertexes();
        Set<TopologyEdge> edges = topologyGraph.getEdges();
        for(TopologyVertex vertex:vertices){
            nodes.put(vertex.deviceId().toString(), new Node(vertex.deviceId().toString()));
        }
        for(TopologyEdge topologyEdge:edges){
            Node src = nodes.get(topologyEdge.src().deviceId().toString());
            Node dst = nodes.get(topologyEdge.dst().deviceId().toString());
            src.Childen.add(dst);
            dst.Childen.add(src);
        }
        FindCut.find(nodes.get("of:0000000000000001"));
        List<Node> points = FindCut.getPoints();
        for(Node node:points){
            log.info("割点是"+node.name);
        }
        return points;
    }


}
