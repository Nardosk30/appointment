import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class RegisterUI {
    private VBox view;

    public RegisterUI(Stage stage) {
        Label userLabel = new Label("Username:");
        TextField usernameField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();

        Label emailLabel = new Label("Email:");
        TextField emailField = new TextField();

        Button registerBtn = new Button("Register");
        Button backBtn = new Button("Back");

        registerBtn.setOnAction(e -> {
            String user = usernameField.getText();
            String pass = passwordField.getText();
            String email = emailField.getText();

            if (Auth.register(user, pass, email)) {
                new Alert(Alert.AlertType.INFORMATION, "Registration successful! Please log in.").show();
                LoginUI loginUI = new LoginUI(stage);
                stage.setScene(new Scene(loginUI.getView(), 400, 300));
            } else {
                new Alert(Alert.AlertType.ERROR, "Registration failed. Username might exist.").show();
            }
        });

        backBtn.setOnAction(e -> {
            LoginUI loginUI = new LoginUI(stage);
            stage.setScene(new Scene(loginUI.getView(), 400, 300));
        });

        view = new VBox(10, userLabel, usernameField, passLabel, passwordField, emailLabel, emailField, registerBtn, backBtn);
        view.setPadding(new Insets(20));
        view.setAlignment(Pos.CENTER);
    }

    public VBox getView() {
        return view;
    }
}

