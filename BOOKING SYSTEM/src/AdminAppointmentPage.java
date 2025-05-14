import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.geometry.*;
import java.sql.*;


public class AdminAppointmentPage extends Application {

    private TableView<Appointment> table = new TableView<>();
    private ObservableList<Appointment> data = FXCollections.observableArrayList();

    TextField userIdField = new TextField();
    TextField dateField = new TextField();
    TextField timeField = new TextField();
    TextField statusField = new TextField();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Admin - Manage Appointments");

        // Table columns
        TableColumn<Appointment, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(cell -> cell.getValue().appointmentIdProperty().asObject());

        TableColumn<Appointment, Integer> userCol = new TableColumn<>("User ID");
        userCol.setCellValueFactory(cell -> cell.getValue().userIdProperty().asObject());

        TableColumn<Appointment, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> cell.getValue().dateProperty());

        TableColumn<Appointment, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cell -> cell.getValue().timeProperty());

        TableColumn<Appointment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> cell.getValue().statusProperty());

        table.getColumns().addAll(idCol, userCol, dateCol, timeCol, statusCol);
        table.setItems(data);
        loadAppointments();

        // Input form
        userIdField.setPromptText("User ID");
        dateField.setPromptText("YYYY-MM-DD");
        timeField.setPromptText("HH:MM:SS");
        statusField.setPromptText("Status");

        Button addButton = new Button("Add Appointment");
        addButton.setOnAction(e -> addAppointment());

        Button deleteButton = new Button("Delete Selected");
        deleteButton.setOnAction(e -> deleteAppointment());

        HBox inputBox = new HBox(10, userIdField, dateField, timeField, statusField, addButton, deleteButton);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, table, inputBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadAppointments() {
        data.clear();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM appointments")) {

            while (rs.next()) {
                data.add(new Appointment(
                    rs.getInt("appointment_id"),
                    rs.getInt("user_id"),
                    rs.getString("date"),
                    rs.getString("time"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            showAlert("Error loading appointments: " + e.getMessage());
        }
    }

    private void addAppointment() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO appointments (user_id, date, time, status) VALUES (?, ?, ?, ?)")) {

            stmt.setInt(1, Integer.parseInt(userIdField.getText()));
            stmt.setString(2, dateField.getText());
            stmt.setString(3, timeField.getText());
            stmt.setString(4, statusField.getText());
            stmt.executeUpdate();

            loadAppointments();
            clearFields();
        } catch (SQLException | NumberFormatException e) {
            showAlert("Error adding appointment: " + e.getMessage());
        }
    }

    private void deleteAppointment() {
        Appointment selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Please select an appointment to delete.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM appointments WHERE appointment_id = ?")) {
            stmt.setInt(1, selected.getAppointmentId());
            stmt.executeUpdate();
            loadAppointments();
        } catch (SQLException e) {
            showAlert("Error deleting appointment: " + e.getMessage());
        }
    }

    private void clearFields() {
        userIdField.clear();
        dateField.clear();
        timeField.clear();
        statusField.clear();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }
}

