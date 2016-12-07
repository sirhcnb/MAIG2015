package bachelor.interactive;

import bachelor.CsvFormat;
import com.anji.integration.XmlPersistableChromosome;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.jgap.Chromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.event.GeneticEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Pierre on 15-11-2016.
 */
public class UploadInterface extends Application {
    //Our top most container
    private Stage stage;

    //Root pane for handling UI
    private GridPane root;

    //Text fields to be uploaded
    private TextField userNameText;
    private TextField commentText;

    //Chromosome, generation and forkedFrom to be uploaded
    private ServerPersistence si;
    private CsvFormat csv;
    private ArrayList<Chromosome> uploadChroms;
    private Chromosome chosenChrom;
    private int generation;
    private int forkedFrom;

    public UploadInterface(ServerPersistence si, CsvFormat csv, ArrayList<Chromosome> uploadChroms, int generation, int forkedFrom, Chromosome chosenChrom) {
        this.si = si;
        this.csv = csv;
        this.uploadChroms = uploadChroms;
        this.generation = generation;
        this.forkedFrom = forkedFrom;
        this.chosenChrom = chosenChrom;
    }

    @Override
    public void start(Stage primaryStage) {
        initialize();
        initializeUploadButton();

        stage = primaryStage;
        stage.setScene(new Scene(root, 265, 110));
        stage.setResizable(false);
        stage.setTitle("Upload chromosome");
        stage.setAlwaysOnTop(true);
        stage.show();
    }

    public void initialize() {
        root = new GridPane();

        root.setVgap(5);
        root.setHgap(5);
        root.setPadding(new Insets(5));

        Label userName = new Label("Username:");
        Label comment = new Label("Comment:");

        userNameText = new TextField();
        userNameText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(userNameText.getText().length() >= 15) {
                    String textString = userNameText.getText().substring(0, 15);
                    userNameText.setText(textString);
                }
            }
        });

        commentText = new TextField();
        commentText.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(commentText.getText().length() >= 255) {
                    String textString = commentText.getText().substring(0, 255);
                    commentText.setText(textString);
                }
            }
        });

        root.add(userName, 0, 0);
        root.add(comment, 0, 1);
        root.add(userNameText, 1, 0);
        root.add(commentText, 1, 1);
    }

    public void initializeUploadButton() {
        Button uploadButton = new Button("Upload");
        root.setHalignment(uploadButton, HPos.RIGHT);

        uploadButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);

                if(userNameText.getText().length() == 0 || commentText.getText().length() == 0) {
                    alert.setTitle("Error uploading!");
                    alert.setHeaderText(null);
                    alert.setContentText("Please provide a username and a comment.");

                    alert.showAndWait();
                } else {
                    /*String xmlString = new XmlPersistableChromosome(uploadChrom).toXml();

                    System.out.println("Xml string: " + xmlString + '\n');
                    System.out.println("Generation: " + generation + '\n');
                    System.out.println("Username: " + userNameText.getText());
                    System.out.println("Comment: " + commentText.getText());
                    System.out.println("Gen-Fit: " + csv.getFinalString().toString());
                    System.out.println("forkedFrom: " + forkedFrom);*/


                    StringBuilder sb = new StringBuilder();
                    try {
                        //Updates the run file with the newest information
                        UserTrainer.getConfig().lockSettings();
                        UserTrainer.getConfig().getEventManager().fireGeneticEvent(
                                new GeneticEvent( GeneticEvent.GENOTYPE_EVALUATED_EVENT, si.getUT().genotype ) );

                        BufferedReader br = new BufferedReader(new FileReader(new File("./db/run/runtestrun.xml")));
                        String line;

                        while((line = br.readLine()) != null){
                            sb.append(line.trim());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    System.out.println(sb.toString());

                    //TODO: access gif and genfit and add as parameters
                    si.uploadToDatabase(uploadChroms, userNameText.getText(), commentText.getText(), generation, chosenChrom.getFitnessValue(), csv.getFinalString().toString(), forkedFrom, sb.toString(), chosenChrom);

                }

                stage.close();
            }
        });

        root.add(uploadButton, 1, 3);
    }
}
