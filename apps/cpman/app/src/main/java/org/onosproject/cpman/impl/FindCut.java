package org.onosproject.cpman.impl;

import java.util.ArrayList;
import java.util.List;

public class FindCut {

    private static int count=1;
    private static List<Node> points=new ArrayList<Node>();
    private static List<Node> mynode=new ArrayList<Node>();
    public static void find(Node node)
    {
        List<Node> childs=node.Childen;
        node.num=count++;
        node.low=node.num;
        node.visited=true;
        for(Node n:childs)
        {
            if(!n.visited)
            {
                n.parent=node;
                //前向遍历，给每个节点编号
                find(n);
                //判断是否是割点
                if(n.low>=node.num&&node.num!=1)
                {
                    points.add(node);
                }
                //后向遍历，计算low
                node.low=Math.min(node.low,n.low);
            }
            else
            {
                //背向边中num
                if(node.parent!=null&&!node.parent.equals(n))
                    node.low=Math.min(node.low,n.num);
            }
        }
    }

    public static void print(Node node)
    {
        mynode.add(node);
        List<Node> childs=node.Childen;
        System.out.println("name"+node.name+"   num:"+node.num+"  low:"+node.low);
        for(Node n:childs)
        {
            if(!mynode.contains(n))
            {
                print(n);
            }
        }
    }

    public static List<Node> getPoints() {
        return points;
    }

    public static void setPoints(List<Node> points) {
        FindCut.points = points;
    }
}
