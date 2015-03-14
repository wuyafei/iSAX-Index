/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ISAXIndex;

/**
 *
 * @author ian
 */
public class ISAX implements Comparable<ISAX> {

    private Symbol[] load;
    private int windowSize;

    public ISAX(double[] vals, int dimensionality, int cardinality, double mean, double sd) {
        int[] temp = getISAXVals(vals, dimensionality, NormalAlphabet.getCuts(cardinality), mean, sd);
        load = new Symbol[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            load[i] = new Symbol(temp[i], cardinality);
        }
        windowSize = vals.length;
    }

//    public ISAX(double[] vals, int dimensionality, int cardinality) {
//        int[] temp = getISAXVals(vals, dimensionality, NormalAlphabet.getCuts(cardinality));
//        load = new ArrayList();
//        for (int i = 0; i < vals.length; i++) {
//            load.add(new Symbol(temp[i], cardinality));
//        }
//        windowSize = vals.length;
//    }
    ISAX(ISAX o) {
        this(o.load);
    }

    ISAX(Symbol[] workload) {
        load = new Symbol[workload.length];
        for (int i = 0; i < workload.length; i++) {
            load[i] = new Symbol(workload[i]);
        }
        windowSize = 0;
    }

    ISAX(int dimensionality) {
        load = new Symbol[dimensionality];
        for (int i = 0; i < dimensionality; i++) {
            load[i] = new Symbol(0, 2);
        }
    }

    public boolean covers(ISAX o) {
        assert size() == o.size();
        for (int i = 0; i < size(); i++) {
            if (getWidth(i) > o.getWidth(i)) {
                return false;
            }
        }
        return compareTo(o) == 0;
    }

    public int getLoad(int i) {
        assert i >= 0 && i < load.length;
        return load[i].load;
    }

    public int getWidth(int i) {
        assert i >= 0 && i < load.length;
        return load[i].width;
    }

//    private void setLoad(int i, int workload) {
//        assert i >= 0 && i < load.length;
//        load[i].load = workload;
//    }
//
//    private void setCard(int i, int cardinality) {
//        assert i >= 0 && i < load.length;
//        load[i].card = cardinality;
//    }
    public void setWidth(int maxWidth) {
        for (int i = 0; i < load.length; i++) {
            if (load[i].width > maxWidth) {
                load[i].load = load[i].load >> (load[i].width - maxWidth);

            } else if (load[i].width < maxWidth) {
                load[i].load = load[i].load << (maxWidth - load[i].width);
            }
            load[i].width = maxWidth;
        }
    }

    /**
     * Convert real-valued series into symbolic representation.
     *
     * @param vals Real valued timeseries.
     * @param windowSize The PAA window size.
     * @param cuts The cut values array used for SAX transform.
     * @return The symbolic representation of the given real time-series.
     * @throws TSException If error occurs.
     */
//    private static int[] getISAXVals(double[] vals, int dimensionality, double[] cuts) {
//        int[] l;
//        if (vals.length == cuts.length + 1) {
//            l = ts2isax(TSUtils.zNormalize(vals), cuts);
//        } else {
//            l = ts2isax(TSUtils.zNormalize(TSUtils.paa(vals, dimensionality)), cuts);
//        }
//        return l;
//    }
    /**
     * Convert real-valued series into symbolic representation.
     *
     * @param vals Real valued timeseries.
     * @param windowSize The PAA window size.
     * @param cuts The cut values array used for SAX transform.
     * @return The symbolic representation of the given real time-series.
     * @throws TSException If error occurs.
     */
    private static int[] getISAXVals(double[] vals, int dimensionality, double[] cuts, double mean, double sd) {
        int[] l;
        if (vals.length == cuts.length + 1) {
            l = ts2isax(TimeSeries.zNormalize(vals, mean, sd), cuts);
        } else {
            l = ts2isax(TimeSeries.zNormalize(TimeSeries.paa(vals, dimensionality), mean, sd), cuts);
        }
        return l;
    }

    /**
     * Converts the timeseries into string using given cuts intervals. Useful
     * for not-normal distribution cuts.
     *
     * @param vals The timeseries.
     * @param cuts The cut intervals.
     * @return The timeseries SAX representation.
     */
    private static int[] ts2isax(double[] vals, double[] cuts) {
        int[] l = new int[vals.length];
        for (int i = 0; i < vals.length; i++) {
            l[i] = num2sax(vals[i], cuts);
        }
        return l;
    }

