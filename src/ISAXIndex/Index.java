/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ian
 */
public class Index implements Iterable<Long> {

    private Node root;
    private int dimension;
    private int minCap;
    private int maxCap;
    private int maxWidth;

    private static final Logger logger = Logger.getLogger(Index.class.getName());

    public Index(int maxCardinality, int dimensionality) {
        if (dimensionality < 4 || dimensionality > 16) {
            throw new UnsupportedOperationException("not supported");
        }
        root = new Node(dimensionality);
        dimension = dimensionality;
        minCap = (1 << (dimension - 4));
        maxCap = 1 << (dimension + 0);
        maxWidth = (int) Math.ceil(Math.log(maxCardinality - 1) / Math.log(2));
    }

    public static void setLoggerLevel(Level level) {
        logger.setLevel(level);
        Node.setLoggerLevel(level);
    }

    private Stack<Node> findPath(ISAX isax) {
        Stack<Node> path = new Stack();
        path.push(root);

        while (!path.peek().equals(isax)) {
            if (path.peek().isLeaf() && path.peek().isEmpty()) {
                break;
            } else {
                Node parent = path.peek();
                for (Node n : parent.children) {
                    if (n.compareTo(isax) == 0) {
                        path.push(n);
                        break;
                    }
                }
                if (path.peek() == parent) {
                    break;
                }
            }
        }
        return path;
    }

    public void add(double[] vals, long position) {
        logger.finer("subsequence at position " + position);

        ISAX in = new ISAX(vals, dimension, 1 << (maxWidth));

        // find the path to the corresponding leaf node
        Stack<Node> path = findPath(in);
        if (!path.peek().equals(in)) {
            Leaf leaf = new Leaf(in);
            path.peek().add(leaf);
            path.push(leaf);
//            System.out.println(leaf.dispLoad());
        }

        // add the subsequence
        path.pop().add(position);

        // check if merge is necessary
        while (!path.isEmpty()) {
            Node n = path.pop();
            if (n.needsSplit(maxCap)) {
                n.split();
            }
        }
    }

    public boolean remove(double[] vals, long position) {
        logger.finer("subsequence at position " + position);

        ISAX out = new ISAX(vals, dimension, 1 << (maxWidth));

        // find the path to the corresponding leaf node
        Stack<Node> path = findPath(out);
        if (!path.peek().equals(out)) {
            logger.fine("subsequence not found: position " + position);
            return false;
        }

        // remove the subsequence
        path.pop().remove(position);

        // check if merge is necessary
        while (!path.isEmpty()) {
            Node child = path.pop();
            if (child.needsMerge(minCap)) {
                if (child.isRoot()) {
                    break;
                } else {
                    Node parent = child.parent;
                    parent.remove(child);
                    parent.add(child.merge());
                }
            }
        }
        return true;
    }

    public void integrityCheck() {
        long totalSS = root.integrityCheck(1);
//        System.out.println("Total number of subsequence: " + totalSS);
    }

    @Override
    public Iterator<Long> iterator() {
        return new BreadthFirstSearch(root);
    }

    public ArrayList<Long> knn(double[] vals, int k) {
        return knn(vals, k, (ArrayList<Long>) null);
    }

