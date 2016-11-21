package own;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Genotype;
import org.jgap.event.GeneticEvent;

import com.anji.Copyright;
import com.anji.integration.LogEventListener;
import com.anji.integration.PersistenceEventListener;
import com.anji.integration.PresentationEventListener;
import com.anji.neat.Evolver;
import com.anji.neat.NeatConfiguration;
import com.anji.persistence.Persistence;
import com.anji.polebalance.DoublePoleBalanceFitnessFunction;
import com.anji.run.Run;
import com.anji.util.Configurable;
import com.anji.util.DummyConfiguration;
import com.anji.util.Properties;
import com.anji.util.Reset;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.MarioAIOptions;
import iec.GenotypeGif;
import iec.GifSequenceWriter;
import iec.MarioGIF;
import own.util.FitnessCSVWriter;

public class MarioNeat implements Configurable{

	protected static Logger logger = Logger.getLogger( Evolver.class );
	
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
	//private static MarioNEATConfiguration config = null;
	
	private Chromosome champ = null;
	Genotype genotype = null;
	GenotypeGif genotypeGif = null;

	public int numEvolutions = 0;
	public int populationSize = 0;
	
	private double targetFitness = 0.0d;
	private double thresholdFitness = 0.0d;
	private int maxFitness = 0;

	private FilePersistenceMario db = null;
	
	// FOR FINDING THE BEST CHROMOSONES FOR EACH RUN: 
	static ArrayList<Chromosome> bestChroms = new ArrayList<Chromosome>(); 
	
	// FOR GIF CREATION 
	public int folderName = 0; 
	public static MarioFitnessFunction ff = new MarioFitnessFunction();
	static ArrayList<Chromosome> iecCandidates = new ArrayList<Chromosome>(); 
	/**
	 * ctor; must call <code>init()</code> before using this object
	 */
	public MarioNeat() {
		super();
	}
	
	@Override
	public void init(Properties props) throws Exception {
		ff.init(props);
		boolean doReset = props.getBooleanProperty( RESET_KEY, false );
		if ( doReset ) {
			logger.warn( "Resetting previous run !!!" );
			Reset resetter = new Reset( props );
			resetter.setUserInteraction( false );
			resetter.reset();
		}

		config = new NeatConfiguration( props );
		//config = new MarioNEATConfiguration( props );
		

		// peristence
		db = (FilePersistenceMario)  props.singletonObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
		numEvolutions = props.getIntProperty( NUM_GENERATIONS_KEY );
		targetFitness = props.getDoubleProperty( FITNESS_TARGET_KEY, 1.0d );
		thresholdFitness = props.getDoubleProperty( FITNESS_THRESHOLD_KEY, targetFitness );
		populationSize = numEvolutions = props.getIntProperty( POPULATION_SIZE );

		// run
		Run run = (Run) props.singletonObjectProperty( Run.class );
		db.startRun( run.getName() );
		config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, run );

