package Test;

import ISAXIndex.DataHandler;
import ISAXIndex.Index;
import ISAXIndex.TSUtils;
import ISAXIndex.ED;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by wuyafei on 2015/3/24.
 */
public class Test2 {

    private static final Logger logger = Logger.getLogger(Test2.class.getName());

    static {
        logger.setLevel(Level.ALL);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(ch);
    }

    public static void main(String[] args) throws Exception {
        int windowSize = 128;
        int DIMENSIONALITY = 4;
        int CARDINALITY = 16;
        int LENGTH = 8192;
        Level level = Level.INFO;

        //read data
        double[][] data=new double[8192][128];
        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream("discord_rw_sw_128_8192.txt")));
        for(int i=0;i<LENGTH;i++) {
            String[] str = br.readLine().split(" ");
            for(int j=0;j<windowSize;j++){
                data[i][j]=Double.parseDouble(str[j]);
            }
        }
        Index index = new Index(CARDINALITY, DIMENSIONALITY);
        Index.setLoggerLevel(level);

        for (int i = 0; i < LENGTH; i++) {
            double mean = TSUtils.mean(data[i]);
            double std = TSUtils.stDev(data[i]);
            index.add(data[i], i, mean, std);
        }
        //index.remove(data[74],74, TSUtils.mean(data[74]), TSUtils.stDev(data[74]));
        //ArrayList<Long> nn = index.NN(data[74], TSUtils.mean(data[74]), TSUtils.stDev(data[74]), new MatrixData(data, windowSize));
        //Iterator<Long> itr=index.iterator();
        double bsf_dist=0.0;
        int bsf_idx=-1;
        for(Long id:index){
            int idx=id.intValue();
            index.remove(data[idx],idx, TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]));
            ArrayList<Long> nn = index.NN(data[idx], TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]), new MatrixData(data, windowSize));
            double nn_dist=ED.distance(data[idx],data[nn.get(0).intValue()]);
            if(nn_dist>bsf_dist) {
                bsf_dist = nn_dist;
                bsf_idx=idx;
            }
            index.add(data[idx], idx, TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]));
        }
        /*
        while(itr.hasNext()){
            int idx = itr.next().intValue();

            index.remove(data[idx],idx, TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]));
            ArrayList<Long> nn = index.NN(data[idx], TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]), new MatrixData(data, windowSize));
            double nn_dist=ED.distance(data[idx],data[nn.get(0).intValue()]);
            if(nn_dist>bsf_dist) {
                bsf_dist = nn_dist;
                bsf_idx=idx;
            }
            index.add(data[idx], idx, TSUtils.mean(data[idx]), TSUtils.stDev(data[idx]));
        }
        */
        System.out.println("bsf_idx="+bsf_idx);
        System.out.println("bsf_dist="+bsf_dist);


    }

}
class MatrixData extends DataHandler{
    double[][] vals = null;
    int windowSize = 0;

    MatrixData(double[][] ts, int ws) {
        vals = ts;
        windowSize = ws;
    }

    @Override
    public long size() {
        return vals.length;
    }

    @Override
    public double[] get(long i) {
        assert i <= size();
        return vals[(int)i];

    }
}
