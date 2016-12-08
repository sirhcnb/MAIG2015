package bachelor;

import bachelor.interactive.InteractiveFilePersistence;
import com.anji.integration.LogEventListener;
import com.anji.integration.PersistenceEventListener;
import com.anji.integration.PresentationEventListener;
import com.anji.neat.Evolver;
import com.anji.neat.NeatConfiguration;
import com.anji.persistence.Persistence;
import com.anji.run.Run;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.anji.util.Reset;
import iec.GifSequenceWriter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Genotype;
import org.jgap.event.GeneticEvent;
import own.FilePersistenceMario;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pierre on 26-10-2016.
 */
public class MarioTrainer implements Configurable {
    protected static Logger logger = Logger.getLogger(Evolver.class);

    /**
     * properties key, # generations in run
     */
    public static final String NUM_GENERATIONS_KEY = "num.generations";
    public static final String POPULATION_SIZE = "popul.size";

    /**
     * properties key, fitness function class
     */
    public static final String FITNESS_FUNCTION_CLASS_KEY = "fitness_function";
    private static final String FITNESS_THRESHOLD_KEY = "fitness.threshold";
    private static final String RESET_KEY = "run.reset";

    /**
     * properties key, target fitness value - after reaching this run will halt
     */
    public static final String FITNESS_TARGET_KEY = "fitness.target";

    private static NeatConfiguration config = null;

    Genotype genotype = null;

    //Generations and population per generation
    public int numEvolutions = 0;
    public int populationSize = 0;

    private double targetFitness = 0.0d;
    private double thresholdFitness = 0.0d;
    private int maxFitness = 0;

    private InteractiveFilePersistence db = null;

    //Object to save as and load from csv format
    private CsvFormat csv;

    //Finding best chromosome each generation
    static ArrayList<Chromosome> bestChroms = new ArrayList<Chromosome>();

    //For gif creation
    public int folderName = 0;
    public static FitnessFunction ff = new FitnessFunction();

    /**
     * Starts the automated trainer.
     * @throws Throwable If initialization of Configuration object fails.
     */
    public MarioTrainer() {
        super();

        csv = new CsvFormat();
    }

    /**
     * Initialize our Configration object and load in the parameters.
     * Contains important information of parts to build the NN.
     * @param props configuration parameters.
     * @throws Exception In case we fail to load in the configuration file.
     */
    @Override
    public void init(Properties props) throws Exception {
        ff.init(props);
        ff.levelOptions = "-mix 16 -miy 223";

        boolean doReset = props.getBooleanProperty(RESET_KEY, false);
        if (doReset) {
            logger.warn("Resetting previous run !!!");
            Reset resetter = new Reset(props);
            resetter.setUserInteraction(false);
            resetter.reset();
        }

        config = new NeatConfiguration(props);

        // peristence
        db = (InteractiveFilePersistence) props.singletonObjectProperty(Persistence.PERSISTENCE_CLASS_KEY);
        numEvolutions = props.getIntProperty(NUM_GENERATIONS_KEY);
        targetFitness = props.getDoubleProperty(FITNESS_TARGET_KEY, 1.0d);
        thresholdFitness = props.getDoubleProperty(FITNESS_THRESHOLD_KEY, targetFitness);
        populationSize = props.getIntProperty(POPULATION_SIZE);

        // run
        Run run = (Run) props.singletonObjectProperty(Run.class);
        db.startRun(run.getName());
        config.getEventManager().addEventListener(GeneticEvent.GENOTYPE_EVALUATED_EVENT, run);

        // logging
        LogEventListener logListener = new LogEventListener(config);
        config.getEventManager().addEventListener(GeneticEvent.GENOTYPE_EVOLVED_EVENT, logListener);
        config.getEventManager()
                .addEventListener(GeneticEvent.GENOTYPE_EVALUATED_EVENT, logListener);

        // persistence
        PersistenceEventListener dbListener = new PersistenceEventListener(config, run);
        dbListener.init(props);
        config.getEventManager().addEventListener(
                GeneticEvent.GENOTYPE_START_GENETIC_OPERATORS_EVENT, dbListener);
        config.getEventManager().addEventListener(
                GeneticEvent.GENOTYPE_FINISH_GENETIC_OPERATORS_EVENT, dbListener);
        config.getEventManager().addEventListener(GeneticEvent.GENOTYPE_EVALUATED_EVENT, dbListener);

        // presentation
        PresentationEventListener presListener = new PresentationEventListener(run);
        presListener.init(props);
        config.getEventManager().addEventListener(GeneticEvent.GENOTYPE_EVALUATED_EVENT,
                presListener);
        config.getEventManager().addEventListener(GeneticEvent.RUN_COMPLETED_EVENT, presListener);

        // fitness function
        BulkFitnessFunction fitnessFunc = (BulkFitnessFunction) props
                .singletonObjectProperty(FITNESS_FUNCTION_CLASS_KEY);
        config.setBulkFitnessFunction(fitnessFunc);
        maxFitness = fitnessFunc.getMaxFitnessValue();

        // load population, either from previous run or random
        genotype = db.loadGenotype(config);

        if (genotype != null)
            logger.info("genotype from previous run");
        else {
            genotype = Genotype.randomInitialGenotype(config);
            logger.info("random genotype");
        }
    }

