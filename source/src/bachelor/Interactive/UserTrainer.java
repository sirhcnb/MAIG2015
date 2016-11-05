package bachelor.Interactive;

import bachelor.FitnessFunction;
import bachelor.MarioTrainer;
import ch.idsia.benchmark.mario.engine.MarioVisualComponent;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.tools.GameViewer;
import com.anji.integration.LogEventListener;
import com.anji.integration.PersistenceEventListener;
import com.anji.integration.PresentationEventListener;
import com.anji.neat.Evolver;
import com.anji.neat.NeatConfiguration;
import com.anji.neat.NeatCrossoverReproductionOperator;
import com.anji.persistence.FilePersistence;
import com.anji.persistence.Persistence;
import com.anji.run.Run;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.anji.util.Reset;
import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;
import iec.GenotypeGif;
import iec.GifSequenceWriter;
import javafx.stage.FileChooser;
import org.apache.log4j.Logger;
import org.jgap.*;
import org.jgap.event.GeneticEvent;
import org.w3c.dom.Document;
import own.FilePersistenceMario;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Pierre on 30-10-2016.
 */
public class UserTrainer implements Configurable {
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

    private FilePersistenceMario db = null;

    //Finding best chromosome each generation
    static ArrayList<Chromosome> bestChroms = new ArrayList<Chromosome>();

    //The loaded chromosome by the user
    private Chromosome loadedChrom;

    //For gif creation and handling in UserInterface
    public int folderName = 0;
    private static FitnessFunction ff = new FitnessFunction();

    /**
     * Starts the user interaction trainer.
     * @throws Throwable If initialization of Configuration object fails.
     */
    public UserTrainer() throws Throwable {
        super();

        Properties props = new Properties("marioInteractive.properties");
        Persistence db = (Persistence) props.newObjectProperty(Persistence.PERSISTENCE_CLASS_KEY);
        ff.init(props);
        ff.levelOptions = "-mix 16 -miy 223";

        //RUN
        try {
            init(props);
        } catch (Throwable th) {
            System.out.println(th);
        }

        ff.generation = 0;
        ff.populationSize = populationSize;
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
        boolean doReset = props.getBooleanProperty(RESET_KEY, false);
        if (doReset) {
            logger.warn("Resetting previous run !!!");
            Reset resetter = new Reset(props);
            resetter.setUserInteraction(false);
            resetter.reset();
        }

        config = new NeatConfiguration(props);

        // peristence
        db = (FilePersistenceMario) props.singletonObjectProperty(Persistence.PERSISTENCE_CLASS_KEY);
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
     * Method being used to train according to users interaction with the UI.
     * @throws Exception In case gif sequence writer fails.
     */
    public void trainWithInteraction() throws Exception {
        String evaluationType = "Interaction";

        logger.info( "Run: start" );

        System.out.println("*************** Running generation: " + ff.generation + " ***************");
        logger.info( "Generation " + ff.generation + ": start" );

        //Create gif folder for training with interaction
        new File("db/gifs/interaction/" + folderName).mkdirs();

        //Get all chromosomes
        List<Chromosome> chroms = genotype.getChromosomes();

        System.out.println(chroms.size());

        //Evaluate each chromosome in the population
        for (int i = 0; i < populationSize; i++) {
            //Get a chromosome
            Chromosome chrommie = (Chromosome) chroms.get(i);

            //Record that chromosome (Fitness will always be 0, as the user is to chose which chromosomes to use)
            int fitness = ff.evaluateChromosome(chrommie, evaluationType);
            chrommie.setFitnessValue(fitness);

            System.out.println(chrommie.getId());

            //Create and save gif
            GifSequenceWriter.createGIF("db/gifs/interaction/" + folderName + "/");
        }

        //Keep track of current generation (for server part and comparison)
        ff.generation++;
        System.out.println("Generation after record: " + ff.generation + " | " + ff);

        GifSequenceWriter.fileNumber = 0;
        folderName++;
    }

    /**
     * Basic breed functionality. ANJI handles everything regarding reproduction, mutation and so on.
     * @param chosenGifs Chosen chromosomes to influence the new offsprings by a huge margin.
     */
    public void breed(boolean[] chosenGifs, boolean isChromLoaded) throws InvalidConfigurationException {
        List<Chromosome> chroms = genotype.getChromosomes();

        //If a chromosome has been loaded, breed from this chromosome only and ignore the others
        if(isChromLoaded == true)
        {
            //Create new genotype from our loaded chromosome and begin evolving from this point
            loadedChrom.setFitnessValue(1000);
            List<Chromosome> loadedChromList = new ArrayList<>();
            loadedChromList.add(loadedChrom);
            genotype = new Genotype(config, loadedChromList);


            //Start anew from generation 0
            ff.generation = 0;
        } else {
            for(int i = 0; i < chosenGifs.length; i++){
                if(chosenGifs[i] == true) {
                    Chromosome chosenChrom = chroms.get(i);
                    chosenChrom.setFitnessValue(1000);
                } else {
                    Chromosome notChosenChrom = chroms.get(i);
                    notChosenChrom.setFitnessValue(0);
                }
            }
        }

        genotype.evolveGif();
    }

    /**
     * Load in the chromosome and evolve to reproduce and populate according to this chromosome
     */
    public void loadChromosome(File file) throws Exception {
        //Load in the whole XML file as one string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line = br.readLine()) != null){
            sb.append(line.trim());
        }

        //Create chromosome from XML string
        loadedChrom = FilePersistence.chromosomeFromXml(config, sb.toString());
    }

    /**
     * Returns the current FitnessFunction object being used.
     * @return FitnessFunction object
     */
    public static FitnessFunction getFf() {
        return ff;
    }
}