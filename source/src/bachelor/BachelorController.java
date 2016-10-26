package bachelor;

import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * Created by Pierre on 26-10-2016.
 */
public class BachelorController {

    /**
     * Taken from BasicMarioAIAgent!!
     */
    protected boolean action[] = new boolean[Environment.numberOfKeys];
    protected String name = "Instance_of_BasicAIAgent._Change_this_name";

    protected byte[][] levelScene;
    protected byte[][] enemies;
    protected byte[][] mergedObservation;

    protected float[] marioFloatPos = null;
    protected float[] enemiesFloatPos = null;

    protected int[] marioState = null;

    protected int marioStatus;
    protected int marioMode;
    protected boolean isMarioOnGround;
    protected boolean isMarioAbleToJump;
    protected boolean isMarioAbleToShoot;
    protected boolean isMarioCarrying;
    protected int getKillsTotal;
    protected int getKillsByFire;
    protected int getKillsByStomp;
    protected int getKillsByShell;

    protected int receptiveFieldWidth;
    protected int receptiveFieldHeight;
    protected int marioEgoRow;
    protected int marioEgoCol;


    // ---- MARIO AI REPRESENTATION ---- //
    private Environment environment;
    private boolean[] actions;

    //http://www.marioai.org/gameplay-track/marioai-benchmark	for zlevels:
    int zLevelScene = 0;
    int zLevelEnemies = 0;

    public BachelorController() {
        this.actions = new boolean[Environment.numberOfKeys];
    }

    public void reset(){
        actions[Mario.KEY_LEFT] = false;
        actions[Mario.KEY_RIGHT] = false;
        actions[Mario.KEY_DOWN] = false;
        actions[Mario.KEY_UP] = false;
        actions[Mario.KEY_JUMP] = false;
        actions[Mario.KEY_SPEED] = false;
    }

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

    public void integrateObservation(Environment environment){
        // Taken from SimpleMLPAgent.java
        this.environment = environment;
        levelScene = environment.getLevelSceneObservationZ(zLevelScene);
        enemies = environment.getEnemiesObservationZ(zLevelEnemies);
        mergedObservation = environment.getMergedObservationZZ(zLevelScene, zLevelEnemies);

        this.marioFloatPos = environment.getMarioFloatPos();
        this.enemiesFloatPos = environment.getEnemiesFloatPos();
        this.marioState = environment.getMarioState();

        marioStatus = marioState[0];
        marioMode = marioState[1];
        isMarioOnGround = marioState[2] == 1;
        isMarioAbleToJump = marioState[3] == 1;
        isMarioAbleToShoot = marioState[4] == 1;
        isMarioCarrying = marioState[5] == 1;
        getKillsTotal = marioState[6];
        getKillsByFire = marioState[7];
        getKillsByStomp = marioState[8];
        getKillsByShell = marioState[9];
    }

    public void makeTick(){
        environment.tick();
        integrateObservation(environment);
    }

    public void performAction(boolean[] actions){
        environment.performAction(actions);
    }
}
