package cc.mrlda;

public class Settings extends cc.mrlda.util.Settings {


  // set the minimum memory threshold, in bytes
  public static final int MEMORY_THRESHOLD = 64 * 1024 * 1024;

  public static final String TOPIC_OPTION = "topic";
  public static final String TERM_OPTION = "term";
  public static final String ITERATION_OPTION = "iteration";

  // todo: add in inference code
  public static final String INFERENCE_MODE_OPTION = "test";
  public static final String RANDOM_START_GAMMA_OPTION = "randomstart";
  // todo: add in result option in code
  public static final String RESUME_OPTION = "modelindex";

  public static final int DEFAULT_NUMBER_OF_TOPICS = 100;
  public static final int DEFAULT_GLOBAL_MAXIMUM_ITERATION = 50;

  public static final boolean RANDOM_START_GAMMA = false;
  public static final boolean LEARNING_MODE = true;
  public static final boolean RESUME = false;

  public static final String PROPERTY_PREFIX = Settings.class.getPackage() + "" + DOT;

  public static final String TEMP = "temp";

  public static final String GAMMA = "gamma";
  public static final String BETA = "beta";
  public static final String ALPHA = "alpha";

  public static final float DEFAULT_GAMMA_UPDATE_CONVERGE_THRESHOLD = 0.0001f;
  public static final float DEFAULT_GAMMA_UPDATE_CONVERGE_CRITERIA = 0.0001f;
  public static final int DEFAULT_GAMMA_UPDATE_MAXIMUM_ITERATION = 50;

  public static final float DEFAULT_ALPHA_UPDATE_CONVERGE_THRESHOLD = 0.000001f;
  public static final int DEFAULT_ALPHA_UPDATE_MAXIMUM_ITERATION = 1000;

  public static final float DEFAULT_ALPHA_UPDATE_INITIAL = 100f;
  public static final int DEFAULT_ALPHA_UPDATE_MAXIMUM_DECAY = 10;
  public static final float DEFAULT_ALPHA_UPDATE_DECAY_FACTOR = 0.8f;

  /**
   * @deprecated
   */
  public static final int DEFAULT_ALPHA_UPDATE_SCALE_FACTOR = 10;

  public static final double DEFAULT_GLOBAL_CONVERGE_CRITERIA = 0.000001;
  public static final double DEFAULT_COUNTER_SCALE = 100;
}