/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ian
 */
public class Index implements Iterable<TimeSeries> {

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

    public void add(TimeSeries ts, double mean, double std) {
        logger.finer("subsequence at position " + ts.getPosition() + ", timeseries " + ts.getID());

        ISAX in = new ISAX(ts.getSeries(), dimension, 1 << (maxWidth), mean, std);

        // find the path to the corresponding leaf node
        Stack<Node> path = findPath(in);
        if (!path.peek().equals(in)) {
            Leaf leaf = new Leaf(in);
            path.peek().add(leaf);
            path.push(leaf);
//            System.out.println(leaf.dispLoad());
        }

        // add the subsequence
        path.pop().add(ts.getPosition(), ts.getID());

        // check if merge is necessary
        while (!path.isEmpty()) {
            Node n = path.pop();
            if (n.needsSplit(maxCap)) {
                n.split();
            }
        }
    }

    public boolean remove(TimeSeries ts, double mean, double std) {
        logger.finer("subsequence at position " + ts.getPosition() + ", timeseries " + ts.getID());

        ISAX out = new ISAX(ts.getSeries(), dimension, 1 << (maxWidth), mean, std);

        // find the path to the corresponding leaf node
        Stack<Node> path = findPath(out);
        if (!path.peek().equals(out)) {
            logger.fine("subsequence not found: position " + ts.getPosition() + ", timeseries " + ts.getID());
            return false;
        }

        // remove the subsequence
        path.pop().remove(ts.getPosition(), ts.getID());

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
    public Iterator<TimeSeries> iterator() {
        return new BreadthFirstSearch();

    }

    class BreadthFirstSearch implements Iterator<TimeSeries> {

        // Implement BFS with Iterative deepening depth-first search (IDDFS) method
        private final Stack<Node> path;
        private int depth = 1;
        private Leaf next = null;
        private boolean completeSearch = false;
        private int pointer = -1;

        private BreadthFirstSearch() {
            this.path = new Stack();
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (root.isEmpty() || completeSearch) {
                return false;
            }

            while (depth < maxWidth + 2) {
                if (path.isEmpty()) {
                    path.push(root);
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
        public TimeSeries next() {
            hasNext();
            if (next == null) {
                return null;
            } else {
                TimeSeries result = next.get(pointer++);
                if (pointer >= next.size()) {
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
            if (parent.size() > idx + 1) {
                return parent.children.get(idx + 1);
            }
            return null;
        }

    }
}
