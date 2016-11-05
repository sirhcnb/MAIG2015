package bachelor.Interactive;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Created by Pierre on 30-10-2016.
 */
public class UserInterface extends Application {

    private Pane root;
    private UserTrainer UT;
    private Button[] gifButtons;
    private Image[] gifs;
    private boolean[] chosenGifs;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Start our JavaFX user interface.
     * @param primaryStage Top level container of our window.
     */
    @Override
    public void start(Stage primaryStage) {
        //    TODO: Make something fancy so that it doesnt give a 'Not Responding' in the breed window while the gifs are being made!
        try {
            UT = new UserTrainer();
            initialize();
            initializeButtons();
            trainWithInteraction();
        } catch (Throwable th) {
            System.out.println(th);
        }

        primaryStage.setScene(new Scene(root, 1500, 770));
        primaryStage.setResizable(false);
        primaryStage.setTitle("Interactive Mario Trainer");
        primaryStage.show();
    }

    /**
     * Set up the basic UI that the user is to interact with.
     */
    public void initialize(){
        //Root pane of our scene (whole screen)
        root = new Pane();

        //Initialize and add gridpane to our root pane
        GridPane gp = new GridPane();
        root.getChildren().add(0, gp);

        //Initialize array for gifs and buttons, and if buttons are chosen or not
        gifs = new Image[UT.getFf().populationSize];
        chosenGifs = new boolean[UT.getFf().populationSize];
        gifButtons = new Button[UT.getFf().populationSize];

        //TODO: Delete!! Test button to see if breeding and UI behaves as expected!
        Button test = new Button();
        test.setOnAction(new EventHandler<ActionEvent>() {
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
        test.setLayoutX(1200);
        test.setLayoutY(200);
        root.getChildren().add(test);
    }

    /**
     * Set up button listeners and actions to be performed
     */
    public void initializeButtons() {
        int columnIndex, rowIndex = 0;
        int populationSize = UT.getFf().populationSize;

        //Initialize buttons to contain nothing. Gifs will be loaded into these later.
        for(int i = 0; i < gifButtons.length; i++)
        {
            gifButtons[i] = new Button("");
        }

        for(int i = 0; i < populationSize; i++) {
            //Change border of button to indicate the specific gif has been chosen
            int chosen = i;
            gifButtons[i].addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if(chosenGifs[chosen] == false) {
                        gifButtons[chosen].setStyle("-fx-border-color: blue");
                        chosenGifs[chosen] = true;
                        System.out.println("Chosen: " + chosen);
                    } else if(chosenGifs[chosen] == true) {
                        gifButtons[chosen].setStyle("-fx-border-color: transparent");
                        chosenGifs[chosen] = false;
                        System.out.println("Not chosen:" + chosen);
                    }
                }
            });

            //Load in gif again when hovering over specific button
            gifButtons[i].addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    int generation = UT.getFf().generation;
                    String gifLocation = "file:./db/gifs/interaction/" + Integer.toString(generation) + "/" + Integer.toString(chosen) + ".gif";
                    gifs[chosen] = new Image(gifLocation);
                    gifButtons[chosen].setGraphic(new ImageView(gifs[chosen]));
                }
            });

            //Get our Gridpane from our root pane
            GridPane gp = (GridPane) root.getChildren().get(0);

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

            //Set padding between grid objects
            gp.setHgap(5);
            gp.setVgap(5);
        }
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
                root.setDisable(true);

                UT.trainWithInteraction();
                return null;
            }
        };

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent e) {
                root.setDisable(false);

                setGifs(UT.getFf().generation-1, UT.getFf().populationSize);
            }
        });

        new Thread(task).start();
    }
}
