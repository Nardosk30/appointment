import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class LoginApp extends Application {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/appointment_system?useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private StackPane layout;
    private VBox loginLayout;
    private VBox registerLayout;
    private VBox welcomeLayout;
    private int loginAttempts = 0;
    private final int maxAttempts = 3;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        System.out.println("=== Starting Application ===");
        
        // Initialize database with sample data
        initializeDatabase();
        
        // Verify data was inserted
        verifyDataExists();
        
        // Create UI
        createUI(stage);
        
        Scene scene = new Scene(layout, 600, 400);
        stage.setTitle("Appointment System Login");
        stage.setScene(scene);
        stage.show();
    }

    private void initializeDatabase() {
        System.out.println("Initializing database...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                // Create tables if they don't exist
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user (" +
                                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                 "username VARCHAR(50) NOT NULL UNIQUE, " +
                                 "password VARCHAR(100) NOT NULL)");
                
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS appointments (" +
                                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                 "title VARCHAR(100) NOT NULL, " +
                                 "description TEXT, " +
                                 "date_time DATETIME NOT NULL, " +
                                 "duration_minutes INT NOT NULL, " +
                                 "is_available BOOLEAN DEFAULT TRUE, " +
                                 "booked_by INT NULL, " +
                                 "FOREIGN KEY (booked_by) REFERENCES user(id))");
                
                // Clear any existing data
                stmt.executeUpdate("DELETE FROM appointments");
                
                // Insert default appointments
                stmt.executeUpdate("INSERT INTO appointments (title, description, date_time, duration_minutes) VALUES " +
                                 "('Dental Checkup', 'Routine dental examination', NOW() + INTERVAL 1 DAY, 30), " +
                                 "('Eye Examination', 'Complete vision test', NOW() + INTERVAL 2 DAY, 45), " +
                                 "('Physical Therapy', 'Knee rehabilitation session', NOW() + INTERVAL 3 DAY, 60), " +
                                 "('Vaccination', 'Annual flu shot', NOW() + INTERVAL 4 DAY, 15), " +
                                 "('General Checkup', 'Annual health assessment', NOW() + INTERVAL 5 DAY, 30)");
                
                System.out.println("Successfully inserted 5 default appointments");
            }
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
            showAlert("Database Error", "Failed to initialize database. Check console for details.");
        }
    }

    private void verifyDataExists() {
        System.out.println("Verifying data exists...");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM appointments")) {
            
            rs.next();
            int count = rs.getInt(1);
            System.out.println("Found " + count + " appointments in database");
            
            if (count == 0) {
                System.err.println("ERROR: No appointments found after initialization!");
            } else {
                System.out.println("Sample appointments:");
                ResultSet appointments = stmt.executeQuery("SELECT id, title FROM appointments");
                while (appointments.next()) {
                    System.out.println("- " + appointments.getInt("id") + ": " + appointments.getString("title"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying data: " + e.getMessage());
        }
    }

    private void createUI(Stage stage) {
        layout = new StackPane();
        layout.setPadding(new Insets(20));

        // Welcome Screen
        createWelcomeScreen(stage);
        
        // Login Form
        createLoginForm(stage);
        
        // Register Form
        createRegisterForm();
        
        layout.getChildren().addAll(welcomeLayout, loginLayout, registerLayout);
        registerLayout.setVisible(false);
        loginLayout.setVisible(false);
    }

    private void createWelcomeScreen(Stage stage) {
        welcomeLayout = new VBox(20);
        welcomeLayout.setPadding(new Insets(30));
        welcomeLayout.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 10px;");
        welcomeLayout.setPrefWidth(400);

        Label welcomeMessage = new Label("Welcome to the Appointment System");
        welcomeMessage.setFont(Font.font("Arial", 24));
        welcomeMessage.setTextFill(Color.WHITE);

        Button proceedButton = new Button("Go to Login");
        styleButton(proceedButton, "#FF9800", "#FF5722");

        proceedButton.setOnAction(e -> transitionToLogin());
        welcomeLayout.getChildren().addAll(welcomeMessage, proceedButton);
    }

    private void transitionToLogin() {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), welcomeLayout);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            welcomeLayout.setVisible(false);
            showLoginForm();
        });
        fadeOut.play();
    }

    private void createLoginForm(Stage stage) {
        loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(20));
        loginLayout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px;");

        Label loginTitle = new Label("Login");
        loginTitle.setFont(Font.font("Arial", 18));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Button loginButton = new Button("Login");
        Button goToRegisterButton = new Button("Go to Register");
        Label loginMessage = new Label();

        loginButton.setOnAction(e -> handleLogin(stage, usernameField, passwordField, loginMessage));
        goToRegisterButton.setOnAction(e -> showRegisterForm());

        loginLayout.getChildren().addAll(loginTitle, usernameField, passwordField, loginButton, goToRegisterButton, loginMessage);
    }

    private void handleLogin(Stage stage, TextField usernameField, PasswordField passwordField, Label loginMessage) {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            validateLoginInput(username, password);

            Integer userId = authenticateUser(username, password);
            if (userId != null) {
                loginMessage.setText("✅ Login successful!");
                loginAttempts = 0;
                showMainApplication(stage, userId);
            } else {
                handleFailedLogin();
            }
        } catch (IllegalArgumentException ex) {
            loginMessage.setText("Error: " + ex.getMessage());
        } catch (Exception ex) {
            loginMessage.setText("Unexpected error: " + ex.getMessage());
        }
    }

    private void validateLoginInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Username or password cannot be empty");
        }
        if (loginAttempts >= maxAttempts) {
            throw new IllegalArgumentException("Too many failed attempts. Please try again later.");
        }
    }

    private void handleFailedLogin() {
        loginAttempts++;
        throw new IllegalArgumentException("❌ Invalid credentials. Attempts left: " + (maxAttempts - loginAttempts));
    }

    private void createRegisterForm() {
        registerLayout = new VBox(10);
        registerLayout.setPadding(new Insets(20));

        Label registerTitle = new Label("Create Account");
        registerTitle.setFont(Font.font("Arial", 18));

        TextField registerUsername = new TextField();
        registerUsername.setPromptText("Username");
        PasswordField registerPassword = new PasswordField();
        registerPassword.setPromptText("Password");

        Button registerButton = new Button("Create Account");
        Button goToLoginButton = new Button("Back to Login");
        Label registerMessage = new Label();

        registerButton.setOnAction(e -> handleRegistration(registerUsername, registerPassword, registerMessage));
        goToLoginButton.setOnAction(e -> showLoginForm());

        registerLayout.getChildren().addAll(registerTitle, registerUsername, registerPassword, registerButton, goToLoginButton, registerMessage);
    }

    private void handleRegistration(TextField registerUsername, PasswordField registerPassword, Label registerMessage) {
        try {
            String user = registerUsername.getText();
            String pass = registerPassword.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                throw new IllegalArgumentException("Username or password cannot be empty");
            }

            boolean success = registerUser(user, pass);
            if (success) {
                registerMessage.setText("✅ Account created successfully!");
            } else {
                throw new IllegalArgumentException("⚠️ User already exists.");
            }
        } catch (IllegalArgumentException ex) {
            registerMessage.setText("Error: " + ex.getMessage());
        } catch (Exception ex) {
            registerMessage.setText("Unexpected error: " + ex.getMessage());
        }
    }

    private Integer authenticateUser(String username, String password) {
        String sql = "SELECT id FROM user WHERE username = ? AND password = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return null;
        }
    }

    private boolean registerUser(String username, String password) {
        String sql = "INSERT INTO user (username, password) VALUES (?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false;
            }
            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    private void showMainApplication(Stage stage, int userId) {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        
        Label welcomeLabel = new Label("Available Appointments");
        welcomeLabel.setFont(Font.font(24));
        
        TableView<Appointment> appointmentTable = createAppointmentTable();
        
        // Load and display appointments
        List<Appointment> appointments = getAvailableAppointments();
        System.out.println("Displaying " + appointments.size() + " appointments");
        appointmentTable.getItems().addAll(appointments);
        
        // Refresh button
        Button refreshButton = new Button("Refresh Appointments");
        refreshButton.setOnAction(e -> {
            appointmentTable.getItems().clear();
            appointmentTable.getItems().addAll(getAvailableAppointments());
        });
        
        // Appointment booking button
        Button bookButton = new Button("Book Selected Appointment");
        bookButton.setOnAction(e -> {
            Appointment selected = appointmentTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.isAvailable()) {
                boolean success = bookAppointment(selected.getId(), userId);
                if (success) {
                    showAlert("Success", "Appointment booked successfully!");
                    refreshButton.fire(); // Refresh the table
                } else {
                    showAlert("Error", "Failed to book appointment");
                }
            } else {
                showAlert("Error", "Please select an available appointment");
            }
        });
        
        // Other navigation buttons
        Button viewBookedButton = new Button("View My Appointments");
        viewBookedButton.setOnAction(e -> showUserAppointments(stage, userId));
        
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> stage.setScene(new Scene(layout, 600, 400)));
        
        // Debug button
        Button debugButton = new Button("Debug Info");
        debugButton.setOnAction(e -> {
            System.out.println("=== DEBUG INFORMATION ===");
            System.out.println("Database URL: " + DB_URL);
            System.out.println("Current User ID: " + userId);
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM appointments")) {
                
                System.out.println("All appointments in database:");
                while (rs.next()) {
                    System.out.printf("- ID: %d, Title: %s, Available: %b%n",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getBoolean("is_available"));
                }
            } catch (SQLException ex) {
                System.err.println("Debug query failed: " + ex.getMessage());
            }
        });
        
        HBox buttonBox = new HBox(10, bookButton, refreshButton, viewBookedButton, debugButton, logoutButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        mainLayout.getChildren().addAll(welcomeLabel, appointmentTable, buttonBox);
        mainLayout.setAlignment(Pos.CENTER);
        
        stage.setScene(new Scene(mainLayout, 800, 600));
    }

    private TableView<Appointment> createAppointmentTable() {
        TableView<Appointment> table = new TableView<>();
        
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Appointment, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        
        TableColumn<Appointment, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        
        TableColumn<Appointment, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getDuration() + " mins"));
        
        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().isAvailable() ? "Available" : "Booked"));
        
        table.getColumns().addAll(idCol, titleCol, dateCol, durationCol, statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        return table;
    }

    private List<Appointment> getAvailableAppointments() {
        List<Appointment> appointments = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM appointments WHERE is_available = TRUE")) {
            
            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("date_time").toLocalDateTime(),
                    rs.getInt("duration_minutes"),
                    rs.getBoolean("is_available")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching appointments: " + e.getMessage());
        }
        
        return appointments;
    }

    private boolean bookAppointment(int appointmentId, int userId) {
        String sql = "UPDATE appointments SET is_available = FALSE, booked_by = ? WHERE id = ? AND is_available = TRUE";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, appointmentId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error booking appointment: " + e.getMessage());
            return false;
        }
    }

    private void showUserAppointments(Stage stage, int userId) {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Your Booked Appointments");
        titleLabel.setFont(Font.font(24));
        
        TableView<Appointment> appointmentTable = createAppointmentTable();
        appointmentTable.getItems().addAll(getUserAppointments(userId));
        
        Button backButton = new Button("Back to Available Appointments");
        backButton.setOnAction(e -> showMainApplication(stage, userId));
        
        layout.getChildren().addAll(titleLabel, appointmentTable, backButton);
        layout.setAlignment(Pos.CENTER);
        
        stage.setScene(new Scene(layout, 800, 600));
    }

    private List<Appointment> getUserAppointments(int userId) {
        List<Appointment> appointments = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE booked_by = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                appointments.add(new Appointment(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getTimestamp("date_time").toLocalDateTime(),
                    rs.getInt("duration_minutes"),
                    false
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user appointments: " + e.getMessage());
        }
        return appointments;
    }

    private void showLoginForm() {
        loginLayout.setVisible(true);
        registerLayout.setVisible(false);
    }

    private void showRegisterForm() {
        loginLayout.setVisible(false);
        registerLayout.setVisible(true);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void styleButton(Button button, String normalColor, String hoverColor) {
        button.setStyle("-fx-font-size: 16px; -fx-background-color: " + normalColor + "; -fx-text-fill: white;");
        button.setPadding(new Insets(10, 20, 10, 20));
        button.setOnMouseEntered(e -> button.setStyle("-fx-font-size: 16px; -fx-background-color: " + hoverColor + "; -fx-text-fill: white;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-font-size: 16px; -fx-background-color: " + normalColor + "; -fx-text-fill: white;"));
    }

    public static class Appointment {
        private final int id;
        private final String title;
        private final String description;
        private final LocalDateTime dateTime;
        private final int duration;
        private final boolean isAvailable;
        
        public Appointment(int id, String title, String description, LocalDateTime dateTime, int duration, boolean isAvailable) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.dateTime = dateTime;
            this.duration = duration;
            this.isAvailable = isAvailable;
        }
        
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getDateTime() { return dateTime; }
        public int getDuration() { return duration; }
        public boolean isAvailable() { return isAvailable; }
    }
}