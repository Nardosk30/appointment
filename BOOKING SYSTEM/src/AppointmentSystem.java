import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class AppointmentSystem extends Application {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appointment_system";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        initializeDatabase();
        showLoginScreen(primaryStage);
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Create tables
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "username VARCHAR(50) NOT NULL UNIQUE, " +
                             "password VARCHAR(100) NOT NULL, " +
                             "is_admin BOOLEAN DEFAULT FALSE)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS slot (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "start_time DATETIME NOT NULL, " +
                             "end_time DATETIME NOT NULL, " +
                             "is_available BOOLEAN DEFAULT TRUE)");
            
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS appointment (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY, " +
                             "user_id INT NOT NULL, " +
                             "slot_id INT NOT NULL, " +
                             "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                             "FOREIGN KEY (user_id) REFERENCES user(id), " +
                             "FOREIGN KEY (slot_id) REFERENCES slot(id))");
            
            // Insert admin user if not exists
            stmt.executeUpdate("INSERT IGNORE INTO user (username, password, is_admin) VALUES " +
                             "('admin', 'admin123', TRUE)");
            
            // Insert sample slots if empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM slot");
            rs.next();
            if (rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO slot (start_time, end_time) VALUES " +
                                 "(NOW() + INTERVAL 1 DAY + INTERVAL 9 HOUR, NOW() + INTERVAL 1 DAY + INTERVAL 10 HOUR), " +
                                 "(NOW() + INTERVAL 1 DAY + INTERVAL 11 HOUR, NOW() + INTERVAL 1 DAY + INTERVAL 12 HOUR), " +
                                 "(NOW() + INTERVAL 2 DAY + INTERVAL 9 HOUR, NOW() + INTERVAL 2 DAY + INTERVAL 10 HOUR)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showLoginScreen(Stage stage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginBtn, 1, 2);
        grid.add(registerBtn, 1, 3);

        loginBtn.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, is_admin FROM user WHERE username = ? AND password = ?")) {
                
                stmt.setString(1, usernameField.getText());
                stmt.setString(2, passwordField.getText());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    boolean isAdmin = rs.getBoolean("is_admin");
                    if (isAdmin) {
                        showAdminDashboard(stage, userId);
                    } else {
                        showUserDashboard(stage, userId);
                    }
                } else {
                    showAlert("Login Failed", "Invalid credentials");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        registerBtn.setOnAction(e -> showRegistrationScreen(stage));

        stage.setScene(new Scene(grid, 400, 300));
        stage.setTitle("Appointment System - Login");
        stage.show();
    }

    private void showRegistrationScreen(Stage stage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Button registerBtn = new Button("Register");
        Button backBtn = new Button("Back to Login");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(registerBtn, 1, 2);
        grid.add(backBtn, 1, 3);

        registerBtn.setOnAction(e -> {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO user (username, password) VALUES (?, ?)")) {
                
                stmt.setString(1, usernameField.getText());
                stmt.setString(2, passwordField.getText());
                stmt.executeUpdate();
                showAlert("Success", "Registration successful!");
                showLoginScreen(stage);
            } catch (SQLException ex) {
                showAlert("Error", "Registration failed: " + ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> showLoginScreen(stage));

        stage.setScene(new Scene(grid, 400, 300));
        stage.setTitle("Appointment System - Register");
    }

    private void showUserDashboard(Stage stage, int userId) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("Available Appointment Slots");
        Button refreshBtn = new Button("Refresh");
        Button logoutBtn = new Button("Logout");

        TableView<Slot> slotTable = new TableView<>();
        TableColumn<Slot, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Slot, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        
        TableColumn<Slot, String> endCol = new TableColumn<>("End Time");
        endCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        
        TableColumn<Slot, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        slotTable.getColumns().addAll(idCol, startCol, endCol, statusCol);
        refreshSlots(slotTable);

        Button bookBtn = new Button("Book Selected Slot");
        bookBtn.setOnAction(e -> {
            Slot selected = slotTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isAvailable()) {
                bookAppointment(stage, userId, selected.getId());
            } else {
                showAlert("Error", "Please select an available slot");
            }
        });

        refreshBtn.setOnAction(e -> refreshSlots(slotTable));
        logoutBtn.setOnAction(e -> showLoginScreen(stage));

        HBox buttonBox = new HBox(10, bookBtn, refreshBtn, logoutBtn);
        root.getChildren().addAll(title, slotTable, buttonBox);

        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("User Dashboard");
    }

    private void refreshSlots(TableView<Slot> table) {
        ObservableList<Slot> slots = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT s.id, s.start_time, s.end_time, s.is_available " +
                 "FROM slot s LEFT JOIN appointment a ON s.id = a.slot_id " +
                 "WHERE s.is_available = TRUE OR a.id IS NULL")) {
            
            while (rs.next()) {
                slots.add(new Slot(
                    rs.getInt("id"),
                    rs.getTimestamp("start_time").toLocalDateTime(),
                    rs.getTimestamp("end_time").toLocalDateTime(),
                    rs.getBoolean("is_available")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        table.setItems(slots);
    }

    private void bookAppointment(Stage stage, int userId, int slotId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Booking");
        confirm.setHeaderText("Book this appointment slot?");
        Optional<ButtonType> result = confirm.showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                // Start transaction
                conn.setAutoCommit(false);
                
                try (PreparedStatement updateSlot = conn.prepareStatement(
                         "UPDATE slot SET is_available = FALSE WHERE id = ?");
                     PreparedStatement insertAppt = conn.prepareStatement(
                         "INSERT INTO appointment (user_id, slot_id) VALUES (?, ?)")) {
                    
                    updateSlot.setInt(1, slotId);
                    updateSlot.executeUpdate();
                    
                    insertAppt.setInt(1, userId);
                    insertAppt.setInt(2, slotId);
                    insertAppt.executeUpdate();
                    
                    conn.commit();
                    showAlert("Success", "Appointment booked successfully!");
                    showUserDashboard(stage, userId);
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to book appointment: " + e.getMessage());
            }
        }
    }

    private void showAdminDashboard(Stage stage, int adminId) {
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));

        Label title = new Label("All Booked Appointments");
        Button refreshBtn = new Button("Refresh");
        Button logoutBtn = new Button("Logout");

        TableView<AppointmentView> apptTable = new TableView<>();
        TableColumn<AppointmentView, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<AppointmentView, String> userCol = new TableColumn<>("User");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        
        TableColumn<AppointmentView, String> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotTime"));
        
        TableColumn<AppointmentView, String> bookedCol = new TableColumn<>("Booked At");
        bookedCol.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        apptTable.getColumns().addAll(idCol, userCol, slotCol, bookedCol);
        refreshAppointments(apptTable);

        refreshBtn.setOnAction(e -> refreshAppointments(apptTable));
        logoutBtn.setOnAction(e -> showLoginScreen(stage));

        HBox buttonBox = new HBox(10, refreshBtn, logoutBtn);
        root.getChildren().addAll(title, apptTable, buttonBox);

        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("Admin Dashboard");
    }

    private void refreshAppointments(TableView<AppointmentView> table) {
        ObservableList<AppointmentView> appointments = FXCollections.observableArrayList();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT a.id, u.username, s.start_time, s.end_time, a.created_at " +
                 "FROM appointment a " +
                 "JOIN user u ON a.user_id = u.id " +
                 "JOIN slot s ON a.slot_id = s.id")) {
            
            while (rs.next()) {
                appointments.add(new AppointmentView(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getTimestamp("start_time").toLocalDateTime(),
                    rs.getTimestamp("end_time").toLocalDateTime(),
                    rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        table.setItems(appointments);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Model classes
    public static class Slot {
        private final int id;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final boolean isAvailable;

        public Slot(int id, LocalDateTime startTime, LocalDateTime endTime, boolean isAvailable) {
            this.id = id;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isAvailable = isAvailable;
        }

        public int getId() { return id; }
        public String getStartTime() { return startTime.toString(); }
        public String getEndTime() { return endTime.toString(); }
        public String getStatus() { return isAvailable ? "Available" : "Booked"; }
        public boolean isAvailable() { return isAvailable; }
    }

    public static class AppointmentView {
        private final int id;
        private final String username;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final LocalDateTime createdAt;

        public AppointmentView(int id, String username, LocalDateTime startTime, 
                             LocalDateTime endTime, LocalDateTime createdAt) {
            this.id = id;
            this.username = username;
            this.startTime = startTime;
            this.endTime = endTime;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getSlotTime() { 
            return startTime.toString() + " to " + endTime.toString(); 
        }
        public String getCreatedAt() { return createdAt.toString(); }
    }
}