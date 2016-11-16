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
public class CsvFormat {
    private final static String delimiter = ";";
    private final static String lineSeperator = "\n";
    private final static String fileHeader = "id, fitness";

    private StringBuilder finalString;

    public CsvFormat() {
        Path csvPath = Paths.get((System.getProperty("user.home") + "/Documents/csv/"));

        if(!Files.exists(csvPath)) {
            new File((System.getProperty("user.home") + "/Documents/csv/")).mkdirs();
        }

        finalString = new StringBuilder();
    }

    public void generateCsvFile(long chromID)
    {
        try
        {
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

    public void writeSingleToString(int bestFitness, int generation) {
        if(finalString.length() == 0) {
            finalString.append(fileHeader);
        }

        finalString.append(lineSeperator);
        finalString.append(Integer.toString(generation));
        finalString.append(delimiter);
        finalString.append(Integer.toString(bestFitness));
    }

    public void loadCSVFromChromosome(long chromID) throws IOException {
        finalString = new StringBuilder();
        File file = new File(System.getProperty("user.home") + "/Documents/csv/" + Long.toString(chromID) + ".csv");

        if(file != null)
        {
            String wholeXml = new String(Files.readAllBytes(file.toPath()));
            finalString.append(wholeXml);
        }
    }

    public void loadCSVFromChromosomeServer(String csvFormat) {
        finalString = new StringBuilder();
        finalString.append(csvFormat);

        //DEBUG!!
        //System.out.println(finalString.toString());
    }

    public StringBuilder getFinalString() {
        return finalString;
    }
}