    // approximated search of k nearest neighbor
    public ArrayList<Long> knn(double[] vals, int k, ArrayList<Long> exception) {

        class KNNLeaf {

            class LeafComparator implements Comparator<Leaf> {

                private ISAX p = null;

                LeafComparator(ISAX o) {
                    p = o;
                }

                @Override
                public int compare(Leaf t1, Leaf t2) {
                    double minDistT1 = p.minDist(t1.load);
                    double minDistT2 = p.minDist(t2.load);
                    if (p.equals(t1.load) && !p.equals(t2.load)) {
                        return -1;
                    } else if (!p.equals(t1.load) && p.equals(t2.load)) {
                        return 1;
                    } else {
                        return (int) (minDistT1 - minDistT2);
                    }
                }
            }

            private ArrayList<Leaf> list = new ArrayList();
            private int k = 0;
            private ArrayList<Long> exception = null;
            private ISAX p = null;
            private LeafComparator comp = null;

            KNNLeaf(int _k, ISAX _p, ArrayList<Long> _exception) {
                k = _k;
                exception = _exception;
                p = _p;
                comp = new LeafComparator(p);
            }

            public boolean contains(Leaf o) {
                for (Leaf leaf : list) {
                    if (o.load.equals(leaf.load)) {
                        return true;
                    }
                }
                return false;
            }

            public boolean add(Leaf leaf) {
                if (contains(leaf)) {
                    return false;
                }
                double dist = p.minDist(leaf.load);
                if (dist <= kDist()) {
                    Leaf leafCopy = new Leaf(leaf);
                    if (exception != null) {
                        leafCopy.children.removeAll(exception);
                        if (leafCopy.isEmpty()) {
                            return false;
                        }
                    }
                    list.add(leafCopy);
                    Collections.sort(list, comp);

                    if (numID() > k) {
                        while (numID() - list.get(list.size() - 1).numChildren() > k) {
                            list.remove(list.size() - 1);
                        }
                    }
                    return true;
                }
                return false;
            }

            public double kDist() {
                if (numID() < k) {
                    return Double.MAX_VALUE;
                } else {
                    Leaf lastLeaf = list.get(list.size() - 1);
                    double bsfDist = lastLeaf.load.minDist(p);
                    return bsfDist;
                }
            }

            public int numID() {
                int count = 0;
                for (Leaf leaf : list) {
                    count += leaf.numChildren();
                }
                return count;
            }

            public ArrayList<Long> toArrayListLong() {
                ArrayList<Long> results = new ArrayList();
                for (Leaf leaf : list) {
                    results.addAll(leaf.children);
                }
                return results;
            }

        }

        assert k > 0 : "Invalid input parameter k. k must be integer greater than zero.";

        ISAX q = new ISAX(vals, dimension, 1 << maxWidth);
        ArrayList<Node> candidates = new ArrayList();
        KNNLeaf knn = new KNNLeaf(k, q, exception);
        candidates.add(root);
        while (!candidates.isEmpty()) {
            Node n = candidates.get(0);
            candidates.remove(0);
            if (n.isLeaf()) {
                knn.add((Leaf) n);
            } else {
                double dist = n.load.minDist(q);
                if (dist <= knn.kDist()) {
                    candidates.addAll(n.children);
                }
            }
        }

        return knn.toArrayListLong();

    }

    public ArrayList<Long> knn(double[] vals, int k, DataHandler dh) {
        return knn(vals, k, dh, null);
    }

