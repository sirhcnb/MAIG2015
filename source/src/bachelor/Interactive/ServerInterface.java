package bachelor.interactive;

import java.sql.*;

/**
 * Created by chris on 15-11-2016.
 */
public class ServerInterface extends InteractiveFilePersistence {

    // for testing the connection
    public static void main(String[] args) {
        Connection conn = null;

        try {
            String userName = "collmariouser";
            String password = "qwerty12345";

            String url = "jdbc:mysql://178.62.20.78:3306/collmario";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection(url, userName, password);
            System.out.println("Database connection established");
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

    public void uploadToDatabase() {

    }

    public void downloadFromDatabase() {

    }

}
