package bachelor.interactive;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jgap.Chromosome;

import java.io.File;
import java.util.*;

/**
 * Created by Pierre on 30-10-2016.
 */
public class UserInterface extends Application {
    private BorderPane root;
    private GridPane gp;
    private VBox cp;
    private VBox rp;

    //Left pane
    private Button[] gifButtons;
    private Image[] gifs;
    private boolean[] chosenGifs;
    private Label genLabel;
    private Label evalLabel;

    //Center pane
    private Button previewButton;
    private Image previewImage;

    //Right pane
    private ListView<HBox> leaderBoard;

    private UserTrainer UT;
    private ServerPersistence si;

    private int amountOfChosen = 0;

    private int leaderBoardAmount = 20;

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
            si = new ServerPersistence(UT, this);
            initialize(primaryStage);
        } catch (Throwable th) {
            System.out.println(th);
        }

        primaryStage.setScene(new Scene(root, 1700, 870));
        primaryStage.setResizable(false);
        primaryStage.setTitle("Interactive Mario Trainer");
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                event.consume();
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Warning");
                alert.setHeaderText("Are you sure you want to exit without uploading/saving your progress?");
                alert.setContentText("Select \"Cancel\" to return to the program and upload/save your progress.\n" +
                        "To upload to the server  select \"Server\" and then \"Upload\".\n" +
                        "If you do not currently have an internet connection, you can select \"File\", then \"Save\".\n" +
                        "You can resume your progress later by selecting \"File\", then \"Load\", and find the file you saved earlier.\n" +
                        "Thank you for helping!");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    Platform.exit();
                    System.exit(0);
                }
                else if (result.get() == ButtonType.CANCEL) {
                    alert.close();
                }
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
        cp = new VBox();

        //Add our VBox to our root pane in the center side
        root.setCenter(cp);

        //Initialize right pane
        rp = new VBox();

        //Add our VBox to our root pane in the right side
        root.setRight(rp);

        //Disable gridpane, center pane and right pane until an action has been taken.
        gp.setDisable(true);
        //cp.setDisable(true);
        rp.setDisable(true);

        //Initialize array for gifs and buttons
        gifs = new Image[UT.getFf().populationSize];
        chosenGifs = new boolean[UT.getFf().populationSize];
        gifButtons = new Button[UT.getFf().populationSize];

        //Initialize menu
        initializeMenu(primaryStage);

        //Initialize buttons, center
        initializeGifButtons();
        initializeCenter();
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

        //Initialize generation label
        genLabel = new Label("Generation: ");
        genLabel.setPadding(new Insets(0, 0, 0, 110));
        gp.add(genLabel, 0, 4);

        //Initialize breed button and add listener to do breed functionality
        Button breed = new Button("Breed");
        breed.setPrefHeight(75.0);
        breed.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    UT.breed(chosenGifs);
                    trainWithInteraction();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        breed.setLayoutX(0);
        breed.setLayoutY(0);
        breed.setMinWidth(320);
        gp.add(breed, 1, 4);
    }

    public void initializeCenter() {
        //Initialize leaderboard
        Label leaderBoardLabel = new Label("Leaderboard:");

        leaderBoard = new ListView<>();
        leaderBoard.setMinWidth(400);

        leaderBoard.setItems(si.importLeaderboard(leaderBoardAmount));

        cp.getChildren().add(leaderBoardLabel);
        cp.getChildren().add(leaderBoard);


        //Make a timer task to update leaderboard every minute
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        leaderBoard.setItems(si.importLeaderboard(leaderBoardAmount));
                        return null;
                    }
                };
            }
        }, 0, 60000);

        //Initialize More button for leaderboard
        Button moreButton = new Button("More...");
        moreButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                leaderBoardAmount += 20;
                leaderBoard.setItems(si.importLeaderboard(leaderBoardAmount));
            }
        });

        cp.getChildren().add(moreButton);


        //Initialize preview
        Label previewLabel = new Label("Preview:");
        previewLabel.setTranslateY(50);

        previewButton = new Button("");
        previewButton.setMinHeight(240);
        previewButton.setMinWidth(320);
        previewButton.setGraphic(new ImageView());
        previewButton.setTranslateY(50);

        previewButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String path = null;
                try {
                    path = UT.runSinglePreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if(path != null)
                {
                    setPreview(path);
                }
            }
        });

        cp.setAlignment(Pos.TOP_CENTER);

        cp.getChildren().add(previewLabel);
        cp.getChildren().add(previewButton);
    }

    public void initializeMenu(Stage primaryStage) {
        DirectoryChooser dirChooser = new DirectoryChooser();

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
                File file = dirChooser.showDialog(primaryStage);
                if(file != null && file.isDirectory()) {
                    try {
                        ArrayList<Chromosome> chroms = (ArrayList) UT.genotype.getChromosomes();
                        UT.saveChromosomes(chroms, file);
                        UT.makeGenForkFile(file);

                        InteractiveFilePersistence.copyFile("./db/run/runtestrun.xml", file.getAbsolutePath() + "/run.xml");
                    } catch (Exception e) {
                        e.printStackTrace();
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
                File file = dirChooser.showDialog(primaryStage);
                if(file != null && file.isDirectory()) {
                    try {
                        UT.loadChromosomes(file);
                        UT.setGenFork(file);
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
                            "Please select only one! The selected chromosome will be used as a preview" +
                            "of your upload.");

                    alert.showAndWait();
                } else if(amountOfChosen <= 0) {
                    alert.setTitle("Upload dialog");
                    alert.setHeaderText(null);
                    alert.setContentText("Please select a chromosome to be uploaded! " +
                            "The selected chromosome will be used as a preview of your upload.");

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

                    Chromosome chosenChrom = (Chromosome) UT.genotype.getChromosomes().get(uploadChromosome);
                    ArrayList<Chromosome> chroms = (ArrayList) UT.genotype.getChromosomes();

                    UploadInterface upload = new UploadInterface(si, UT.getCsv(), chroms, UT.getFf().generation, UT.getForkedFrom(), chosenChrom);

                    Stage stage = new Stage();
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
     * show new generation gifs and generation.
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

            //Show new generation in genLabel
            genLabel.setText("Generation: " + UT.getFf().generation);
        }
    }

    /**
     * After running preview evaluation, set preview button to show the preview gif.
     * @param gifLocation Path of our gif
     */
    public void setPreview(String gifLocation) {
        previewImage = null;
        previewImage = new Image(gifLocation);
        previewButton.setGraphic(new ImageView(previewImage));
    }

    /**
     * Run a preview evaluation and set preview button when done
     */
    public void runPreview() {
        Task<String> task = new Task<String>() {
            @Override
            public String call() throws Exception {
                String path;
                try {
                    path = UT.runSinglePreview();
                    return path;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                setPreview(task.getValue());
            }
        });

        new Thread(task).start();
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
