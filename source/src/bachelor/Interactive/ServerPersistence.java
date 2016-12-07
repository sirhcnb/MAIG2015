package bachelor.interactive;

import com.anji.integration.XmlPersistableChromosome;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.jgap.Chromosome;
import org.jgap.Configuration;

import java.sql.*;
import java.util.ArrayList;

/**
 * Created by chris on 15-11-2016.
 */
public class ServerPersistence extends InteractiveFilePersistence {
    private UserTrainer UT;
    private UserInterface UI;

    Connection conn = null;
    String sqlUserName = "collmariouser";
    String password = "qwerty12345";
    String url = "jdbc:mysql://178.62.20.78:3306/bachelormario";

    public ServerPersistence(UserTrainer UT, UserInterface UI) {
        this.UT = UT;
        this.UI = UI;
    }

    public UserTrainer getUT() {
        return UT;
    }

    // for testing the connection
    /*public static void main(String[] args) {
        Connection conn = null;

        try {
            String sqlUserName = "collmariouser";
            String password = "qwerty12345";

            String url = "jdbc:mysql://178.62.20.78:3306/collmario";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);
            System.out.println("Database connection established");

            ServerPersistence si = new ServerPersistence();
            //si.importFromDatabase(1);
            si.importLeaderboard();

        } catch (Exception e) {
            System.err.println("Cannot connect to database server");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database Connection Terminated");
                } catch (Exception e) {}
            }
        }
    }*/

    public void uploadToDatabase(ArrayList<Chromosome> chrom, String username, String comment, int gen, int fitness, String genfit, int forkedFrom, String runFile, Chromosome chosenChrom) {
        String query = "INSERT INTO collmario ("
                + " id,"
                + " username,"
                + " comment,"
                + " genfit,"
                + " gen,"
                + " fitness,"
                + " forkedFrom,"
                + " chrom1,"
                + " chrom2,"
                + " chrom3,"
                + " chrom4,"
                + " chrom5,"
                + " chrom6,"
                + " chrom7,"
                + " chrom8,"
                + " chrom9,"
                + " runFile,"
                + " prevChrom ) VALUES ("
                + " null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            int counter = 7;

            // set all the preparedstatement parameters
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, username);
            st.setString(2, comment);
            st.setString(3, genfit);
            st.setInt(4, gen);
            st.setInt(5, fitness);
            st.setInt(6, forkedFrom);

            // save chosenChrom as the first chromosome. We will preview this chromosome
            for(int i = 0; i < chrom.size(); i++) {
                System.out.println(chrom.get(i).getId());

                String xmlString = new XmlPersistableChromosome(chrom.get(i)).toXml();
                st.setString(counter, xmlString);

                System.out.println("****************************************");
                System.out.println(xmlString);
                System.out.println("****************************************");
                counter++;
            }

            st.setString(counter, runFile);

            counter++;
            String prevChromString = new XmlPersistableChromosome(chosenChrom).toXml();
            st.setString(counter, prevChromString);

            // execute the preparedstatement insert
            st.executeUpdate();
            st.close();

        }
        catch (Exception e) {
            System.err.println("Cannot connect to database server");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database Connection Terminated");
                } catch (Exception e) {}
            }
        }
    }

    public void importFromDatabase(int id) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            String query = "SELECT chrom1, chrom2, chrom3, chrom4, chrom5, chrom6, chrom7, chrom8, chrom9, " +
                    "genfit, gen, runFile FROM collmario WHERE ID=" + id;

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            //Move to next result
            rs.next();

            // show  resultset
            ArrayList<String> chroms = new ArrayList<>();

            for(int i = 1; i < 10; i++) {
                chroms.add(rs.getString("chrom" + Integer.toString(i)));
            }

            String genfit = rs.getString("genfit");
            int gen = rs.getInt("gen");


            //TODO: load into appropriate places (Done, need confirmation!)
            UT.loadChromosomesServer(chroms);
            UT.setGenerationForkServer(gen, id); //TODO: Load ID into fork ID into appropriate place (0)
            UT.getCsv().loadCSVFromChromosomeServer(genfit);

            // print the results
            System.out.format("%s, %s, %s\n", chroms, genfit, gen);

            st.close();

        }
        catch (Exception e) {
            System.err.println("Cannot connect to database server");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database Connection Terminated");

                    //Start evaluation of the received chromosome
                    boolean[] array = new boolean[0]; //Empty boolean array, for breeding only on loaded chromosome
                    UT.breed(array);
                    UI.trainWithInteraction();
                } catch (Exception e) {}
            }
        }
    }

    public void importPreviewChrom(int id) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            String query = "SELECT prevChrom FROM collmario WHERE ID=" + id;

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            //Move to next result
            rs.next();

            // show  resultset
            String chrom = rs.getString("prevChrom");

            //Load into UserTrainer
            UT.loadPreviewChromosome(id, chrom);

            System.out.println(UT.getPreviewChrom().getId());

            // print the results
            System.out.format("%s\n", chrom);

            st.close();

        }
        catch (Exception e) {
            System.err.println("Cannot connect to database server");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database Connection Terminated");
                } catch (Exception e) {}
            }
        }
    }

    public ObservableList<HBox> importLeaderboard(int amount) {
        ObservableList<HBox> hBoxObservableList = FXCollections.observableArrayList();

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            String query = "SELECT * FROM collmario ORDER BY fitness DESC LIMIT " + amount;

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            int i = 1;
            while (rs.next())
            {
                int cid = rs.getInt("id");
                String username = rs.getString("username");
                int gen = rs.getInt("gen");
                int fitness = rs.getInt("fitness");
                String comment = rs.getString("comment");

                // print the results
                System.out.format("%s, %s, %s, %s, %s\n", cid, username, gen, fitness, comment);

                HBox hBox = new HBox();
                hBox.setSpacing(20.0);

                //Limit content of the label to show only a part of the comment (30 characters)
                Label commentLabel = new Label();
                commentLabel.textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if(commentLabel.getText().length() > 30) {
                            String commentPart = commentLabel.getText().substring(0, 30);
                            commentLabel.setText(commentPart + "...");
                        }
                    }
                });
                commentLabel.setText("Comment: " + comment);

                //On hover will show a tooltip containing the whole comment
                commentLabel.setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        Tooltip commentTip = new Tooltip();
                        commentTip.setText(comment);
                        commentLabel.setTooltip(commentTip);
                    }
                });

                //Listener on preview button
                Button previewButton = new Button("Preview");
                previewButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        importPreviewChrom(Integer.parseInt(hBox.getId()));

                        UI.runPreview();
                    }
                });

                //Listener on import button to get everything necessary from the database
                Button impButton = new Button("Import");
                impButton.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        importFromDatabase(Integer.parseInt(hBox.getId()));
                    }
                });

                hBox.setId(Integer.toString(cid)); //Has been added. ID of the hbox equals ID from database entry
                hBox.getChildren().addAll(new Label("Rank: " + Integer.toString(i)), new Label("Username: " + username), new Label("Generation: " + Integer.toString(gen)), new Label("Fitness: " + Integer.toString(fitness)), commentLabel, previewButton, impButton);
                hBoxObservableList.add(hBox);
                i++;
            }
            st.close();

        }
        catch (Exception e) {
            System.err.println("Cannot connect to database server");
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                    System.out.println("Database Connection Terminated");
                } catch (Exception e) {}
            }
        }
        return hBoxObservableList;
    }
}
