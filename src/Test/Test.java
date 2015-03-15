/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Test;

import ISAXIndex.DataHandler;
import ISAXIndex.Index;
import ISAXIndex.TSUtils;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 *
 * @author ian
 */
public class Test {

    private static final Logger logger = Logger.getLogger(Test.class.getName());

    static {
        logger.setLevel(Level.ALL);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(ch);
    }

    public static void main(String[] args) throws Exception {
        String FILE = "C:/users/ian/datasets/ecg/ecg100.arff";
        String DATA_VALUE_ATTRIBUTE = "value0";
        int windowSize = 360;
        int DIMENSIONALITY = 4;
        int CARDINALITY = 16;
        int LENGTH = -1;
        Level level = Level.INFO;

        // get the data first
        Instances tsData = ConverterUtils.DataSource.read(FILE);
        Attribute dataAttribute = tsData.attribute(DATA_VALUE_ATTRIBUTE);
        double[] timeseries = TSUtils.toRealSeries(tsData, dataAttribute);

        if (LENGTH > 0) {
            timeseries = TSUtils.getSubSeries(timeseries, 0, LENGTH);
        }

        double mean = TSUtils.mean(timeseries);
        double std = TSUtils.stDev(timeseries);

        Index index = new Index(CARDINALITY, DIMENSIONALITY);
        Index.setLoggerLevel(level);

//        Date start = new Date();
        for (int i = 0; i < timeseries.length - windowSize + 1; i++) {
            double[] subSeries = TSUtils.getSubSeries(timeseries, i, i + windowSize);
            index.add(subSeries, i, mean, std);
        }

        double[] qs = TSUtils.getSubSeries(timeseries, 5100, 5100 + windowSize);
        ArrayList<Long> nn = index.NN(qs, mean, std, new DataInMemory(timeseries, windowSize));

        for (long id : nn) {
            System.out.println(id);
        }

//        Date end = new Date();
//        System.out.println((end.getTime() - start.getTime()) / 1000);
//        index.integrityCheck();
//        for (Leaf leaf : index) {
//            System.out.println(leaf.dispLoad());
//        }
//        Iterator<Leaf> leafIterator = index.iterator();
//        while (leafIterator.hasNext()) {
//            System.out.println(leafIterator.next().dispLoad());
//        }
//
        for (int i = 0; i < timeseries.length - windowSize + 1; i++) {
            double[] subSeries = TSUtils.getSubSeries(timeseries, i, i + windowSize);
            index.remove(subSeries, i, mean, std);
        }
    }

}

class DataInMemory extends DataHandler {

    double[] vals = null;
    int windowSize = 0;

    DataInMemory(double[] ts, int ws) {
        vals = ts;
        windowSize = ws;
    }

    @Override
    public long size() {
        return vals.length;
    }

    @Override
    public double[] get(long i) {
        assert i + windowSize <= size();
        double[] subSeries = TSUtils.getSubSeries(vals, ((int) i), ((int) i) + windowSize);
        return subSeries;

    }

}
