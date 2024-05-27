import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MiddlewareServer {

    private final int port = 12345; // Puerto del servidor
    private final String dbUrl = "jdbc:postgresql://localhost:5432/tu_base_de_datos";
    private final String dbUser = "tu_usuario";
    private final String dbPassword = "tu_contraseña";

    public static void main(String[] args) {
        MiddlewareServer server = new MiddlewareServer();
        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Middleware server started on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket).start();
                } catch (Exception e) {
                    System.out.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error starting server: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

                String request = in.readLine();
                System.out.println("Received request: " + request);

                // Process the request and generate response
                String response = processRequest(request, conn);

                out.println(response);
            } catch (Exception e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        }

        private String processRequest(String request, Connection conn) {
            try {
                // Parse the request (example: "CHECK_AVAILABLE:ID_RUTA")
                String[] parts = request.split(":");
                String action = parts[0];
                int idRuta = Integer.parseInt(parts[1]);

                if ("CHECK_AVAILABLE".equalsIgnoreCase(action)) {
                    return checkAvailability(idRuta, conn);
                } else if ("SELL_TICKET".equalsIgnoreCase(action)) {
                    return sellTicket(parts, conn); // Example: "SELL_TICKET:ID_RUTA:RUC:NOMBRE:PRODUCTO:LUGAR_DE_COMPRA:ASIENTO:COSTO_TOTAL"
                } else {
                    return "ERROR: Unknown action";
                }
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }
        }

        private String checkAvailability(int idRuta, Connection conn) {
            String query = "SELECT aforo - COUNT(*) AS available_seats FROM BD1 WHERE id_ruta = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, idRuta);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int availableSeats = rs.getInt("available_seats");
                    return "AVAILABLE_SEATS:" + availableSeats;
                } else {
                    return "ERROR: Route not found";
                }
            } catch (SQLException e) {
                return "ERROR: " + e.getMessage();
            }
        }

        private String sellTicket(String[] parts, Connection conn) {
            try {
                int idRuta = Integer.parseInt(parts[1]);
                int ruc = Integer.parseInt(parts[2]);
                String nombre = parts[3];
                String producto = parts[4];
                String lugarDeCompra = parts[5];
                int asiento = Integer.parseInt(parts[6]);
                double costoTotal = Double.parseDouble(parts[7]);

                // Check availability
                String availability = checkAvailability(idRuta, conn);
                if (availability.startsWith("AVAILABLE_SEATS:")) {
                    int availableSeats = Integer.parseInt(availability.split(":")[1]);
                    if (availableSeats > 0) {
                        // Insert into BD1
                        String insertQuery = "INSERT INTO BD1 (id_ruta, ruc, nombre, producto, lugar_de_compra, asiento, costo_total) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                            stmt.setInt(1, idRuta);
                            stmt.setInt(2, ruc);
                            stmt.setString(3, nombre);
                            stmt.setString(4, producto);
                            stmt.setString(5, lugarDeCompra);
                            stmt.setInt(6, asiento);
                            stmt.setDouble(7, costoTotal);
                            stmt.executeUpdate();
                            return "SUCCESS: Ticket sold";
                        }
                    } else {
                        return "ERROR: No available seats";
                    }
                } else {
                    return availability;
                }
            } catch (SQLException e) {
                return "ERROR: " + e.getMessage();
            }
        }
    }
}