    /**
     * Get mapping of a number to char.
     *
     * @param value the value to map.
     * @param cuts the array of intervals.
     * @return character corresponding to numeric value.
     */
    private static int num2sax(double value, double[] cuts) {
        int count = 0;
        while ((count < cuts.length) && (cuts[count] <= value)) {
            count++;
        }
        return count;
    }

    @Override
    public int compareTo(ISAX o) {
        for (int i = 0; i < size(); i++) {
            if (load[i].compareTo(o.load[i]) > 0) {
                return 1;
            } else if (load[i].compareTo(o.load[i]) < 0) {
                return -1;
            }
        }
        return 0;
    }

    public boolean equals(ISAX o) {
        for (int i = 0; i < size(); i++) {
            if (!load[i].equals(o.load[i])) {
                return false;
            }
        }
        return true;
    }

    public int size() {
        return load.length;
    }

    // minimum bounding word
    public ISAX commonPrefix(ISAX o) {
        assert size() == o.size();
        Symbol[] r = new Symbol[size()];
        for (int i = 0; i < size(); i++) {
            r[i] = load[i].commonPrefix(o.load[i]);
        }
        return new ISAX(r);
    }

    public double minDist(ISAX o) {
        assert size() == o.size();
        double dist = 0;
        for (int i = 0; i < size(); i++) {
            double temp = load[i].minDist(o.load[i]);
            dist = temp * temp;
        }
        return Math.sqrt(dist * windowSize / size());
    }

    public boolean extendByOneBit(int a, int maxCard) {
        for (Symbol s : load) {
            if (s.width >= maxCard) {
                return false;
            }
        }
        for (int i = 0; i < size(); i++) {
            Symbol s = load[i];
            int bit = (a & (1 << (size() - 1 - i))) >> (size() - 1 - i);
            s.load = (s.load << 1) + bit;
        }
        return true;
    }
}

class Symbol implements Comparable<Symbol> {

    public int load;
    public int width;

    Symbol(int workload, int cardinality) {
        load = workload;
        width = (int) Math.ceil(Math.log(cardinality - 1) / Math.log(2));
    }

    Symbol(Symbol o) {
        load = o.load;
        width = o.width;
    }

    public Symbol commonPrefix(Symbol o) {

        int minWidth = width < o.width ? width : o.width;
        int prefix = 0;
        int commonWidth = 0;
        for (int i = 1; i <= minWidth; i++) {
            int bitLoad = (load & (1 << (width - i))) >> (width - i);
            int bitOLoad = (o.load & (1 << (o.width - i))) >> (o.width - i);
            if (bitLoad == bitOLoad) {
                prefix = prefix << 1;
                prefix = prefix + bitLoad;
                commonWidth++;
            } else {
                break;
            }
        }
        return new Symbol(prefix, commonWidth);
    }

    @Override
    public int compareTo(Symbol o) {
        if (width == o.width) {
            return load - o.load;
        } else if (width > o.width) {
            int cardDiff = width - o.width;
            int rsLoad = load >> cardDiff;
            return rsLoad - o.load;
        } else {
            int cardDiff = o.width - width;
            int rsOLoad = o.load >> cardDiff;
            return load - rsOLoad;
        }
    }

    public boolean equals(Symbol o) {
        return load == o.load && width == o.width;
    }

    public double minDist(Symbol o) {
        if (width == o.width) {
            double[][] distMat = NormalAlphabet.getDistanceMatrix(1 << width);
            return distMat[load][o.load];
        } else {
            if (width > o.width) {
                double[][] distMat = NormalAlphabet.getDistanceMatrix(1 << width);
                int cardDiff = width - o.width;
                int rsLoad = load >> cardDiff;
                if (rsLoad > o.load) {
                    int lsOLoad = (o.load << cardDiff) | (Integer.MAX_VALUE >> (Integer.SIZE - cardDiff));
                    return distMat[load][lsOLoad];
                } else if (rsLoad < o.load) {
                    int lsOLoad = (o.load << cardDiff) & (Integer.MAX_VALUE << cardDiff);
                    return distMat[load][lsOLoad];
                } else {
                    return 0;
                }
            } else {
                double[][] distMat = NormalAlphabet.getDistanceMatrix(1 << o.width);
                int widthDiff = o.width - width;
                int rsOLoad = o.load >> widthDiff;
                if (load > rsOLoad) {
                    int lsLoad = (load << widthDiff) | (Integer.MAX_VALUE >> (Integer.SIZE - widthDiff));
                    return distMat[load][lsLoad];
                } else if (load < rsOLoad) {
                    int lsLoad = (load << widthDiff) & (Integer.MAX_VALUE << widthDiff);
                    return distMat[load][lsLoad];
                } else {
                    return 0;
                }
            }
        }
    }
}
