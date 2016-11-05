package bachelor;

import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.tools.MarioAIOptions;
import com.anji.integration.Activator;
import com.anji.integration.ActivatorTranscriber;
import com.anji.integration.TargetFitnessFunction;
import com.anji.util.Configurable;
import com.anji.util.Properties;
import org.apache.log4j.Logger;
import org.jgap.BulkFitnessFunction;
import org.jgap.Chromosome;
import own.MarioInputs;

import java.util.*;

/**
 * Created by Pierre on 26-10-2016.
 */
public class FitnessFunction implements BulkFitnessFunction, Configurable {
    //Logger and factory for chromosome activator, being used for evaluation
    private static Logger logger = Logger.getLogger( TargetFitnessFunction.class );
    private ActivatorTranscriber factory;

    //Timer for distance traveled fitness function
    private int timer = 0;

    //Mario variables
    static final MarioAIOptions marioAIOptions = new MarioAIOptions();
    static final BasicTask basicTask = new BasicTask(marioAIOptions);

    static Environment environment = MarioEnvironment.getInstance();
    public BachelorController agent = new BachelorController();

    //Info on stage
    protected byte[][] mergedObservation;
    public String levelOptions = "-vis off -mix 16 -miy 223"; // see class ParameterContainer.java for each flag

    //Control buttons
    boolean[] actions = new boolean[Environment.numberOfKeys];

    public static int populationSize = 0;
    public static int generation = 0;
    public static int difficulty = 0;
    public static int level = 0;
    public static int seed = 0;

    //Define the inputs for Mario
    MarioInputs marioInputs = new MarioInputs(  true, 1, 1, 1, 1,
            true, 2,
            false, true, false, true );

    //Recording params
    public int delayRecording = 100;

    @Override
    public void init(Properties props) throws Exception {
        System.out.println("MarioFitnessFunction: Factory intitiated.");
        factory = (ActivatorTranscriber) props.singletonObjectProperty( ActivatorTranscriber.class );
    }

    @Override
    public void evaluate(List genotypes) {
        System.out.println("Evaluting list of chromosones...");
        System.out.println("Increasing generation" + generation);
        Iterator it = genotypes.iterator();
        while ( it.hasNext() ) {
            Chromosome genotype = (Chromosome) it.next();
            evaluate(genotype, false);
        }
        generation++;
    }

    /**
     * Evaluate for the automated Neat step.
     * @param c The chromosome to evaluate.
     * @param visual Show mario window or not.
     */
    public void evaluate( Chromosome c, boolean visual ) {
        marioAIOptions.setVisualization(visual);

        try {
            Activator activator = factory.newActivator( c );

            // calculate fitness
            int fitness = 0;
            fitness += singleTrial(activator);

            c.setFitnessValue(fitness);
        }
        catch (Throwable e) {
            logger.warn("error evaluating chromosome " + c.toString(), e);
            c.setFitnessValue(0);
        }
    }

    /**
     * Stimulates the chromosome and evaluates according to its fitness.
     * @param activator Stimuli object
     * @return Fitness of the chromosome
     */
    private int singleTrial( Activator activator ) {
        int fitness;

        marioInputs.setRadius(1, 1, 1, 1);
        while(!environment.isLevelFinished()){
            //Set all actions to false
            resetActions();

            //Get inputs
            double[] networkInput = marioInputs.getAllInputs();
            double[] networkOutput = activator.next(networkInput);

            //Get output
            boolean[] actions = getAction(networkOutput);

            //Perform action and tick the environment forward
            environment.performAction(actions);
            makeTick();
        }
        fitness = environment.getEvaluationInfo().distancePassedCells;

        return fitness;
    }

    /**
     * Evaluates according to if its automated or interactive.
     * @param c The chromosome to be evaluated.
     * @param evaluationType Automated or interaction.
     * @return Fitness of the chromosome.
     */
    public int evaluateChromosome(Chromosome c, String evaluationType){
        //New evaluation, put mario at start position
        environment.reset(levelOptions);

        //Turn recording on or off
        marioAIOptions.setVisualization(true);
        environment.recordMario(false);

        int fitness = 0;

        try {
            // Load in chromosome to the factory
            Activator activator = factory.newActivator(c);

            if(evaluationType.equals("DistanceTraveled"))
            {
                fitness = runDistanceEvaluation(activator);
            } else if(evaluationType.equals("Interaction"))
            {
                fitness = runInteraction(activator, delayRecording);
            } else
            {
                System.out.println("No chromosome evaluation for this type yet!: " + evaluationType);
                System.exit(0);
            }
        }
        catch ( Throwable e ) {
            logger.warn( "error evaluating chromosome " + c.toString(), e );
            c.setFitnessValue( 0 );
        }

        return fitness;
    }

