import beans.StudentBean;

import java.sql.*;

public class StudentSQLite {
    public static void createTable() throws Exception {
        String url = "jdbc:sqlite:students.db";

        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite driver loaded");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS STUDENTS (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT,"+
                    "NUME TEXT," +
                    "PRENUME TEXT," +
                    "VARSTA INTEGER)");

            System.out.println("Tabel creat");
        }
    }

    public static boolean add(StudentBean student) throws Exception {
        String url = "jdbc:sqlite:students.db";

        String checkSql = "SELECT * FROM STUDENTS WHERE NUME = ? AND PRENUME = ?";
        String insertSql = "INSERT INTO STUDENTS (NUME, PRENUME, VARSTA) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, student.getNume());
            checkStmt.setString(2, student.getPrenume());

            ResultSet rs = checkStmt.executeQuery();

            // dacă NU există → inserăm
            if (!rs.next()) {

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, student.getNume());
                    insertStmt.setString(2, student.getPrenume());
                    insertStmt.setInt(3, student.getVarsta());

                    insertStmt.executeUpdate();
                    return true;
                }
            }

            return false;
        }
    }

    public static void showAll() throws SQLException {
        String url = "jdbc:sqlite:students.db";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM STUDENTS")) {

            while (rs.next()) {
                int id = rs.getInt("ID");
                String nume = rs.getString("NUME");
                String prenume = rs.getString("PRENUME");
                int varsta = rs.getInt("VARSTA");

                System.out.println("[APP] ID: " + id);
                System.out.println("[APP] Nume: " + nume);
                System.out.println("[APP] Prenume: " + prenume);
                System.out.println("[APP] Varsta: " + varsta);
            }
        }
    }
}