    // exception aware approximated nearest neighbor
    public ArrayList<Long> knn(double[] vals, int k, DataHandler dh, ArrayList<Long> exception) {

        class KNNID {

            class IDDist implements Comparable<IDDist> {

                private long id = -1;
                private double dist = Double.MAX_VALUE;

                IDDist(long _id, double _dist) {
                    id = _id;
                    dist = _dist;
                }

                @Override
                public int compareTo(IDDist t) {
                    if (dist > t.dist) {
                        return 1;
                    } else if (dist < t.dist) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

                public boolean equals(IDDist o) {
                    return o.id == id;
                }

                public boolean equals(Long _id) {
                    return _id == id;
                }
            }

            private ArrayList<IDDist> list = new ArrayList();
            private int k = 0;
            private ArrayList<Long> exception = null;

            KNNID(int _k, ArrayList<Long> _exception) {
                k = _k;
                exception = _exception;
            }

            public boolean add(Long id, double dist) {

                if (contains(id)) {
                    return false;
                }
                if (exception != null) {
                    if (exception.contains(id)) {
                        return false;
                    }
                }
                IDDist o = new IDDist(id, dist);
                list.add(o);
                Collections.sort(list);
//                while (list.size() > k) {
//                    if (list.get(list.size() - 1).dist > kDist()) {
//                        list.remove(list.size() - 1);
//                    } else {
//                        break;
//                    }
//                }
                return true;
            }

            public boolean contains(Long id) {
                for (IDDist iddist : list) {
                    if (iddist.equals(id)) {
                        return true;
                    }
                }
                return false;
            }

            public double kDist() {
                if (numID() < k) {
                    return Double.MAX_VALUE;
                } else {
                    return list.get(k - 1).dist;
                }
            }

            public int numID() {
                return list.size();
            }

            public ArrayList<Long> toArrayListLong() {
                ArrayList<Long> results = new ArrayList();

                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).dist <= kDist()) {
                        results.add(list.get(i).id);
                    } else {
                        break;
                    }
                }
                return results;
            }
        }

        ArrayList<Long> results = knn(vals, k, exception);
        KNNID knn = new KNNID(k, exception);
        for (Long id : results) {
            double dist = ED.distance(vals, dh.get(id));
            knn.add(id, dist);
        }

        ISAX q = new ISAX(vals, dimension, 1 << maxWidth);
        ArrayList<Node> candidates = new ArrayList();
        candidates.add(root);
        while (!candidates.isEmpty()) {
            Node n = candidates.get(0);
            candidates.remove(0);
            if (n.isLeaf()) {
                for (Long id : ((Leaf) n).children) {
                    if (knn.contains(id)) {
                        continue;
                    }
                    double dist = ED.distance(vals, dh.get(id));
                    if (dist <= knn.kDist()) {
                        knn.add(id, dist);
                    }
                }
            } else {
                double dist = n.load.minDist(q);
                if (dist <= knn.kDist()) {
                    candidates.addAll(n.children);
                }
            }

        }
        return knn.toArrayListLong();
    }

    class BreadthFirstSearch implements Iterator<Long> {

        // Implement BFS with Iterative deepening depth-first search (IDDFS) method
        private final Stack<Node> path;
        private int depth = 1;
        private Leaf next = null;
        private boolean completeSearch = false;
        private int pointer = 0;
        private Node start = null;

        private BreadthFirstSearch(Node n) {
            start = n;
            this.path = new Stack();
            assert !start.isLeaf();
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (start.isEmpty() || completeSearch) {
                return false;
            }

            while (depth < maxWidth + 2) {
                if (path.isEmpty()) {
                    path.push(start);
                    goDown(depth);
                    if (path.peek().isLeaf() && path.size() == depth) {
                        next = (Leaf) path.peek();
                        logger.finer("found leaf " + next.dispLoad() + " at depth " + path.size());
                        return true;

                    }
                }
                while (true) {

                    goUp();

                    if (path.isEmpty()) {
                        depth++;
                        break;
                    }

                    // go down
                    goDown(depth);

                    if (path.peek().isLeaf() && path.size() == depth) {
                        next = (Leaf) path.peek();
                        logger.finer("found leaf " + next.dispLoad() + " at depth " + path.size());
                        return true;

                    }
                }
            }
            completeSearch = true;
            return false;
        }

        @Override
        public Long next() {
            hasNext();
            if (next == null) {
                return null;
            } else {
                long result = next.get(pointer++);
                if (pointer >= next.numChildren()) {
                    next = null;
                    pointer = 0;
                }
                return result;
            }
        }

        private void goDown(int curDepth) {
            while (path.size() < curDepth && !path.peek().isLeaf()) {
                Node parent = path.peek();
                if (!parent.isEmpty()) {
                    Node child = parent.children.get(0);
                    path.push(child);
                } else {
                    return;
                }
            }
        }

        private void goUp() {
            Node rightNeighbor;
            do {
                Node n = path.pop();
                rightNeighbor = rightNeighbor(n);
            } while (rightNeighbor == null && !path.isEmpty());
            if (rightNeighbor != null) {
                path.push(rightNeighbor);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private Node rightNeighbor(Node n) {
            if (n.isRoot()) {
                return null;
            }
            Node parent = n.parent;
            int idx = parent.children.indexOf(n);
            if (parent.numChildren() > idx + 1) {
                return parent.children.get(idx + 1);
            }
            return null;
        }

    }
}
