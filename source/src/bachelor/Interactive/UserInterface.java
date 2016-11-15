package bachelor.interactive;

import com.anji.integration.XmlPersistableChromosome;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jgap.Chromosome;

import javax.swing.*;
import java.io.File;

/**
 * Created by Pierre on 30-10-2016.
 */
public class UserInterface extends Application {
    private BorderPane root;
    private GridPane gp;
    private Pane cp;
    private Button[] gifButtons;
    private Image[] gifs;
    private boolean[] chosenGifs;

    private UserTrainer UT;

    private int amountOfChosen = 0;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start our JavaFX user interface.
     * @param primaryStage Top level container of our window.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            UT = new UserTrainer();
            initialize(primaryStage);
        } catch (Throwable th) {
            System.out.println(th);
        }

        primaryStage.setScene(new Scene(root, 1500, 770));
        primaryStage.setResizable(false);
        primaryStage.setTitle("Interactive Mario Trainer");
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                Platform.exit();
                System.exit(0);
            }
        });
        primaryStage.show();
    }

    /**
     * Set up the basic UI that the user is to interact with.
     */
    public void initialize(Stage primaryStage) {
        //Root pane of our scene (whole screen)
        root = new BorderPane();

        //Initialize our gridpane
        gp = new GridPane();

        //Set padding between grid objects
        gp.setHgap(5);
        gp.setVgap(5);

        //Add our gridpane to our root pane in the left side
        root.setLeft(gp);

        //Initialize center pane
        cp = new Pane();

        //Add our pane to our root pane in the center side
        root.setCenter(cp);

        //Disable gridpane and center pane until an action has been taken.
        gp.setDisable(true);
        cp.setDisable(true);

        //Initialize array for gifs and buttons
        gifs = new Image[UT.getFf().populationSize];
        chosenGifs = new boolean[UT.getFf().populationSize];
        gifButtons = new Button[UT.getFf().populationSize];

        //Initialize menu
        initializeMenu(primaryStage);

        //Initialize buttons
        initializeGifButtons();
        initializeBreedButton();
    }

    /**
     * Set up button listeners and actions to be performed
     */
    public void initializeGifButtons() {
        int columnIndex, rowIndex = 0;
        int populationSize = UT.getFf().populationSize;

        //Initialize buttons to contain nothing. Gifs will be loaded into these later.
        for(int i = 0; i < gifButtons.length; i++)
        {
            gifButtons[i] = new Button("");
            gifButtons[i].setMinHeight(240);
            gifButtons[i].setMinWidth(320);
        }

        for(int i = 0; i < populationSize; i++) {
            //Change border of button to indicate the specific gif has been chosen
            int chosen = i;
            gifButtons[i].addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if(chosenGifs[chosen] == false) {
                        gifButtons[chosen].setStyle("-fx-border-color: blue; -fx-border-width: 3px");
                        chosenGifs[chosen] = true;
                        amountOfChosen++;
                        System.out.println("Chosen: " + chosen);
                    } else if(chosenGifs[chosen] == true) {
                        gifButtons[chosen].setStyle("-fx-border-color: transparent");
                        chosenGifs[chosen] = false;
                        amountOfChosen--;
                        System.out.println("Not chosen:" + chosen);
                    }
                }
            });

            //Load in gif again when hovering over specific button
            gifButtons[i].addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    int generation = UT.getFf().generation-1;
                    String gifLocation = "file:./db/gifs/interaction/" + Integer.toString(generation) + "/" + Integer.toString(chosen) + ".gif";
                    gifs[chosen] = new Image(gifLocation);
                    gifButtons[chosen].setGraphic(new ImageView(gifs[chosen]));
                }
            });

            //Set column and row index in which the gifs are to be shown
            columnIndex = i%3;

            if(i%3 == 0){
                rowIndex++;
            }

            //Add gif button to the grid in position (column,row) (x,y)
            if(gp.getChildren().size() < UT.getFf().populationSize)
            {
                gp.add(gifButtons[i], columnIndex, rowIndex);
            }
        }
    }

    public void initializeBreedButton() {
        //Initialize breed button and add listener to do breed functionality
        Button breed = new Button("Breed");
        breed.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    UT.breed(chosenGifs, false);
                    trainWithInteraction();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        breed.setLayoutX(0);
        breed.setLayoutY(0);
        cp.getChildren().add(breed);
    }

    public void initializeMenu(Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();

        //Initialize menu bar
        MenuBar menuBar = new MenuBar();

        //Initialize menu for starting a new run
        Menu menuStart = new Menu("Start");
        menuBar.getMenus().add(menuStart);

        //New run menuItem
        MenuItem newRun = new MenuItem("New run");
        newRun.setAccelerator(KeyCombination.keyCombination("Ctrl + N"));
        newRun.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    UT.newRun();
                    trainWithInteraction();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        //Add menu items to the menu bar menu
        menuStart.getItems().add(newRun);


        //Initialize menu for save and load, and add it to the menu bar
        Menu menuFile = new Menu("File");
        menuBar.getMenus().add(menuFile);

        //Save menuItem
        MenuItem save = new MenuItem("Save");
        save.setAccelerator(KeyCombination.keyCombination("Ctrl + S"));
        save.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);

                if(amountOfChosen > 1) {
                    alert.setTitle("Save dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("You have selected several chromosomes to save. " +
                            "Please select only one!");

                    alert.showAndWait();
                } else if(amountOfChosen <= 0) {
                    alert.setTitle("Save dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("Please select one chromosome to be saved!");

                    alert.showAndWait();
                } else {
                    File file = fileChooser.showSaveDialog(primaryStage);
                    if(file != null) {
                        try {
                            int saveChromosome = 0;
                            for(int i = 0; i < chosenGifs.length; i++) {
                                if(chosenGifs[i] == true)
                                {
                                    saveChromosome = i;
                                    break;
                                }
                            }

                            Chromosome chrom = (Chromosome) UT.genotype.getChromosomes().get(saveChromosome);

                            UT.saveChromosome(chrom, file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        //Load menuItem
        MenuItem load = new MenuItem("Load");
        load.setAccelerator(KeyCombination.keyCombination("Ctrl + L"));
        load.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                File file = fileChooser.showOpenDialog(primaryStage);
                if(file != null) {
                    try {
                        UT.loadChromosome(file);
                        UT.setGeneration(file);
                        UT.breed(chosenGifs, true);
                        trainWithInteraction();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        //Add menu items to the menu bar menu
        menuFile.getItems().add(save);
        menuFile.getItems().add(load);


        //Initialize menu for server functionality
        Menu menuServer = new Menu("Server");
        menuBar.getMenus().add(menuServer);

        //New upload menuItem
        MenuItem upload = new MenuItem("Upload");
        upload.setAccelerator(KeyCombination.keyCombination("Ctrl + U"));
        upload.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);

                if(amountOfChosen > 1) {
                    alert.setTitle("Upload dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("You have selected several chromosomes to upload. " +
                            "Please select only one!");

                    alert.showAndWait();
                } else if(amountOfChosen <= 0) {
                    alert.setTitle("Upload dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("Please select one chromosome to be uploaded!");

                    alert.showAndWait();
                } else {
                    int uploadChromosome = 0;
                    for(int i = 0; i < chosenGifs.length; i++) {
                        if(chosenGifs[i] == true)
                        {
                            uploadChromosome = i;
                            break;
                        }
                    }

                    Chromosome chrom = (Chromosome) UT.genotype.getChromosomes().get(uploadChromosome);

                    Stage stage = new Stage();
                    UploadInterface upload = new UploadInterface(UT.getSi(), chrom, UT.getFf().generation);
                    upload.start(stage);
                }
            }
        });

        //Add menu items to the menu bar menu
        menuServer.getItems().add(upload);

        //Add menu bar to our root pane
        root.setTop(menuBar);
    }

    /**
     * After breeding has been done and new evaluations has been run, set buttons to
     * show new generation gifs.
     * @param generation Current generation, in which to show gifs of.
     * @param populationSize How many gifs are there to be set.
     */
    public void setGifs(int generation, int populationSize) {
        for(int i = 0; i < populationSize; i++) {
            String gifLocation = "file:./db/gifs/interaction/" + Integer.toString(generation) + "/" + Integer.toString(i) + ".gif";
            gifs[i] = null;
            gifs[i] = new Image(gifLocation);
            chosenGifs[i] = false;
            amountOfChosen = 0;
            gifButtons[i].setGraphic(new ImageView(gifs[i]));
            gifButtons[i].setStyle("-fx-border-color: transparent");
        }
    }

    /**
     * Start by breeding and then set gifs to show new generation.
     * Uses Task to ensure that the UI thread isn't being blocked.
     * @throws Exception In case gif sequence writer fails.
     */
    public void trainWithInteraction() {
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                root.getTop().setDisable(true);
                root.getLeft().setDisable(true);
                root.getCenter().setDisable(true);

                UT.trainWithInteraction();
                return null;
            }
        };

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                root.getTop().setDisable(false);
                root.getLeft().setDisable(false);
                root.getCenter().setDisable(false);

                setGifs(UT.getFf().generation-1, UT.getFf().populationSize);
            }
        });

        new Thread(task).start();
    }
}
