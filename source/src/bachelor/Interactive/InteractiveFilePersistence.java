package bachelor.interactive;

import com.anji.integration.XmlPersistableChromosome;
import org.apache.commons.io.FileUtils;
import org.jfree.io.FileUtilities;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import own.FilePersistenceMario;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by Pierre on 14-11-2016.
 */
public class InteractiveFilePersistence extends FilePersistenceMario {
    /**
     * Load in the chromosomes and evolve to reproduce and populate according to these chromosomes
     */
    public void loadChromosomes(File file) throws Exception {
        /*for(int i = 0; i < file.listFiles().length; i++) {
            if((file.listFiles()[i].getAbsolutePath()).contains("chromosome")) {
                copyFile(file.listFiles()[i].getAbsolutePath(), "./db/chromosome/" + file.listFiles()[i].getName());
            }
        }

        copyFile(file.getAbsolutePath() + "/run.xml", "./db/run/runtestrun.xml");*/

        FileUtils.copyDirectory(new File(file.getAbsolutePath() + "/db"), new File("./db"));
        FileUtils.copyDirectory(new File(file.getAbsolutePath() + "/nevt"), new File("./nevt"));
    }

    public void loadChromosomesServer(Configuration config, ArrayList<String> chroms, String runFile) throws Exception {
        FileOutputStream out = null;

        for(int i = 0; i < chroms.size(); i++) {
            Chromosome chrom = chromosomeFromXml(config, chroms.get(i));

            //Save the chromosome string format into the xml file specified
            try {
                out = new FileOutputStream("./db/chromosome/chromosome" + chrom.getId() + ".xml" );
                out.write(chroms.get(i).getBytes());
                out.close();
            }
            finally {
                if ( out != null )
                    out.close();
            }
        }

        //Save the runFile string format into xml file specified
        try {
            out = new FileOutputStream("./db/run/runtestrun.xml" );
            out.write(runFile.getBytes());
            out.close();
        }
        finally {
            if ( out != null )
                out.close();
        }
    }

    /**
     * Load in the chosen chromosome from the server
     * @param config Config file to make the chromosome object
     * @param xmlFormat The xml format of our received chromosome
     * @return The chromosome object
     */
    public Chromosome loadPrevChromosomeServer(Configuration config, String xmlFormat) throws Exception {
        return chromosomeFromXml(config, xmlFormat);
    }

    /**
     * Save the chromosomes in the specified path
     */
    public void saveChromosomes(ArrayList<Chromosome> chroms, String path) throws Exception {
        /*for(int i = 0; i < chroms.size(); i++) {
            storeChromosome(chroms.get(i), path);
        }*/

        FileUtils.copyDirectory(new File("./db"), new File(path + "/db"));
        FileUtils.copyDirectory(new File("./nevt"), new File(path + "/nevt"));
    }

    /**
     * Sets the generation of the chromosome in the xml file and stores the xml file
     */
    public void storeChromosome(Chromosome c, String path) throws Exception {
        //Load chromosome into a DOM parser
        XmlPersistableChromosome xp = new XmlPersistableChromosome(c);

        //Make the xml document
        ByteArrayInputStream in = new ByteArrayInputStream(xp.toXml().getBytes());
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(in);

        //Transform the Document object into a string
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        String xml = sw.toString();

        //Save the chromosome string format into the xml file specified
        FileOutputStream out = null;
        try {
            out = new FileOutputStream( path + "/chromosome" + c.getId() + ".xml" );
            out.write(xml.getBytes());
            out.close();
        }
        finally {
            if ( out != null )
                out.close();
        }
    }

    /**
     * Make a genfork file for loading later
     * @param file directory to save to
     */
    public void makeGenForkFile(File file, int generation, int forkedFrom) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        //GenFork root tag
        Document doc = docBuilder.newDocument();
        Element genForkElement = doc.createElement("GenFork");
        doc.appendChild(genForkElement);

        //Generation tag
        Element gen = doc.createElement("generation");
        Attr genAttribute = doc.createAttribute("id");
        genAttribute.setValue(Integer.toString(generation));
        gen.setAttributeNode(genAttribute);
        genForkElement.appendChild(gen);

        //forkedFrom tag
        Element fork = doc.createElement("forkedFrom");
        Attr forkAttribute = doc.createAttribute("id");
        forkAttribute.setValue(Integer.toString(forkedFrom));
        fork.setAttributeNode(forkAttribute);
        genForkElement.appendChild(fork);

        //Transform the Document object into a string
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        String xml = sw.toString();

        //Save the genfork string format into the xml file specified
        FileOutputStream out = null;
        try {
            out = new FileOutputStream( file.getAbsolutePath() + "/genFork.xml" );
            out.write(xml.getBytes());
            out.close();
        }
        finally {
            if ( out != null )
                out.close();
        }
    }

    /**
     * Get generation and forkedFrom from xml file if it contains a generation, else return 0.
     */
    public GenFork getGenAndForkFromFile(File file) throws Exception {
        //Load in the whole XML file as one string
        BufferedReader br = new BufferedReader(new FileReader(file + "/genFork.xml"));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line = br.readLine()) != null){
            sb.append(line.trim());
        }

        String xml = sb.toString();
        br.close();

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

    /**
     * Copies source and puts in destination
     * @param source source to copy
     * @param dest destination to save to
     * @throws IOException
     */
    public static void copyFile(String source, String dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }
}
