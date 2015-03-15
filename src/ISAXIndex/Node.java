/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ian
 */
public class Node implements Comparable<Node> {
    
    protected Node parent = null;
    protected ArrayList<Node> children = new ArrayList();
    protected ISAX load;
    private static final Logger logger = Logger.getLogger(Node.class.getName());
    
    Node(ISAX word) {
        load = word;
    }
    
    Node(ISAX word, ArrayList<Node> incoming) {
        this(word);
        add(incoming);
    }

    // for root node
    Node(int dimensionality) {
        this(new ISAX(dimensionality));
        
    }
    
    public boolean equals(ISAX o) {
        return load.equals(o);
    }
    
    public boolean isEmpty() {
        return numChildren() == 0;
    }
    
    public long integrityCheck(int depth) {
        long ssCount = 0;
        if (isRoot()) {
            assert parent == null;
        }
//        System.out.println("Node at depth " + depth + "\t" + dispLoad());
        for (int i = 0; i < load.dimension(); i++) {
            assert load.getWidth(i) == depth - 1;
        }
        Collections.sort(children);
        for (int i = 0; i < numChildren() - 1; i++) {
            Node first = children.get(i);
            Node second = children.get(i + 1);
            assert first.compareTo(second) < 0;
        }
        int leafCount = 0;
        int nodeCount = 0;
        for (Node n : children) {
            if (n.isLeaf()) {
                leafCount++;
            } else {
                nodeCount++;
            }
            assert n.parent == this;
            ssCount += n.integrityCheck(depth + 1);
        }
        assert leafCount <= (1 << load.dimension());
        assert nodeCount <= (1 << load.dimension());
        
        return ssCount;
    }
    
    public ISAX getLoad() {
        return new ISAX(load);
    }
    
    public int getLoad(int i) {
        return load.getLoad(i);
    }
    
    static void setLoggerLevel(Level level) {
        logger.setLevel(level);
    }

//    public boolean covers(ISAX o) {
//        return load.covers(o);
//    }
    public int compareTo(ISAX o) {
        return load.compareTo(o);
    }
    
    public boolean isLeaf() {
        return false;
    }
    
    public boolean isRoot() {
        return parent == null;
    }
    
    public int numChildren() {
        return children.size();
    }

//    public long recursiveSize() {
//        long result = 0;
//        if (isLeaf()) {
//            return size();
//        } else {
//            for (Node n : children) {
//                result += n.recursiveSize();
//            }
//            return result;
//        }
//    }
//    public int depth2Size() {
//        int sum = 0;
//        for (Node n : children) {
//            if (n.isLeaf()) {
//                sum++;
//            } else {
//                sum += n.size();
//            }
//        }
//        return sum;
//    }
    public void split() {
        assert !isEmpty();
        ArrayList<Node> tempChildList = new ArrayList();
        for (Node n : children) {
            if (n.isLeaf()) {
                tempChildList.add(n);
            }
        }
        if (tempChildList.size() > 0) {
            remove(tempChildList);
        }
        
        ArrayList<Node> tempParentList = new ArrayList();
        for (Node leaf : tempChildList) {
            boolean processed = false;
            for (Node tempParent : tempParentList) {
                if (tempParent.compareTo(leaf) == 0) {
                    tempParent.add(leaf);
                    processed = true;
                    break;
                }
            }
            if (!processed) {
                ISAX tempLoad = leaf.getLoad();
                tempLoad.setWidth(getWidth(0) + 1);
                Node tempParent = new Node(tempLoad);
                tempParentList.add(tempParent);
                tempParent.add(leaf);
            }
        }
        add(tempParentList);
//        Collections.sort(children);
        return;
    }
    
    public void setParent(Node n) {
        parent = n;
    }
    
    public void add(ArrayList<Node> nodeList) {
        for (Node n : nodeList) {
            n.setParent(this);
        }
        children.addAll(nodeList);
    }
    
    public void remove(ArrayList<Node> nodeList) {
        children.removeAll(nodeList);
    }
    
    public String dispLoad() {
        String l = "";
        for (int i = 0; i < load.dimension(); i++) {
            l = l + "\t" + load.getLoad(i) + "(" + load.getWidth(i) + ")";
        }
        return l;
    }
    
    public boolean needsSplit(int maxCap) {
        int count = 0;
        for (int i = 0; i < numChildren(); i++) {
            if (children.get(i).isLeaf()) {
                count++;
            }
        }
        return count > maxCap;
    }
    
    public ArrayList<Node> merge() {
        return children;
    }
    
    public boolean needsMerge(int minCap) {
        int count = 0;
        for (int i = 0; i < numChildren(); i++) {
            if (!children.get(i).isLeaf()) {
                count++;
            }
        }
        return count == numChildren() && count < minCap;
    }
    
    private int getWidth(int i) {
        return load.getWidth(i);
    }
    
    @Override
    public int compareTo(Node o) {
        return load.compareTo(o.load);
    }
    
    public void add(Node n) {
        children.add(n);
        n.setParent(this);
//        Collections.sort(children);
    }

//    public void updateLoad(int maxCard) {
//        Node first = children.get(0);
//        Node last = children.get(size() - 1);
//        load = first.load.commonPrefix(last.load);
//        int[] c = new int[load.size()];
//        for (int i = 0; i < c.length; i++) {
//            c[i] = maxCard;
//        }
//        load.setCard(c);
//    }
    public void add(long position) {
        throw new UnsupportedOperationException("not supported");
    }
    
    public void remove(long positions) {
        throw new UnsupportedOperationException("not supported");
    }
    
    public void remove(Node n) {
        children.remove(n);
    }
    
}