    /**
     * Automated evaluation!!
     */
    private int runDistanceEvaluation(Activator activator) {
        //If mario doesn't get further within 5 seconds, we begin new chromosome evaluation
        int longestDistance = 0;
        int currentDistance = environment.getEvaluationInfo().distancePassedCells;

        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                timer++;
            }
        };

        //Start timer, with 1 second delay and increment each second
        t.scheduleAtFixedRate(task, 1000, 1000);

        //Run trial
        while(!environment.isLevelFinished() && timer < 3){
            if(longestDistance < currentDistance) {
                //System.out.println("NEW DISTANCE!!");
                longestDistance = currentDistance;
                timer = 0;
            }

            //Begin recording after some seconds
            /*if(environment.getEvaluationInfo().timeSpent >= ( delayRecording / 1000 ) )
                environment.recordMario(false);*/

            //Set all actions to false
            resetActions();

            //Get inputs
            double[] networkInput = marioInputs.getAllInputs();

            //Feed the inputs to the network and translate it
            double[] networkOutput = activator.next(networkInput);
            boolean[] actions = getAction(networkOutput);

            //Perform action
            environment.performAction(actions);
            makeTick();

            currentDistance = environment.getEvaluationInfo().distancePassedCells;
        }

        //Cancel timer in current evaluation, as we got out of the evaluation while loop
        timer = 0;
        t.cancel();
        t.purge();

        //Get current position of mario and put it as fitness (even though he might have gotten further)
        int fitness = environment.getEvaluationInfo().distancePassedCells;

        return fitness;
    }

    /**
     * Interactive evaluation!!
     */
    private int runInteraction(Activator activator, int delayRecording) {
        //If mario doesn't get further within 5 seconds, we begin new chromosome evaluation
        int longestDistance = 0;
        int currentDistance = environment.getEvaluationInfo().distancePassedCells;

        Timer t = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                timer++;
            }
        };

        //Start timer, with 1 second delay and increment each second
        t.scheduleAtFixedRate(task, 1000, 1000);

        //Run trial
        while(!environment.isLevelFinished() && timer < 3){
            if(longestDistance < currentDistance) {
                //System.out.println("NEW DISTANCE!!");
                longestDistance = currentDistance;
                timer = 0;
            }

            //Begin recording after some seconds
            if(environment.getEvaluationInfo().timeSpent >= ( delayRecording / 1000 ) )
                environment.recordMario(true);

            //Set all actions to false
            resetActions();

            //Get inputs
            double[] networkInput = marioInputs.getAllInputs();

            //Feed the inputs to the network and translate it
            double[] networkOutput = activator.next(networkInput);
            boolean[] actions = getAction(networkOutput);

            //Perform action
            environment.performAction(actions);
            makeTick();

            currentDistance = environment.getEvaluationInfo().distancePassedCells;
        }

        //Cancel timer in current evaluation, as we got out of the evaluation while loop
        timer = 0;
        t.cancel();
        t.purge();

        //Get current position of mario and put it as fitness (even though he might have gotten further)
        int fitness = environment.getEvaluationInfo().distancePassedCells;

        return fitness;
    }

    public static MarioAIOptions getMarioAIOptions() {
        return marioAIOptions;
    }

    public static Environment getEnvironment() {
        return environment;
    }

    /**
     * Should be methods in controller and not in FF
     * ******************************************************************************
     */
    public boolean[] getAction(double[] networkOutput){
        if(networkOutput[0] < 0.5)
            actions[Mario.KEY_LEFT] = false;
        else
            actions[Mario.KEY_LEFT] = true;

        if(networkOutput[1] < 0.5)
            actions[Mario.KEY_RIGHT] = false;
        else
            actions[Mario.KEY_RIGHT] = true;

        if(networkOutput[2] < 0.5)
            actions[Mario.KEY_DOWN] = false;
        else
            actions[Mario.KEY_DOWN] = true;

        if(networkOutput[3] < 0.5)
            actions[Mario.KEY_UP] = false;
        else
            actions[Mario.KEY_UP] = true;

        if(networkOutput[4] < 0.5)
            actions[Mario.KEY_JUMP] = false;
        else
            actions[Mario.KEY_JUMP] = true;

        if(networkOutput[5] < 0.5)
            actions[Mario.KEY_SPEED] = false;
        else
            actions[Mario.KEY_SPEED] = true;

        if ( networkOutput[0] > networkOutput[1] && networkOutput[0] > networkOutput[2] ){
            actions[Mario.KEY_LEFT] = true;
            actions[Mario.KEY_RIGHT] = false;
            actions[Mario.KEY_DOWN] = false;
        } else if ( networkOutput[1] > networkOutput[0] && networkOutput[1] > networkOutput[2] ){
            actions[Mario.KEY_LEFT] = false;
            actions[Mario.KEY_RIGHT] = true;
            actions[Mario.KEY_DOWN] = false;
        } else if ( networkOutput[2] > networkOutput[1] && networkOutput[2] > networkOutput[0] ){
            actions[Mario.KEY_LEFT] = false;
            actions[Mario.KEY_RIGHT] = false;
            actions[Mario.KEY_DOWN] = true;
        }

        return actions;
    }

    public void makeTick(){
        environment.tick();
        agent.integrateObservation(environment);
    }

    public void resetActions(){
        actions[Mario.KEY_LEFT] = false;
        actions[Mario.KEY_RIGHT] = false;
        actions[Mario.KEY_DOWN] = false;
        actions[Mario.KEY_UP] = false;
        actions[Mario.KEY_JUMP] = false;
        actions[Mario.KEY_SPEED] = false;
    }

    @Override
    public int getMaxFitnessValue() {
        return 10;
    }
}
