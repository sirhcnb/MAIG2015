package bachelor.interactive;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.sql.*;

/**
 * Created by chris on 15-11-2016.
 */
public class ServerInterface extends InteractiveFilePersistence {
    Connection conn = null;
    String sqlUserName = "collmariouser";
    String password = "qwerty12345";
    String url = "jdbc:mysql://178.62.20.78:3306/collmario";

    // for testing the connection
    public static void main(String[] args) {
        Connection conn = null;

        try {
            String sqlUserName = "collmariouser";
            String password = "qwerty12345";

            String url = "jdbc:mysql://178.62.20.78:3306/collmario";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);
            System.out.println("Database connection established");

            ServerInterface si = new ServerInterface();
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
    }

    public void uploadToDatabase(String chrom, String username, String comment, int gen, int fitness, String genfit) {
        String query = "INSERT INTO cmario ("
                + " chrom,"
                + " username,"
                + " comment,"
                + " gen,"
                + " id,"
                + " fitness,"
                + " genfit ) VALUES ("
                + "?, ?, ?, ?, null, ?, ?)";
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            //Statement statement = conn.createStatement();
            //statement.executeUpdate("INSERT INTO cmario " + chrom + username + comment + gen + fitness + genfit);

            // set all the preparedstatement parameters
            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, chrom);
            st.setString(2, username);
            st.setString(3, comment);
            st.setInt(4, gen);
            st.setInt(5, fitness);
            st.setString(6, genfit);

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

            //Statement statement = conn.createStatement();
            //statement.executeUpdate("INSERT INTO cmario " + chrom + username + comment + gen + fitness + genfit);

            String query = "SELECT chrom FROM cmario WHERE ID=" + id;

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next())
            {
                //int cid = rs.getInt("id");
                //String firstName = rs.getString("first_name");
                //String lastName = rs.getString("last_name");
                //Date dateCreated = rs.getDate("date_created");
                //boolean isAdmin = rs.getBoolean("is_admin");
                //int numPoints = rs.getInt("num_points");

                String chrom = rs.getString("chrom");
                System.out.println(chrom);

                // print the results
                //System.out.format("%s, %s, %s, %s, %s, %s\n", cid, firstName, lastName, dateCreated, isAdmin, numPoints);
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
    }

    public ObservableList<HBox> importLeaderboard() {
        //Observablelist  -- FXCollections.observableArrayList
        //id, username, comment, gen, fit, gif
        //query -> hbox -> add til obsList rinse & repeat
        ObservableList<HBox> hBoxObservableList = FXCollections.observableArrayList();

        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, sqlUserName, password);

            System.out.println("Database connection established");

            String query = "SELECT * FROM cmario ORDER BY fitness DESC LIMIT 10";

            // create the java statement
            Statement st = conn.createStatement();

            // execute the query, and get a java resultset
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next())
            {
                int i = 1;
                int cid = rs.getInt("id");
                String username = rs.getString("username");
                int gen = rs.getInt("gen");
                int fitness = rs.getInt("fitness");
                String comment = rs.getString("comment");
                //Byte[] numPoints = rs.getBytes("gif");

                // print the results
                System.out.format("%s, %s, %s, %s, %s\n", cid, username, gen, fitness, comment);

                HBox hBox = new HBox();
                hBox.setSpacing(20.0);
                hBox.getChildren().addAll(new Label("Rank: " + Integer.toString(i)), new Label("Username: " + username), new Label("Generation: " + Integer.toString(gen)), new Label("Fitness: " + Integer.toString(fitness)), new Label("Comment: " + comment), new Button("Preview"), new Button("Import"));
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