    /**
     * Method being used to train automatically, according to distance traveled.
     * @throws Exception In case DB storing fails.
     */
    public void trainDistanceTraveled() throws Exception {
        String evaluationType = "DistanceTraveled";

        logger.info("Run: start");

        for (int generation = 0; generation < numEvolutions; generation++) {

            //Generation step
            System.out.println("*************** Running generation: " + generation + " ***************");
            logger.info("Generation " + generation + ": start");

            //Create gif folder for specific fitness function purpose
            new File("db/gifs/automated" + folderName).mkdirs();

            //Get all the chromosomes (for evaluation)
            ArrayList<Chromosome> chroms = (ArrayList<Chromosome>) genotype.getChromosomes();

            //Evaluate each chromosome in the population
            ff.setEvaluationType(evaluationType);
            ff.evaluate(chroms);

            /*for (int i = 0; i < populationSize; i++) {
                //Get a chromosome
                Chromosome chrommie = (Chromosome) chroms.get(i);

                //Evaluate that chromosome
                int fitness = ff.evaluateChromosome(chrommie, evaluationType);
                chrommie.setFitnessValue(fitness);
            }*/

            //ff.generation++;
            System.out.println("Generation after record: " + ff.generation + " | " + ff);

            //Get chromosome with best fitness
            Chromosome chosen = genotype.getFittestChromosome();

            csv.writeSingleToString(chosen.getFitnessValue(), ff.generation);

            //If fitness value hits the tartet, stop evolving and save the chromosome to desktop
            int bestFitness = chosen.getFitnessValue();
            if(bestFitness >= targetFitness-1 || generation == 30)
            {
                //Updates the run file with the newest information
                config.lockSettings();
                config.getEventManager().fireGeneticEvent(
                        new GeneticEvent( GeneticEvent.GENOTYPE_EVALUATED_EVENT, genotype ) );

                new File(System.getProperty("user.home") + "/Desktop/bestAutoChromosome/").mkdir();

                FileUtils.copyDirectory(
                        new File("./db"),
                        new File(System.getProperty("user.home") + "/Desktop/bestAutoChromosome/db")
                );
                FileUtils.copyDirectory(
                        new File("./nevt"),
                        new File(System.getProperty("user.home") + "/Desktop/bestAutoChromosome/nevt")
                );

                /*db.saveChromosomes(chroms, System.getProperty("user.home") + "/Desktop/bestAutoChromosome/");
                System.out.println("Best automated chromosome saved to the desktop successfully!");

                csv.generateCsvFileAuto();
                System.out.println("GenFit for best automated chromosome saved to the desktop successfully!");

                InteractiveFilePersistence.copyFile(
                        "./db/run/runtestrun.xml",
                        System.getProperty("user.home") + "/Desktop/bestAutoChromosome/run.xml"
                );*/
                break;
            }

            //Add best evaluated chromosome to the list and save it
            bestChroms.add(chosen);
            db.storeToFolder(chosen, "./db/best/automated");
            genotype.evolveGif();

            GifSequenceWriter.fileNumber = 0;
            folderName++;
        }

        System.exit(0);
    }

    /**
     * Starts the automated trainer.
     * @param args nothing, just the program name as usual.
     * @throws Throwable If initialization of Configuration object fails.
     */
    public static void main(String[] args) throws Throwable {
        Properties props = new Properties("marioAuto.properties");
        ff.init(props);
        ff.levelOptions = "-mix 16 -miy 223 -ld 1 -ll 500";
        ff.generation = 0;

        try {
            MarioTrainer mT = new MarioTrainer();
            mT.init(props);
            mT.trainDistanceTraveled();
        } catch (Throwable th) {
            System.out.println(th);
        }
    }
}
