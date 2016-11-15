package bachelor;

import org.jgap.Chromosome;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pierre on 15-11-2016.
 */
public class csvFormat {
    private final static String delimiter = ";";
    private final static String lineSeperator = "\n";
    private final static String fileHeader = "id, fitness";

    private int oldGeneration;

    private StringBuilder finalString;

    public csvFormat(int oldGeneration) {
        Path csvPath = Paths.get((System.getProperty("user.home") + "/Documents/csv/"));

        if(!Files.exists(csvPath)) {
            new File((System.getProperty("user.home") + "/Documents/csv/")).mkdirs();
        }

        finalString = new StringBuilder();

        this.oldGeneration = oldGeneration;
        if(oldGeneration != 0)
        {
            oldGeneration++;
        }
    }

    public void generateCsvFile(long chromID, ArrayList<Integer> bestChroms)
    {
        try
        {
            writeToString(bestChroms);

            FileWriter writer = new FileWriter(System.getProperty("user.home") + "/Documents/csv/" + Long.toString(chromID) + ".csv");

            writer.append(finalString);

            writer.flush();
            writer.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void writeToString(ArrayList<Integer> bestChroms) {
        if(finalString.length() == 0) {
            finalString.append(fileHeader);
            finalString.append(lineSeperator);
        }

        for(int i = 0; i < bestChroms.size(); i++) {
            finalString.append(Integer.toString(oldGeneration + i));
            finalString.append(delimiter);
            finalString.append(Integer.toString(bestChroms.get(i).intValue()));
            finalString.append(lineSeperator);
        }
    }

    public void loadCSVFromChromosome(long chromID) throws IOException {
        File file = new File(System.getProperty("user.home") + "/Documents/csv/" + Long.toString(chromID) + ".csv");

        if(file != null)
        {
            String wholeXml = new String(Files.readAllBytes(file.toPath()));
            finalString.append(wholeXml);
            finalString.append(lineSeperator);
        }
    }

    public void setOldGeneration(int oldGeneration) {
        this.oldGeneration = oldGeneration;
    }

    public StringBuilder getFinalString() {
        return finalString;
    }
}
