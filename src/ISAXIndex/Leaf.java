/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

import java.util.ArrayList;

/**
 *
 * @author ian
 */
public class Leaf extends Node {

    ArrayList<Long> children = new ArrayList();

    Leaf(ISAX word, ArrayList<Long> incoming) {
        this(word);
        children = incoming;
    }

    Leaf(ISAX word) {
        super(word);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public int numChildren() {
        return children.size();
    }

    @Override
    public void add(long position) {
        children.add(position);
    }
    
//    public void add(long position) {
//        add(position, 0);
//    }
    @Override
    public void remove(long position) {
        for (int i = 0; i < numChildren(); i++) {
            if (children.get(i).equals(position)) {
                children.remove(i);
                break;
            }
        }
    }

    public long get(int i) {
        assert i > 0 && i < numChildren();
        return children.get(i);
    }

//    public void remove(long position) {
//        remove(position, 0);
//    }
    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public void add(Node n) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void remove(Node n) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public void split() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean needsSplit(int maxCard) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public ArrayList<Node> merge() {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public boolean needsMerge(int minCap) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    public long integrityCheck(int depth) {
        assert numChildren() > 0;
        long ssCount = numChildren();
//        System.out.println("Leaf at depth " + depth + "\t" + dispLoad());
//        System.out.println(dispLoad());
        return ssCount;
    }

//    @Override
//    public int depth2Size() {
//        throw new UnsupportedOperationException("not supported");
//    }
//    @Override
//    public void updateLoad(int maxCard) {
//        throw new UnsupportedOperationException("not supported");
//    }
}
