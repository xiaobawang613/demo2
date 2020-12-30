package org.onosproject.cpman.impl;

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.*;

public class Topsis {
    public final static int D = 2;//指标数
    public final static double[] weight=new double[D];	//各指标权重
    public static int T = 0;
    private static Scanner s;
    public static List<Alternative> al =new LinkedList<>();
    public static double x[]=new double[D],y[]=new double[D];
    public static Alternative best;
    public static Alternative worse;

    public static void setAl(List<Alternative> al) {
        Topsis.al = al;
    }
    public static void setWeight(){
        List<List<Double>> result=new ArrayList<>();
        List<Double> temp1 = new ArrayList<>();
        List<Double> temp2 = new ArrayList<>();
        for(Alternative a:al){
            temp1.add(a.attribute[0]);
            temp2.add(a.attribute[1]);
        }
        result.add(temp1);
        result.add(temp2);
        List<Double> w = Entropy.getWeight(result);
        weight[0]=w.get(0)*100;
        weight[1]=w.get(1)*100;
    }
    /*
        public static void readData(){
            try(BufferedReader br=new BufferedReader( new FileReader("/Users/wangyao/毕业设计/代码/Demo1/data.txt"))){
                String tmp=null;
                while((tmp=br.readLine())!=null){
                    s = new Scanner(tmp);
                    double[] data=new double[D];
                    String num=s.next();
                    for(int i=0;i<D;i++){
                        data[i]=s.nextDouble();
                    }
                    al.add(new Alternative(num,data));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

     */
    public static void standardData(){

        for(int i=0;i<D;i++){
            x[i]=0;
            y[i]=0;
        }
        for(Alternative a:al){
            for(int i=0;i<D;i++){
                x[i]+=Math.pow(a.attribute[i],2);
            }
        }
        for(int i=0;i<D;i++){
            x[i]= Math.sqrt(x[i]);
        }
        for(Alternative a:al){
            for(int i=0;i<D;i++){
                a.attribute[i]=a.attribute[i]/x[i];
            }
            a.weighted();
        }
    }
    public static void cal(){
        for(int i=0;i<D;i++){
            for(Alternative a:al){
                a.comp=i;
            }
            x[i]= Collections.max(al).attribute[i];
            y[i]=Collections.min(al).attribute[i];
        }

        best = new Alternative(Arrays.asList(DeviceId.deviceId("")), NodeId.nodeId(""),NodeId.nodeId(""),y);
        worse = new Alternative(Arrays.asList(DeviceId.deviceId("")), NodeId.nodeId(""),NodeId.nodeId(""),x);
        ListIterator<Alternative> it = al.listIterator();
        while(it.hasNext()){
            Alternative t=it.next();
            t.bestdis=0;
            for(int j=0;j<D;j++){
                t.bestdis+=Math.pow(t.attribute[j]-best.attribute[j],2);
            }
            t.bestdis=Math.sqrt(t.bestdis);
        }
        it = al.listIterator();
        while(it.hasNext()){
            Alternative t=it.next();
            t.worsedis=0;
            for(int j=0;j<D;j++){
                t.worsedis+=Math.pow(t.attribute[j]-worse.attribute[j],2);
            }
            t.worsedis=Math.sqrt(t.worsedis);
            t.c=t.worsedis/(t.worsedis+t.bestdis);
        }
        //按照贴进度排序
        Collections.sort(al,new Comparator<Alternative>(){

            @Override
            public int compare(Alternative a1, Alternative a2){
                return a2.c>a1.c?1:-1;
            }
        });
    }

    public static List<Alternative> getAl() {
        return al;
    }

}
