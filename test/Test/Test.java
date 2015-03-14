/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Test;

import ISAXIndex.Index;
import ISAXIndex.TimeSeries;
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
        String FILE = "ecg100.arff";
        String DATA_VALUE_ATTRIBUTE = "value0";
        int windowSize = 360;
        int DIMENSIONALITY = 4;
        int CARDINALITY = 16;
        int LENGTH = -1;
        Level level = Level.INFO;

        // get the data first
        Instances tsData = ConverterUtils.DataSource.read(FILE);
        Attribute dataAttribute = tsData.attribute(DATA_VALUE_ATTRIBUTE);
        double[] timeseries = TimeSeries.toRealSeries(tsData, dataAttribute);

        if (LENGTH > 0) {
            timeseries = TimeSeries.getSubSeries(timeseries, 0, LENGTH);
        }

        double mean = TimeSeries.mean(timeseries);
        double std = TimeSeries.stDev(timeseries);

        Index index = new Index(CARDINALITY, DIMENSIONALITY);
        Index.setLoggerLevel(level);

//        Date start = new Date();
        for (int i = 0; i < timeseries.length - windowSize + 1; i++) {
            double[] subSeries = TimeSeries.getSubSeries(timeseries, i, i + windowSize);
            TimeSeries ts = new TimeSeries(subSeries,i,0);
            index.add(ts, mean, std);
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
            double[] subSeries = TimeSeries.getSubSeries(timeseries, i, i + windowSize);
            TimeSeries ts = new TimeSeries(subSeries,i,0);
            index.remove(ts, mean, std);
        }
    }

}
