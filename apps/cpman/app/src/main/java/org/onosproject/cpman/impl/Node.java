package org.onosproject.cpman.impl;

import java.util.ArrayList;
import java.util.List;

public class Node {
    Node(String name)
    {
        this.name=name;
        Childen=new ArrayList<Node>();
    }
    boolean visited;
    int num;
    int low;
    String name;
    Node parent;
    List<Node> Childen;
    public boolean equals(Node node)
    {
        if(this.name==node.name) {
            return true;
        }
        return false;
    }
}
