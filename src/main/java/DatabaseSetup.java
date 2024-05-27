import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetup {

    private final String url = "jdbc:postgresql://localhost:5432/tu_base_de_datos";
    private final String user = "tu_usuario";
    private final String password = "tu_contraseña";

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public void createTables() {
        String createBD1 = "CREATE TABLE IF NOT EXISTS BD1 (" +
                "ID_SALES SERIAL PRIMARY KEY, " +
                "ID_RUTA INT, " +
                "RUC INT, " +
                "NOMBRE VARCHAR(255), " +
                "COSTO_TOTAL DOUBLE PRECISION, " +
                "PRODUCTO VARCHAR(30), " +
                "LUGAR_DE_COMPRA VARCHAR(255), " +
                "ASIENTO INT, " +
                "FOREIGN KEY (ID_RUTA) REFERENCES BD2(ID_RUTA)" +
                ");";

        String createBD2 = "CREATE TABLE IF NOT EXISTS BD2 (" +
                "ID_RUTA SERIAL PRIMARY KEY, " +
                "NOMBRE_DE_RUTA VARCHAR(255), " +
                "DESDE VARCHAR(255), " +
                "HASTA VARCHAR(255), " +
                "AFORO INT, " +
                "COSTO DOUBLE PRECISION, " +
                "ID_BUS INT" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createBD2);
            stmt.execute(createBD1);
            System.out.println("Tablas creadas correctamente.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        DatabaseSetup dbSetup = new DatabaseSetup();
        dbSetup.createTables();
    }
}

