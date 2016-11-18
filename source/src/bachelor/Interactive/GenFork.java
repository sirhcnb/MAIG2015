package bachelor.interactive;

/**
 * Created by Pierre on 18-11-2016.
 */
public class GenFork {
    private int generation;
    private int forkedFrom;

    public GenFork() {
        generation = 0;
        forkedFrom = 0;
    }

    public int getGeneration() {
        return generation;
    }

    public int getForkedFrom() {
        return forkedFrom;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public void setForkedFrom(int forkedFrom) {
        this.forkedFrom = forkedFrom;
    }
}