		// logging
		LogEventListener logListener = new LogEventListener( config );
		config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVOLVED_EVENT, logListener );
		config.getEventManager()
				.addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, logListener );

		// persistence
		PersistenceEventListener dbListener = new PersistenceEventListener( config, run );
		dbListener.init( props );
		config.getEventManager().addEventListener(
				GeneticEvent.GENOTYPE_START_GENETIC_OPERATORS_EVENT, dbListener );
		config.getEventManager().addEventListener(
				GeneticEvent.GENOTYPE_FINISH_GENETIC_OPERATORS_EVENT, dbListener );
		config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT, dbListener );

		// presentation
		PresentationEventListener presListener = new PresentationEventListener( run );
		presListener.init( props );
		config.getEventManager().addEventListener( GeneticEvent.GENOTYPE_EVALUATED_EVENT,
				presListener );
		config.getEventManager().addEventListener( GeneticEvent.RUN_COMPLETED_EVENT, presListener );

		// fitness function
		BulkFitnessFunction fitnessFunc = (BulkFitnessFunction) props
				.singletonObjectProperty( FITNESS_FUNCTION_CLASS_KEY );
		config.setBulkFitnessFunction( fitnessFunc );
		maxFitness = fitnessFunc.getMaxFitnessValue();

		// load population, either from previous run or random
		genotype = db.loadGenotype( config );
		genotypeGif = (GenotypeGif) db.loadGenotype( config );
		
		if ( genotype != null )
			logger.info( "genotype from previous run" );
		else {
			genotype = Genotype.randomInitialGenotype( config );
			logger.info( "random genotype" );
		}
		
		

	}

	
	public void trainWithIEC() throws Exception {
		// TODO: Cleaning and evaluating, think about class variables, more proper logging
		logger.info( "Run: start" );
		
		boolean wait = false;
		for(int IECGeneration = 0; IECGeneration < 20; IECGeneration++){
			// IEC STEP
			System.out.println("*************** Running IECgeneration: " + IECGeneration + " ***************"); 
			logger.info( "Generation " + IECGeneration + ": start" );
//			ff.printEvolutionaryParameters(config.props);
			//Reset MarioGIF object and create new .gif folder
			MarioGIF.reset(folderName);
			new File("db/gifs/" + folderName).mkdirs();

			// RECORDING STEP
			List<Chromosome> chroms = genotype.getChromosomes();
			
			ff.delayRecording();
			
			for (int i = 0; i < populationSize; i++) {
				//Get a chromosome
			    Chromosome chrommie = (Chromosome) chroms.get(i);
			    //Record that chromosome
			    ff.recordImages( chrommie, IECGeneration );
			    //Create and save gif 
			    GifSequenceWriter.createGIF("db/gifs/" + folderName + "/");   
			}
	
			ff.generation++;
			System.out.println("Generation after record: " + ff.generation + " | " + ff);
	
			MarioGIF.runIEC(folderName, populationSize);
			
			wait = true;
			while(wait){
				Thread.sleep(10);
				//Check if chromosome has been chosen
				if(MarioGIF.getChosenGif() != -1){
					
					MarioGIF.setVisibility(false);
					
					for (Chromosome c : chroms)
						c.setFitnessValue(0);
					
				 	Chromosome theChosenChrom = (Chromosome) chroms.get( MarioGIF.getChosenGif() );
				 	bestChroms.add(theChosenChrom);
				 	
				 	//Set it's fitness
				 	theChosenChrom.setFitnessValue(100);
				 	db.storeToFolder(theChosenChrom, "./db/best/chromosome");
				 	MarioGIF.changeGifName( IECGeneration );
					genotype.evolveGif();
					//MarioGIF.deleteGifs("./db/gifs/" + folderName);

					wait = false;
				}
			}
			
			iecCandidates.clear();
			GifSequenceWriter.fileNumber = 0; 
			folderName++;
			
			// Changing evolutionary parameters
			// NOTE: Maybe weight.mutation.std.dev
//			config.changePropertyValue("weight.mutation.rate", -0.01f);
//			
//			config.changePropertyValue("add.neuron.mutation.rate", -0.001f);
			
			

			
		}	
	}
	
	public void trainANNWithStaticFitness() throws Exception{
		// AUTOMATED NEATSTEP WITH DISTANCE PASSED AS FITNESS
		for ( int generation = 0; generation < 20; generation++ ) {
			System.out.println("Running generation: " + generation + "..."); 
			Date generationStartDate = Calendar.getInstance().getTime();
			//logger.info( "Automated NEAT Generation " + generation + ": start" );
			
			genotype.evolve();
			
			Chromosome c = genotype.getFittestChromosome();
			bestChroms.add(c);
			db.storeToFolder(c, "./db/best/chromosome");
			
			//System.out.println("Generation in NEAT loop: " + ff.generation + " | " + ff);
			
			System.out.println("Evaluted Chrom: " + generation + ", with fitness: " + c.getFitnessValue());
			// generation finish
//			Date generationEndDate = Calendar.getInstance().getTime();
//			long durationMillis = generationEndDate.getTime() - generationStartDate.getTime();
//			logger.info( "Generation " + generation + ": [" + durationMillis + "]" );
		}
		
//		for(int i = 0; i<20; i++){
//			Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
//			ff.evaluate( bestChroms.get(i), true);
//			ff.setMarioLevel( 0, 0, 0 );
//		}
			
	}
	
	public void testControllersOnDiffLevelSeeds(){
		int chromSize = 20;
		int[] fitnessValues = new int[chromSize];
		String participantName = "VARL";
		
		//GENERALIZED TEST
		int numLevels = 10;
		for(int i = 0; i < chromSize; i++){
			int fitnessVal = 0;
			
			//Get new chromosome
			Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
			
			for(int l = 0; l < numLevels; l++){
				ff.environment.reset(ff.marioAIOptions);
				ff.setMarioLevel( 0, 0, l );
				
				//Evaluate the chromosome
				ff.evaluate( chrom, false );
				fitnessVal += chrom.getFitnessValue();
				
				//Print results
				System.out.println("ChromID: " + i + " | Fitness: " + chrom.getFitnessValue() + " | LEVEL: " + l);
			}
			
			//Save to array
			fitnessValues[ i ] = fitnessVal / numLevels;
		}
		
		FitnessCSVWriter.generateCsvFile("GENERALIZED_FITNESS_", fitnessValues, participantName );
	}
	
	public void testControllersWithOffset(){
		int chromSize = 20;
		int[] fitnessValues = new int[chromSize];
		String participantName = "VARL";
		
		for(int i = 0; i < chromSize; i++){
			
			//Get new chromosome
			Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
			
			int offsetX = 16;	
			int offsetY = 170;
		
			//Find offset X and Y 
			if(i >= 4){
				offsetX = 300;
				offsetY = 170;
			}
			if (i >= 8){
				offsetX = 850;
				offsetY = 190;
			}
			if (i >= 12){
				offsetX = 1748;
				offsetY = 170;
			} 
			if ( i >= 16){
				offsetX = 2716;
				offsetY = 170;
			}
			
			//Move Mario according to generation
			ff.levelOptions = "-vis off -mix " + offsetX + " -miy "+ offsetY;
	
			ff.environment.reset(ff.levelOptions);
			
			//Evaluate the chromosome
			ff.evaluate( chrom, false );
			
			//Print results
			System.out.println("ChromID: " + i + " | Fitness: " + chrom.getFitnessValue());
			
			//Save to array
			fitnessValues[ i ] = chrom.getFitnessValue() - ( offsetX / 16 );
		}
		
		FitnessCSVWriter.generateCsvFile("FITNESS_W_OFFSET_", fitnessValues, participantName );
	}
	
	public void testControllersFromLevelBeggining(){
		int chromSize = 20;
		int[] fitnessValues = new int[chromSize];
		String participantName = "VARL";
		
		for(int i = 0; i < chromSize; i++){
			//Get new chromosome
			Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
			
			//Evaluate the chromosome
			ff.evaluate( chrom, true );
			
			//Print results
			System.out.println("ChromID: " + i + " | Fitness: " + chrom.getFitnessValue());
			
			//Save to array
			fitnessValues[ i ] = chrom.getFitnessValue( );
		}
	}
	
	public static void main( String[] args ) throws Throwable {
		Properties props = new Properties( "mario.properties" );
		Persistence db = (Persistence) props.newObjectProperty( Persistence.PERSISTENCE_CLASS_KEY );
		ff.init( props );
		ff.levelOptions = "-mix 16 -miy 223"; //Set starting position
		ff.generation = 0;
		ff.adjustFPS(); //Switches delay to opposite
		
		//RUN
		try {
			System.out.println("Booting up!");
			MarioNeat mNeat = new MarioNeat();
			mNeat.init(props);
			mNeat.trainWithIEC();
			//mNeat.trainANNWithStaticFitness();
			
			System.out.println("Last up!");
			
		}
		catch ( Throwable th ) {
			System.out.println(th);
		}

		
		
//		int chromSize = 20;
		//int[] fitnessValues = new int[chromSize];
		//String participantName = "STA1";
////		
//		//GENERALIZED TEST TRAINED IN MULTIPLE SEED LEVELS: REQUIRES CHROMOSOMES TO BE LOADED
//		int numLevels = 10;
//		for(int i = 0; i < chromSize; i++){
//			int fitnessVal = 0;
//			
//			//Get new chromosome
//			Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
//			
//			for(int l = 0; l < numLevels; l++){
//				ff.environment.reset(ff.marioAIOptions);
//				ff.setMarioLevel( 0, 0, l );
//				
//				//Evaluate the chromosome
//				ff.evaluate( chrom, false );
//				fitnessVal += chrom.getFitnessValue();
//				
//				//Print results
//				System.out.println("ChromID: " + i + " | Fitness: " + chrom.getFitnessValue() + " | LEVEL: " + l);
//			}
//			
//			//Save to array
//			fitnessValues[ i ] = fitnessVal / numLevels;
//		}
		
		
		
//		FitnessCSVWriter.generateCsvFileInteractive("GENERALIZED_FITNESS_", fitnessValues, participantName );
//		
//		// EVALUATE WITH OFFSET
//		for(int i = 0; i < chromSize; i++){
//					
//				//Get new chromosome
//				Chromosome chrom = db.loadChromosome( Integer.toString( i ), config );
//				
//				int offsetX = 16;	
//				int offsetY = 170;
//			
//				//Find offset X and Y 
//				if(i >= 4){
//					offsetX = 300;
//					offsetY = 170;
//				}
//				if (i >= 8){
//					offsetX = 850;
//					offsetY = 190;
//				}
//				if (i >= 12){
//					offsetX = 1748;
//					offsetY = 170;
//				} 
//				if ( i >= 16){
//					offsetX = 2716;
//					offsetY = 170;
//				}
//				
//				//Move Mario according to generation
//				ff.levelOptions = "-vis on -mix " + offsetX + " -miy "+ offsetY;
//		
//				ff.environment.reset(ff.levelOptions);
//				
//				//Evaluate the chromosome
//				ff.evaluate( chrom, true );
//				
//				//Print results
//				System.out.println("ChromID: " + i + " | Fitness: " + chrom.getFitnessValue());
//				
//				//Save to array
//				//fitnessValues[ i ] = chrom.getFitnessValue() - ( offsetX / 16 );
//			}
//				
//			FitnessCSVWriter.generateCsvFileInteractive("FITNESS_W_OFFSET_", fitnessValues, participantName );
//			
//		
//		// Load in chromosome: 
//		String chromId = "11";
//		
//		
//		Chromosome chrom = db.loadChromosome( chromId, config );
//		ff.levelOptions = "-mix 1748 -miy 150";
//		ff.environment.reset(ff.levelOptions);
//		ff.generation = 0;
//		if ( chrom != null ){
//			System.out.println("Chrom: " + chrom);
//			
//			ff.evaluate(chrom, true);
//			//throw new IllegalArgumentException( "no chromosome found.");
//		}

		System.exit(0);
	}
	
}
