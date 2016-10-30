package bachelor.Interactive;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * Created by Pierre on 30-10-2016.
 */
public class UserInterface extends Application {
    //UI specific variables
    public Pane root;

    //Interactive specific variables
    public Image[] gifs;
    public boolean[] chosenGifs;
    public UserTrainer UT;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initialize();

        primaryStage.setScene(new Scene(root, 1500, 770));
        primaryStage.setResizable(false);
        primaryStage.setTitle("Interactive Mario Trainer");
        primaryStage.show();

        //    TODO: Make something fancy so that it doesnt give a 'Not Responding' in the breed window while the gifs are being made!
        try {
            UT = new UserTrainer();
            UT.trainWithInteraction();
            setGifs(0, 9);
        } catch (Throwable th) {
            System.out.println(th);
        }
    }

    public void initialize(){
        //Root pane of our scene (whole screen)
        root = new Pane();

        //Initialize and add gridpane to our root pane
        GridPane gp = new GridPane();
        root.getChildren().add(0, gp);

        /*Button test = new Button();
        test.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setGifs(1,9);
            }
        });
        test.setLayoutX(1200);
        test.setLayoutY(200);
        root.getChildren().add(test);*/
    }

    public void setGifs(int generation, int populationSize) {
        gifs = new Image[populationSize];
        chosenGifs = new boolean[populationSize];

        for(int i = 0; i < populationSize; i++) {
            String gifLocation = "file:./db/gifs/interaction/" + Integer.toString(generation) + "/" + Integer.toString(i) + ".gif";
            gifs[i] = new Image(gifLocation);
            chosenGifs[i] = false;
        }

        int columnIndex, rowIndex = 0;

        for(int i = 0; i < populationSize; i++) {
            //Button gifButton = new Button(Integer.toString(i+1), new ImageView(gifs[i]));
            Button gifButton = new Button("", new ImageView(gifs[i]));

            int chosen = i;
            gifButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if(chosenGifs[chosen] == false) {
                        gifButton.setStyle("-fx-border-color:blue");
                        chosenGifs[chosen] = true;
                    } else {
                        gifButton.setStyle("-fx-inner-border-color:transparent");
                        chosenGifs[chosen] = false;
                    }
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
            gp.add(gifButton, columnIndex, rowIndex);

            //Set padding between grid objects
            gp.setHgap(5);
            gp.setVgap(5);
        }
    }
}
