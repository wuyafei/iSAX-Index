/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

import java.util.ArrayList;
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

    public void add(double[] vals, long position, double mean, double std) {
        logger.finer("subsequence at position " + position);

        ISAX in = new ISAX(vals, dimension, 1 << (maxWidth), mean, std);

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

    public boolean remove(double[] vals, long position, double mean, double std) {
        logger.finer("subsequence at position " + position);

        ISAX out = new ISAX(vals, dimension, 1 << (maxWidth), mean, std);

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

    // approximated search for nearest neighbor
    public ArrayList<Long> NN(double[] vals, double mean, double std) {
        ISAX q = new ISAX(vals, dimension, 1 << maxWidth, mean, std);
        Stack<Node> path = findPath(q);
        if (path.peek().equals(q)) {
            Leaf leaf = (Leaf) path.peek();
            return new ArrayList(leaf.children);
        } else {
            ArrayList<Long> result = new ArrayList();
            Iterator<Long> iter = new BreadthFirstSearch(path.peek());
            while (iter.hasNext()) {
                result.add(iter.next());
            }
            return result;
        }
    }

    public ArrayList<Long> NN(double[] vals, double mean, double std, DataHandler dh) {
        
        // use approxmiated search first to speed up exact search
        double bsfDist = Double.MAX_VALUE;
        ArrayList<Long> bsfID = new ArrayList();
        ArrayList<Long> approx = NN(vals, mean, std);
        for (long id : approx) {
            double dist = ED.distance(vals, dh.get(id));
            if (bsfDist > dist) {
                bsfDist = dist;
                bsfID.clear();
                bsfID.add(id);
            }
        }
        assert !bsfID.isEmpty();
        ISAX q = new ISAX(vals, dimension, 1 << maxWidth, mean, std);
        PriorityQueue<Node> pq = new PriorityQueue(1, new ComparatorNode(q));
        pq.add(root);
        while (!pq.isEmpty()) {
            Node p = pq.poll();
            if (p.isLeaf()) {
                ArrayList<Long> candidates = ((Leaf) p).children;
                candidates.removeAll(approx);
                for (long id : candidates) {
                    double dist = ED.distance(vals, dh.get(id));
                    if (bsfDist > dist) {
                        bsfDist = dist;
                        bsfID.clear();
                        bsfID.add(id);
                    } else if (bsfDist == dist) {
                        bsfID.add(id);
                    }
                }
            } else {
                for (Node n : p.children) {
                    // shieh2008isax use MINDIST_PAA_iSAX to obain a tigher lower bound
                    // For simplicity, we reuse MINDIST_iSAX_iSAX
                    if (q.minDist(n.load) <= bsfDist) {
                        pq.add(n);
                    }
                }
            }
        }
        return bsfID;
    }

    class ComparatorNode implements Comparator<Node> {

        ISAX load = null;

        ComparatorNode(ISAX o) {
            load = o;
        }

        @Override
        public int compare(Node n, Node n1) {
            double minDistT = load.minDist(n.load);
            double minDistT1 = load.minDist(n1.load);;
            return (int) (minDistT - minDistT1);
        }
    }

    class BreadthFirstSearch implements Iterator<Long> {

        // Implement BFS with Iterative deepening depth-first search (IDDFS) method
        private final Stack<Node> path;
        private int depth = 1;
        private Leaf next = null;
        private boolean completeSearch = false;
        private int pointer = -1;
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
