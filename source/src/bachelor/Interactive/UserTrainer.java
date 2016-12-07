package bachelor.interactive;

import bachelor.FitnessFunction;
import bachelor.CsvFormat;
import com.anji.integration.LogEventListener;
import com.anji.integration.PersistenceEventListener;
import com.anji.integration.PresentationEventListener;
import com.anji.integration.XmlPersistableChromosome;
import com.anji.neat.Evolver;
import com.anji.neat.NeatConfiguration;
import com.anji.persistence.Persistence;
import com.anji.run.Run;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import com.anji.util.Reset;
import iec.GifSequenceWriter;
import org.apache.log4j.Logger;
import org.jgap.*;
import org.jgap.event.GeneticEvent;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public Genotype genotype = null;

    //Generations and population per generation
    public int numEvolutions = 0;
    public int populationSize = 0;

    private double targetFitness = 0.0d;
    private double thresholdFitness = 0.0d;
    private int maxFitness = 0;

    //Persistence objects
    private InteractiveFilePersistence db = null;

    //Object to save as and load from csv format
    private CsvFormat csv;

    //The loaded chromosome by the user and the forkedFrom id
    private Chromosome loadedChrom;

    private int forkedFrom;

    public Chromosome getPreviewChrom() {
        return previewChrom;
    }

    //The preview chromosome from the server
    private Chromosome previewChrom;
    private int previewChromID;

    //Properties/new run
    private Properties props;
    private boolean isNewRun;

    //For gif creation and handling in UserInterface
    private static FitnessFunction ff = new FitnessFunction();

    /**
     * Starts the user interaction trainer.
     * @throws Throwable If initialization of Configuration object fails.
     */
    public UserTrainer() throws Throwable {
        props = new Properties("marioInteractive.properties");

        ff.generation = 0;

        csv = new CsvFormat();
        isNewRun = true;

        //Initialize User Trainer
        try {
            init(props);
            ff.populationSize = populationSize;
        } catch (Throwable th) {
            System.out.println(th);
        }
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
        ff.levelOptions = "-vlx -500 -vly -500 -mix 16 -miy 223";

        //boolean doReset = props.getBooleanProperty(RESET_KEY, false);
        if (isNewRun) {
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
     * Returns the path to the preview gif when done generating it. If already exist, just return
     * the path to the gif.
     * @return Path to preview gif.
     */
    public String runSinglePreview() throws Exception {
        String evaluationType = "Preview";

        logger.info( "Preview: Start" );

        //Create gif folder for previews
        Path previewPath = Paths.get((System.getProperty("user.home") + "/Documents/previews/"));
        if(!Files.exists(previewPath)) {
            new File(previewPath.toString()).mkdirs();
        }

        //Get the gif if it exists else create a gif in the preview folder
        File chromFile = null;
        if(previewChrom != null)
        {
            chromFile = new File(previewPath.toString() + "/" + previewChromID + ".gif");

            if(!chromFile.exists()) {
                //ff.evaluateChromosome(previewChrom, evaluationType);
                List<Chromosome> previewChromList = new ArrayList<>();
                previewChromList.add(previewChrom);
                ff.setEvaluationType(evaluationType);
                ff.evaluate(previewChromList);
                GifSequenceWriter.fileNumber = previewChromID;
                GifSequenceWriter.createGIF(previewPath.toString() + "/");
            }
        }

        logger.info( "Preview: Stop" );

        if(chromFile != null) {
            return "file:///" + (chromFile.getAbsoluteFile().toString());
        }

        return null;
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

        //Get all chromosomes
        List<Chromosome> chroms = genotype.getChromosomes();

        System.out.println(chroms.size());

        //Evaluate each chromosome in the population
        ff.setEvaluationType(evaluationType);
        ff.evaluate(chroms);

        /*for (int i = 0; i < populationSize; i++) {
            //Get a chromosome
            Chromosome chrommie = (Chromosome) chroms.get(i);

            //Record that chromosome (Fitness will always be 0, as the user is to chose which chromosomes to use)
            int fitness = ff.evaluateChromosome(chrommie, evaluationType);
            chrommie.setFitnessValue(fitness);

            System.out.println(chrommie.getId());

            //Create and save gif.
            GifSequenceWriter.createGIF("db/gifs/interaction/" + folderName + "/");

            //Create and save path of mario image TODO: Out comment if image level isn't to be made
            //GifSequenceWriter.createLevelImage(System.getProperty("user.home") + "/Desktop/levelImage.png");

        }*/

        csv.writeSingleToString(genotype.getFittestChromosome().getFitnessValue(), ff.generation-1);

        //Keep track of current generation (for server part and comparison)
        //ff.generation++;
        System.out.println("Generation after record: " + ff.generation + " | " + ff);
    }

    /**
     * Basic breed functionality. ANJI handles everything regarding reproduction, mutation and so on.
     * @param chosenGifs Chosen chromosomes to influence the new offsprings by a huge margin.
     */
    public void breed(boolean[] chosenGifs) throws Exception {
        List<Chromosome> chroms = genotype.getChromosomes();

        for(int i = 0; i < chosenGifs.length; i++){
            if(chosenGifs[i] == true) {
                Chromosome chosenChrom = chroms.get(i);
                chosenChrom.setFitnessValue(1000);
                System.out.println(chosenChrom.getId());
            } else {
                Chromosome notChosenChrom = chroms.get(i);
                notChosenChrom.setFitnessValue(0);
            }
        }

        genotype.evolveGif();
    }

    /**
     * Makes a new random genotype to start a new run from
     * @throws InvalidConfigurationException If config doesn't work with the genotype
     */
    public void newRun() throws InvalidConfigurationException {
        //Start anew from generation 0
        ff.generation = 0;
        ff.folderName = 0;
        forkedFrom = 0;

        csv = new CsvFormat();
        genotype = Genotype.randomInitialGenotype(config);
    }

    /**
     * Returns the current FitnessFunction object being used.
     * @return FitnessFunction object
     */
    public static FitnessFunction getFf() {
        return ff;
    }

    /**
     * Returns the file persistence object
     * @return InteractiveFilePersistence object
     */
    public InteractiveFilePersistence getDb() {
        return db;
    }

    /**
     * Returns the csvFormat object
     * @return csvFormat object
     */
    public CsvFormat getCsv() {
        return csv;
    }

    /**
     * Returns the forkedFrom int
     * @return forkedFrom ID
     */
    public int getForkedFrom() {
        return forkedFrom;
    }

    /**
     * Loads a chromosome from a XML file
     * @param file To get the path in which we will load the chromosome
     * @throws Exception If I/O fails
     */
    public void loadChromosomes(File file) throws Exception {
        db.loadChromosomes(file);
        csv.loadCSVFromChromosome(file.getName());

        isNewRun = false;
        init(props);
    }

    /**
     * Loads chromosomes from the server
     * @param chroms The chromosomes to be loaded
     * @throws Exception If connection fails
     */
    public void loadChromosomesServer(ArrayList<String> chroms, String runFile) throws Exception {
        loadedChrom = db.loadChromosomesServer(chroms, runFile);
    }

    /**
     * Loads a chromosome from the server into the preview
     * @param id ID of the entry from database
     * @param xmlFormat The chromosome to be previewed
     * @throws Exception If connection fails
     */
    public void loadPreviewChromosome(int id, String xmlFormat) throws Exception {
        previewChrom = db.loadPrevChromosomeServer(config, xmlFormat);
        previewChromID = id;
    }

    /**
     * Saves the chosen chromosomes to the specified path from the file
     * @param chroms Chromosomes to be saved
     * @param file To get the path in which we will save the chromosome
     * @throws Exception If I/O fails
     */
    public void saveChromosomes(ArrayList<Chromosome> chroms, File file) throws Exception {
        //Updates the run file with the newest information
        config.lockSettings();
        config.getEventManager().fireGeneticEvent(
                new GeneticEvent( GeneticEvent.GENOTYPE_EVALUATED_EVENT, genotype ) );

        db.saveChromosomes(chroms, file.getAbsolutePath());
        csv.generateCsvFileInteractive(file.getName());
    }

    /**
     * Makes a genfork file
     * @param file To get the path in which we will save the genFork file
     * @throws Exception If I/O fails
     */
    public void makeGenForkFile(File file) throws Exception {
        db.makeGenForkFile(file, ff.generation, forkedFrom);
    }

    /**
     * Sets the generation and forkedFrom received from a specific XML file
     * @param file The XML file to get the generation and forkedFrom from
     * @throws Exception If I/O fails
     */
    public void setGenFork(File file) throws Exception {
        GenFork genFork = db.getGenAndForkFromFile(file);
        ff.generation = genFork.getGeneration();
        ff.folderName = ff.generation;
        forkedFrom = genFork.getForkedFrom();
    }

    /**
     * Sets the generation received from the server
     * @param generation Generation received from the server when downloading
     */
    public void setGenerationForkServer(int generation, int forkedFrom) {
        ff.generation = generation;
        ff.folderName = ff.generation;
        this.forkedFrom = forkedFrom;
    }

    /**
     * Get the config object
     * @return configurable object
     */
    public static NeatConfiguration getConfig() {
        return config;
    }
}