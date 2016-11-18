package bachelor.interactive;

import com.anji.integration.XmlPersistableChromosome;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import own.FilePersistenceMario;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * Created by Pierre on 14-11-2016.
 */
public class InteractiveFilePersistence extends FilePersistenceMario {
    /**
     * Load in the chromosome and evolve to reproduce and populate according to this chromosome
     */
    public Chromosome loadChromosome(Configuration config, File file) throws Exception {
        //Load in the whole XML file as one string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line = br.readLine()) != null){
            sb.append(line.trim());
        }

        //Create chromosome from XML string
        return chromosomeFromXml(config, sb.toString());
    }

    /**
     * Load in the chosen chromosome from the server
     * @param config Config file to make the chromosome object
     * @param xmlFormat The xml format of our received chromosome
     * @return The chromosome object
     */
    public Chromosome loadChromosomeServer(Configuration config, String xmlFormat) throws Exception {
        return chromosomeFromXml(config, xmlFormat);
    }

    /**
     * Save the chromosome in the specified path
     */
    public void saveChromosome(Chromosome c, File file, int generation, int forkedFrom) throws Exception {
        storeChromosome(c, file.getAbsolutePath(), generation, forkedFrom);
    }

    /**
     * Sets the generation of the chromosome in the xml file and stores the xml file
     */
    public void storeChromosome(Chromosome c, String path, int generation, int forkedFrom) throws Exception {
        //Load chromosome into a DOM parser
        XmlPersistableChromosome xp = new XmlPersistableChromosome(c);

        ByteArrayInputStream in = new ByteArrayInputStream(xp.toXml().getBytes());
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);

        //Append a generation tag to our xml file before saving
        Element gen = doc.createElement("generation");
        gen.setAttribute("id", Integer.toString(generation));
        doc.getDocumentElement().appendChild(gen);

        //Append a forked from tag to our xml file before saving
        Element fork = doc.createElement("forkedFrom");
        fork.setAttribute("id", Integer.toString(forkedFrom));
        doc.getDocumentElement().appendChild(fork);

        //Transform the Document object into a string
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        String xml = sw.toString();

        //Save the chromosome string format into the xml file specified
        FileOutputStream out = null;
        try {
            out = new FileOutputStream( path + ".xml" );
            out.write(xml.getBytes());
            out.close();
            counter++;
        }
        finally {
            if ( out != null )
                out.close();
        }
    }

    /**
     * Get generation and forkedFrom from xml file if it contains a generation, else return 0.
     */
    public GenFork getGenAndForkFromChromosome(File file) throws Exception {
        //Load in the whole XML file as one string
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line = br.readLine()) != null){
            sb.append(line.trim());
        }

        String xml = sb.toString();

        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);

        //Make new GenFork object to contain generation and forkedFrom
        GenFork genFork = new GenFork();

        //Check if contains generation tag else start from generation 0.
        NodeList generations = doc.getDocumentElement().getElementsByTagName("generation");

        if(generations.getLength() != 0) {
            String generation = doc.getDocumentElement().getElementsByTagName("generation").item(0).getAttributes().getNamedItem("id").getNodeValue();
            genFork.setGeneration(Integer.parseInt(generation));
        }

        //Check if contains forkedFrom tag else set forkedFrom to 0.
        NodeList forksFrom = doc.getDocumentElement().getElementsByTagName("forkedFrom");

        if(forksFrom.getLength() != 0) {
            String forkedFrom = doc.getDocumentElement().getElementsByTagName("forkedFrom").item(0).getAttributes().getNamedItem("id").getNodeValue();
            genFork.setForkedFrom(Integer.parseInt(forkedFrom));
        }

        return genFork;
    }
}
