package cc.mrlda;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.lib.MultipleOutputs;

import cc.mrlda.VariationalInference.ParameterCounter;
import cc.mrlda.util.LogMath;

import com.google.common.base.Preconditions;

import edu.umd.cloud9.io.map.HMapIFW;
import edu.umd.cloud9.io.pair.PairOfIntFloat;
import edu.umd.cloud9.io.pair.PairOfInts;
import edu.umd.cloud9.util.map.HMapIV;

public class TermReducer extends MapReduceBase implements
    Reducer<PairOfInts, DoubleWritable, IntWritable, DoubleWritable> {
  private static HMapIV<Set<Integer>> lambdaMap = null;

  private static int numberOfTerms;
  private static int numberOfTopics = Settings.DEFAULT_NUMBER_OF_TOPICS;
  private static boolean learning = Settings.LEARNING_MODE;

  private int topicIndex = 0;
  private double normalizeFactor = 0;

  private MultipleOutputs multipleOutputs;
  private OutputCollector<PairOfIntFloat, HMapIFW> outputBeta;

  private IntWritable intWritable = new IntWritable();
  private DoubleWritable doubleWritable = new DoubleWritable();

  private PairOfIntFloat outputKey = new PairOfIntFloat();
  private HMapIFW outputValue = new HMapIFW();

  public void configure(JobConf conf) {
    multipleOutputs = new MultipleOutputs(conf);

    numberOfTerms = conf.getInt(Settings.PROPERTY_PREFIX + "corpus.terms", Integer.MAX_VALUE);
    numberOfTopics = conf.getInt(Settings.PROPERTY_PREFIX + "model.topics",
        Settings.DEFAULT_NUMBER_OF_TOPICS);
    learning = conf.getBoolean(Settings.PROPERTY_PREFIX + "model.train", Settings.LEARNING_MODE);

    boolean informedPrior = conf.getBoolean(Settings.PROPERTY_PREFIX + "model.informed.prior",
        false);

    Path[] inputFiles;
    SequenceFile.Reader sequenceFileReader = null;

    try {
      inputFiles = DistributedCache.getLocalCacheFiles(conf);

      if (inputFiles != null) {
        for (Path path : inputFiles) {
          try {
            sequenceFileReader = new SequenceFile.Reader(FileSystem.getLocal(conf), path, conf);

            if (path.getName().startsWith(Settings.BETA)) {
              continue;
            } else if (path.getName().startsWith(InformedPrior.ETA)) {
              Preconditions.checkArgument(lambdaMap == null,
                  "Lambda matrix was initialized already...");
              lambdaMap = InformedPrior.importEta(sequenceFileReader);

            } else {
              throw new IllegalArgumentException("Unexpected file in distributed cache: "
                  + path.getName());
            }
          } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
          } catch (IOException ioe) {
            ioe.printStackTrace();
          } finally {
            IOUtils.closeStream(sequenceFileReader);
          }
        }
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    Preconditions.checkArgument(informedPrior == (lambdaMap != null),
        "Fail to initialize informed prior...");

    System.out.println("======================================================================");
    System.out.println("Available processors (cores): "
        + Runtime.getRuntime().availableProcessors());
    long maxMemory = Runtime.getRuntime().maxMemory();
    System.out.println("Maximum memory (bytes): "
        + (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));
    System.out.println("Free memory (bytes): " + Runtime.getRuntime().freeMemory());
    System.out.println("Total memory (bytes): " + Runtime.getRuntime().totalMemory());
    System.out.println("======================================================================");
  }

  public void reduce(PairOfInts key, Iterator<DoubleWritable> values,
      OutputCollector<IntWritable, DoubleWritable> output, Reporter reporter) throws IOException {
    if (key.getLeftElement() == 0) {
      double sum = values.next().get();
      while (values.hasNext()) {
        sum += values.next().get();
      }

      Preconditions.checkArgument(key.getRightElement() >= 0,
          "Unexpected sequence order for Convergence Criteria: " + key.toString());

      intWritable.set(key.getRightElement());
      doubleWritable.set(sum);
      output.collect(intWritable, doubleWritable);

      return;
    }

    // I would be very surprised to get here...
    Preconditions.checkArgument(learning, "Invalid key from Mapper");
    reporter.incrCounter(ParameterCounter.TOTAL_TERM, 1);

    double phiValue = values.next().get();
    while (values.hasNext()) {
      phiValue = LogMath.add(phiValue, values.next().get());
    }

    if (topicIndex != key.getLeftElement()) {
      if (topicIndex == 0) {
        outputBeta = multipleOutputs.getCollector(Settings.BETA, Settings.BETA, reporter);
      } else {
        outputKey.set(topicIndex, (float) normalizeFactor);
        outputBeta.collect(outputKey, outputValue);
      }

      topicIndex = key.getLeftElement();
      if (lambdaMap != null) {
        phiValue = LogMath.add(
            InformedPrior.getEta(key.getRightElement(), lambdaMap.get(topicIndex)), phiValue);
      }
      normalizeFactor = phiValue;
      outputValue.clear();
      outputValue.put(key.getRightElement(), (float) phiValue);
    } else {
      if (lambdaMap != null) {
        phiValue = LogMath.add(
            InformedPrior.getEta(key.getRightElement(), lambdaMap.get(topicIndex)), phiValue);
      }
      normalizeFactor = LogMath.add(normalizeFactor, phiValue);
      outputValue.put(key.getRightElement(), (float) phiValue);
    }
  }

  public void close() throws IOException {
    if (!outputValue.isEmpty()) {
      outputKey.set(topicIndex, (float) normalizeFactor);
      outputBeta.collect(outputKey, outputValue);
    }
    multipleOutputs.close();
  }

}