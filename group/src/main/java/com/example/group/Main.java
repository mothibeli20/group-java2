    package com.example.group;

    import javafx.scene.control.ScrollPane;
    import javafx.scene.text.Font;
    import javafx.application.Application;
    import javafx.beans.property.SimpleStringProperty;
    import javafx.beans.property.StringProperty;
    import javafx.geometry.Insets;
    import javafx.geometry.Pos;
    import javafx.scene.Node;
    import javafx.scene.Scene;
    import javafx.scene.chart.BarChart;
    import javafx.scene.chart.CategoryAxis;
    import javafx.scene.chart.NumberAxis;
    import javafx.scene.chart.XYChart;
    import javafx.scene.control.*;
    import javafx.scene.control.Button;
    import javafx.scene.control.Dialog;
    import javafx.scene.control.Label;
    import javafx.scene.control.Menu;
    import javafx.scene.control.MenuBar;
    import javafx.scene.control.MenuItem;
    import javafx.scene.control.TextArea;
    import javafx.scene.control.TextField;
    import javafx.scene.control.cell.PropertyValueFactory;
    import javafx.scene.layout.*;
    import javafx.stage.FileChooser;
    import javafx.stage.Stage;
    
    import java.awt.*;

    import java.io.File;
    import java.sql.*;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.util.ArrayList;

    import javafx.scene.effect.DropShadow;
    import javafx.scene.paint.Color;
    import javafx.animation.FadeTransition;
    import javafx.util.Duration;
    import javafx.scene.layout.Priority;
    
    public class Main extends Application {
        private Stage primaryStage;
        private String currentUser = null;
        private String currentRole = null;
        private BorderPane rootLayout;
    
        public static void main(String[] args) {
            launch(args);
        }
    
        @Override
        public void start(Stage primaryStage) {
            this.primaryStage = primaryStage;
            primaryStage.setTitle("Learning Management System");
            rootLayout = new BorderPane();
    
            // Show login view first
            showLoginView();
    
            Scene scene = new Scene(rootLayout, 800, 600);
            // Apply CSS
            scene.getStylesheets().add(getClass().getResource("/learning.css").toExternalForm());
    
            primaryStage.setScene(scene);
            primaryStage.show();
        }
    
        // ======================= Utility: Log system activity =======================
        private void logSystemAction(String username, String action) {
            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO system_logs (username, action) VALUES (?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, action);
                ps.executeUpdate();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    
        private void showLoginView() {
            VBox loginBox = new VBox(10);
            loginBox.setPadding(new Insets(20));
            loginBox.setStyle("-fx-background-color: #F0F8FF;");
            Label lblTitle = new Label("Login to LMS");
            lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2E8B57;");
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            Button btnLogin = new Button("Login");
            btnLogin.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnLogin.setOnAction(e -> handleLogin(usernameField.getText(), passwordField.getText()));
    
            Button btnSignup = new Button("Sign Up");
            btnSignup.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnSignup.setOnAction(e -> showRegistrationDialog());
    
            HBox hbButtons = new HBox(10, btnLogin, btnSignup);
            loginBox.getChildren().addAll(lblTitle, new Label("Username:"), usernameField,
                    new Label("Password:"), passwordField, hbButtons);
            rootLayout.setCenter(loginBox);
            rootLayout.setTop(null);
        }
    
        private void handleLogin(String username, String password) {
            if (username.isEmpty() || password.isEmpty()) {
                showErrorAlert("Login Error", "Please enter username and password");
                return;
            }
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT password, role FROM users WHERE username=?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String role = rs.getString("role");
                    if (verifyPassword(password, storedHash)) {
                        currentUser = username;
                        currentRole = role;
                        logSystemAction(currentUser, "Logged in");
                        showMainMenu();
                    } else {
                        showErrorAlert("Login Failed", "Incorrect password");
                    }
                } else {
                    showErrorAlert("Login Failed", "User not found");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showMainMenu() {
            // Create MenuBar with role-specific menu items
            MenuBar menuBar = new MenuBar();
    
            // Role-specific menu items
            Menu menuButton = new Menu("Menu");
            menuButton.setStyle("-fx-background-color: #4682B4; -fx-text-fill: white; -fx-font-size: 14px;");
    
            // Common Logout menu item
            MenuItem logoutItem = new MenuItem("Logout");
            logoutItem.setOnAction(e -> logout());
    
            if ("admin".equalsIgnoreCase(currentRole)) {
                // ... your admin menu items ...
                MenuItem manageUsersItem = new MenuItem("Manage Users");
                manageUsersItem.setOnAction(e -> showManageUsersPanel());
                MenuItem manageCoursesItem = new MenuItem("Manage Courses");
                manageCoursesItem.setOnAction(e -> showManageCoursesPanel());
                MenuItem systemLogsItem = new MenuItem("View System Logs");
                systemLogsItem.setOnAction(e -> showSystemLogs());
                MenuItem reportsItem = new MenuItem("Generate Reports");
                reportsItem.setOnAction(e -> showReports());
                MenuItem systemSettingsItem = new MenuItem("System Settings");
                systemSettingsItem.setOnAction(e -> showSystemSettings());
                MenuItem notificationsItem = new MenuItem("Manage Notifications");
                notificationsItem.setOnAction(e -> showManageNotifications());
                MenuItem discussionItem = new MenuItem("Discussion Forums");
                discussionItem.setOnAction(e -> showDiscussionForums());
    
                menuButton.getItems().addAll(manageUsersItem, manageCoursesItem, systemLogsItem, reportsItem,
                        systemSettingsItem, notificationsItem, discussionItem);
            } else if ("instructor".equalsIgnoreCase(currentRole)) {
                MenuItem dashboardItem = new MenuItem("Dashboard");
                dashboardItem.setOnAction(e -> {
                    Node dashboardNode = showInstructorDashboard(currentUser);
                    rootLayout.setCenter(dashboardNode);
                });
                menuButton.getItems().addAll(dashboardItem);
            } else if ("student".equalsIgnoreCase(currentRole)) {
                MenuItem dashboardItem = new MenuItem("Dashboard");
                dashboardItem.setOnAction(e -> {
                    Node dashboardNode = showStudentDashboard(currentUser);
                    rootLayout.setCenter(dashboardNode);
                });
                menuButton.getItems().addAll(dashboardItem);
            }
            menuButton.getItems().add(new SeparatorMenuItem());
            menuButton.getItems().add(logoutItem);
            menuBar.getMenus().add(menuButton);
    
            // Create the "Logout" Button with effects
            Button btnExit = new Button("Logout");
            btnExit.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
    
            // Apply DropShadow effect for visual enhancement
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.GRAY);
            shadow.setRadius(5);
            btnExit.setEffect(shadow);
    
            btnExit.setOnAction(e -> logout());
    
            // Create an animated button with fade in/out effect
            Button animatedButton = new Button("I Fade In & Out");
            animatedButton.setStyle("-fx-background-color: #009688; -fx-text-fill: white; -fx-font-size: 14px;");
    
            // Set up fade transition
            FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2), animatedButton);
            fadeTransition.setFromValue(1.0);
            fadeTransition.setToValue(0.3);
            fadeTransition.setCycleCount(FadeTransition.INDEFINITE);
            fadeTransition.setAutoReverse(true);
            fadeTransition.play();
    
            // Assemble the top bar
            HBox hb = new HBox(10, menuBar, new Region(), animatedButton, btnExit);
            HBox.setHgrow(hb.getChildren().get(1), Priority.ALWAYS); // spacer
            rootLayout.setTop(hb);
    
            // Set default center content
            Label welcome = new Label("Welcome, " + currentUser);
            rootLayout.setCenter(welcome);
        }
    
        private void logout() {
            logSystemAction(currentUser, "Logged out");
            currentUser = null;
            currentRole = null;
            // Clear the main layout
            rootLayout.setTop(null);
            rootLayout.setCenter(null);
            // Show login view
            showLoginView();
        }
    
        // ======================= Database connection and helpers =======================
        private Connection getConnection() throws SQLException {
            String url = "jdbc:postgresql://localhost:5432/java"; // update as needed
            String user = "postgres";
            String password = "123456";
            return DriverManager.getConnection(url, user, password);
        }
    
        private String hashPassword(String password) {
            // implement your hashing
            return password; // placeholder
        }
    
        private boolean verifyPassword(String input, String storedHash) {
            // implement your password verification
            return input.equals(storedHash); // placeholder
        }
    
        // ======================= Registration Dialog =======================
        private void showRegistrationDialog() {
            Dialog<Map<String, String>> dialog = new Dialog<>();
            dialog.setTitle("Register New User");
            dialog.setHeaderText("Fill in details to register");
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setStyle("-fx-background-color: #F0F8FF;");
    
            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));
            TextField usernameField = new TextField(); usernameField.setPromptText("Username");
            TextField emailField = new TextField(); emailField.setPromptText("Email");
            PasswordField passwordField = new PasswordField(); passwordField.setPromptText("Password");
            ChoiceBox<String> roleChoice = new ChoiceBox<>();
            roleChoice.getItems().addAll("admin", "instructor", "student");
            roleChoice.setValue("student");
    
            grid.add(new Label("Username:"), 0, 0); grid.add(usernameField, 1, 0);
            grid.add(new Label("Email:"), 0, 1); grid.add(emailField, 1, 1);
            grid.add(new Label("Password:"), 0, 2); grid.add(passwordField, 1, 2);
            grid.add(new Label("Role:"), 0, 3); grid.add(roleChoice, 1, 3);
    
            dialog.getDialogPane().setContent(grid);
    
            // Style OK/Cancel buttons
            for (ButtonType btnType : dialog.getDialogPane().getButtonTypes()) {
                Node btnNode = dialog.getDialogPane().lookupButton(btnType);
                if (btnNode != null) {
                    btnNode.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8px 16px;");
                }
            }
    
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    Map<String, String> data = new HashMap<>();
                    data.put("username", usernameField.getText());
                    data.put("email", emailField.getText());
                    data.put("password", passwordField.getText());
                    data.put("role", roleChoice.getValue());
                    return data;
                }
                return null;
            });
            Optional<Map<String, String>> result = dialog.showAndWait();
            result.ifPresent(data -> {
                try (Connection conn = getConnection()) {
                    String hashedPw = hashPassword(data.get("password"));
                    String sql = "INSERT INTO users (username, email, password, role, active) VALUES (?, ?, ?, ?, TRUE)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, data.get("username"));
                    ps.setString(2, data.get("email"));
                    ps.setString(3, hashedPw);
                    ps.setString(4, data.get("role"));
                    ps.executeUpdate();
                    ps.close();
                    showAlert("Success", "User registered successfully");
                    logSystemAction(data.get("username"), "Registered new user");
                } catch (SQLException e) {
                    showErrorAlert("Error", e.getMessage());
                }
            });
        }
    
        // ======================= Utility Methods =======================
        private void showAlert(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    
        private void showErrorAlert(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    
        // ======================= Data Classes =======================
        public static class User {
            private StringProperty username;
            private StringProperty email;
            private StringProperty role;
            private StringProperty status;
    
            public User(String username, String email, String role, String status) {
                this.username= new SimpleStringProperty(username);
                this.email= new SimpleStringProperty(email);
                this.role= new SimpleStringProperty(role);
                this.status= new SimpleStringProperty(status);
            }
            public String getUsername() { return username.get(); }
            public String getEmail() { return email.get(); }
            public String getRole() { return role.get(); }
            public String getStatus() { return status.get(); }
            public StringProperty usernameProperty() { return username; }
            public StringProperty emailProperty() { return email; }
            public StringProperty roleProperty() { return role; }
            public StringProperty statusProperty() { return status; }
        }
    
        public static class Course {
            private String id;
            private StringProperty name;
            private StringProperty description;
            private StringProperty instructor;
            private StringProperty status;
    
            public Course(String id, String name, String description, String instructor, String status) {
                this.id= id;
                this.name= new SimpleStringProperty(name);
                this.description= new SimpleStringProperty(description);
                this.instructor= new SimpleStringProperty(instructor);
                this.status= new SimpleStringProperty(status);
            }
    
            public String getId() { return id; }
            public String getName() { return name.get(); }
            public String getDescription() { return description.get(); }
            public String getInstructor() { return instructor.get(); }
            public String getStatus() { return status.get(); }
            public StringProperty nameProperty() { return name; }
            public StringProperty descriptionProperty() { return description; }
            public StringProperty instructorProperty() { return instructor; }
            public StringProperty statusProperty() { return status; }
            @Override
            public String toString() { return getName(); }
        }
    
        public static class Assessment {
            private String id;
            private String courseId;
            private StringProperty title;
            private StringProperty description;
    
            public Assessment(String id, String courseId, String title, String description) {
                this.id= id;
                this.courseId= courseId;
                this.title= new SimpleStringProperty(title);
                this.description= new SimpleStringProperty(description);
            }
    
            public String getId() { return id; }
            public String getCourseId() { return courseId; }
            public String getTitle() { return title.get(); }
            public String getDescription() { return description.get(); }
            public StringProperty titleProperty() { return title; }
            public StringProperty descriptionProperty() { return description; }
        }
    
        public static class Submission {
            private String id;
            private String studentUsername;
            private String assessmentId;
            private String submissionText;
            private String grade;
    
            public Submission(String id, String studentUsername, String assessmentId, String submissionText, String grade) {
                this.id= id;
                this.studentUsername= studentUsername;
                this.assessmentId= assessmentId;
                this.submissionText= submissionText;
                this.grade= grade;
            }
    
            public String getId() { return id; }
            public String getStudentUsername() { return studentUsername; }
            public String getAssessmentId() { return assessmentId; }
            public String getSubmissionText() { return submissionText; }
            public String getGrade() { return grade; }
        }

        public static class StudentProgress {
            private String studentUsername;
            private String courseId;
            private int progressPercentage;

            public StudentProgress(String studentUsername, String courseId, int progressPercentage) {
                this.studentUsername= studentUsername;
                this.courseId= courseId;
                this.progressPercentage= progressPercentage;
            }
            public String getStudentUsername() { return studentUsername; }
            public String getCourseId() { return courseId; }
            public int getProgressPercentage() { return progressPercentage; }
        }
    
    
        public static class GradeProgress {
            private String assessmentTitle;
            private String grade;
    
            public GradeProgress(String assessmentTitle, String grade) {
                this.assessmentTitle= assessmentTitle;
                this.grade= grade;
            }
            public String getAssessmentTitle() { return assessmentTitle; }
            public String getGrade() { return grade; }
    
        }
    
        public static class MCQQuestion {
            private int questionId; // generated after saving to DB
            private String questionText;
            private boolean isMultipleAnswer; // true if multiple correct answers
    
            public MCQQuestion(String questionText, boolean isMultipleAnswer) {
                this.questionText = questionText;
                this.isMultipleAnswer = isMultipleAnswer;
            }
    
            // getters and setters
            public int getQuestionId() { return questionId; }
            public void setQuestionId(int questionId) { this.questionId = questionId; }
            public String getQuestionText() { return questionText; }
            public boolean isMultipleAnswer() { return isMultipleAnswer; }
        }
    
        public static class MCQOption {
            private String optionText;
            private boolean isCorrect;
    
            public MCQOption(String optionText, boolean isCorrect) {
                this.optionText = optionText;
                this.isCorrect = isCorrect;
            }
    
            public String getOptionText() { return optionText; }
            public boolean isCorrect() { return isCorrect; }
        }
    
        // ======================= Additional role-specific panels =======================
    
        // Manage Users Panel
        private void showManageUsersPanel() {
            Stage stage = new Stage();
            stage.setTitle("Manage Users");
            BorderPane pane = new BorderPane();
    
            TableView<User> userTable = new TableView<>();
            TableColumn<User, String> colUser = new TableColumn<>("Username");
            colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
            TableColumn<User, String> colEmail = new TableColumn<>("Email");
            colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
            TableColumn<User, String> colRole = new TableColumn<>("Role");
            colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            TableColumn<User, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            userTable.getColumns().addAll(colUser, colEmail, colRole, colStatus);
            loadUsersIntoTable(userTable);
    
            Button btnAdd = new Button("Add User");
            btnAdd.setOnAction(e -> showAddUserDialog(userTable));
            Button btnUpdate = new Button("Update User");
            btnUpdate.setOnAction(e -> showUpdateUserDialog(userTable));
            Button btnDeactivate = new Button("Deactivate User");
            btnDeactivate.setOnAction(e -> deactivateSelectedUser(userTable));
            Button btnActivate = new Button("Activate User");
            btnActivate.setOnAction(e -> activateSelectedUser(userTable));
            // New Enrollment Button
            Button btnEnroll = new Button("Enroll in Course");
            btnEnroll.setStyle("-fx-background-color: #FF5722; -fx-text-fill: white;");
            btnEnroll.setOnAction(e -> showEnrollmentDialog(userTable));
    
            HBox hb = new HBox(10, btnAdd, btnUpdate, btnDeactivate, btnActivate, btnEnroll);
            hb.setPadding(new Insets(10));
            pane.setCenter(userTable);
            pane.setBottom(hb);
    
            Scene scene = new Scene(pane, 900, 600);
            stage.setScene(scene);
            stage.show();
        }
    
        // Enrollment Dialog for Admin
        private void showEnrollmentDialog(TableView<User> userTable) {
            User selectedUser = userTable.getSelectionModel().getSelectedItem();
            if (selectedUser == null) {
                showErrorAlert("Error", "Select a user to enroll");
                return;
            }
    
            Stage stage = new Stage();
            stage.setTitle("Enroll " + selectedUser.getUsername() + " in a Course");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
    
            ComboBox<Course> courseComboBox = new ComboBox<>();
            loadAllCourses(courseComboBox);
            courseComboBox.setPromptText("Select Course");
    
            Button btnEnroll = new Button("Enroll");
            btnEnroll.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnEnroll.setOnAction(e -> {
                Course course = courseComboBox.getValue();
                if (course != null) {
                    try (Connection conn = getConnection()) {
                        String checkSql = "SELECT * FROM enrollments WHERE student_username=? AND course_id=?";
                        PreparedStatement checkPs = conn.prepareStatement(checkSql);
                        checkPs.setString(1, selectedUser.getUsername());
                        checkPs.setString(2, course.getId());
                        ResultSet rs = checkPs.executeQuery();
                        if (rs.next()) {
                            showErrorAlert("Error", "User already enrolled in this course");
                        } else {
                            String sql = "INSERT INTO enrollments (student_username, course_id) VALUES (?, ?)";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, selectedUser.getUsername());
                            ps.setString(2, course.getId());
                            ps.executeUpdate();
                            ps.close();
                            logSystemAction(currentUser, "Enrolled user " + selectedUser.getUsername() + " in course " + course.getName());
                            showAlert("Success", "User enrolled in " + course.getName());
                            stage.close();
                        }
                        rs.close();
                        checkPs.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Error", "Select a course");
                }
            });
    
            vbox.getChildren().addAll(new Label("Select Course:"), courseComboBox, btnEnroll);
            stage.setScene(new Scene(vbox, 400, 150));
            stage.show();
        }
    
        // Load all courses for combo box
        private void loadAllCourses(ComboBox<Course> comboBox) {
            comboBox.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT id, name FROM courses WHERE status='active'";
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    comboBox.getItems().add(
                            new Course(rs.getString("id"), rs.getString("name"), "", "", "active")
                    );
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // Load all courses for instructor (by instructor username)
        private void loadAllCourses(ComboBox<Course> comboBox, String instructorUsername) {
            comboBox.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT id, name FROM courses WHERE instructor=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, instructorUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    comboBox.getItems().add(
                            new Course(rs.getString("id"), rs.getString("name"), "", "", "active")
                    );
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // Load users into table
        private void loadUsersIntoTable(TableView<User> table) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT username, email, role, active FROM users");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String status = rs.getBoolean("active") ? "Active" : "Inactive";
                    table.getItems().add(new User(rs.getString("username"), rs.getString("email"), rs.getString("role"), status));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showAddUserDialog(TableView<User> table) {
            Stage stage = new Stage();
            stage.setTitle("Add User");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            PasswordField pwField = new PasswordField();
            pwField.setPromptText("Password");
            ChoiceBox<String> roleChoice = new ChoiceBox<>();
            roleChoice.getItems().addAll("admin", "instructor", "student");
            roleChoice.setValue("student");
            Button btnAdd = new Button("Add User");
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAdd.setOnAction(e -> {
                String username = usernameField.getText();
                String email = emailField.getText();
                String pw = pwField.getText();
                String role = roleChoice.getValue();
    
                if (!username.isEmpty() && !email.isEmpty() && !pw.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String hashed = hashPassword(pw);
                        String sql = "INSERT INTO users (username, email, password, role, active) VALUES (?, ?, ?, ?, TRUE)";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, username);
                        ps.setString(2, email);
                        ps.setString(3, hashed);
                        ps.setString(4, role);
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Added user: " + username);
                        showAlert("Success", "User added");
                        loadUsersIntoTable(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing Data", "Fill all fields");
                }
            });
            vbox.getChildren().addAll(
                    new Label("Username:"), usernameField,
                    new Label("Email:"), emailField,
                    new Label("Password:"), pwField,
                    new Label("Role:"), roleChoice,
                    btnAdd
            );
            stage.setScene(new Scene(vbox, 400, 350));
            stage.show();
        }
    
        private void showUpdateUserDialog(TableView<User> table) {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a user");
                return;
            }
            Stage stage = new Stage();
            stage.setTitle("Update User");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextField emailField = new TextField(selected.getEmail());
            emailField.setPromptText("Email");
            PasswordField pwField = new PasswordField();
            pwField.setPromptText("New Password");
            ChoiceBox<String> roleChoice = new ChoiceBox<>();
            roleChoice.getItems().addAll("admin", "instructor", "student");
            roleChoice.setValue(selected.getRole());
            Button btnUpdate = new Button("Update");
            btnUpdate.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnUpdate.setOnAction(e -> {
                String email = emailField.getText();
                String pw = pwField.getText();
                String role = roleChoice.getValue();
    
                if (!email.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        if (pw.isEmpty()) {
                            String sql = "UPDATE users SET email=?, role=? WHERE username=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, email);
                            ps.setString(2, role);
                            ps.setString(3, selected.getUsername());
                            ps.executeUpdate();
                            ps.close();
                            logSystemAction(currentUser, "Updated user: " + selected.getUsername());
                        } else {
                            String sql = "UPDATE users SET email=?, password=?, role=? WHERE username=?";
                            PreparedStatement ps = conn.prepareStatement(sql);
                            ps.setString(1, email);
                            ps.setString(2, hashPassword(pw));
                            ps.setString(3, role);
                            ps.setString(4, selected.getUsername());
                            ps.executeUpdate();
                            ps.close();
                            logSystemAction(currentUser, "Updated user: " + selected.getUsername());
                        }
                        showAlert("Success", "Updated");
                        loadUsersIntoTable(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Error", "Email cannot be empty");
                }
            });
            vbox.getChildren().addAll(
                    new Label("Email:"), emailField,
                    new Label("New Password:"), pwField,
                    new Label("Role:"), roleChoice,
                    btnUpdate
            );
            stage.setScene(new Scene(vbox, 400, 350));
            stage.show();
        }
    
        private void deactivateSelectedUser(TableView<User> table) {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a user");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "UPDATE users SET active=FALSE WHERE username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getUsername());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Deactivated user: " + selected.getUsername());
                showAlert("Success", "User deactivated");
                loadUsersIntoTable(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void activateSelectedUser(TableView<User> table) {
            User selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a user");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "UPDATE users SET active=TRUE WHERE username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getUsername());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Activated user: " + selected.getUsername());
                showAlert("Success", "User activated");
                loadUsersIntoTable(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // ======================= System Logs =======================
        private void showSystemLogs() {
            String logs = getSystemLogs();
            TextArea ta = new TextArea(logs);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefSize(700, 500);
            ta.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-control-inner-background: #FFFACD;");
            Stage stage = new Stage();
            stage.setTitle("System Activity Logs");
            VBox vbox = new VBox(ta);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F5F5F5;");
            stage.setScene(new Scene(vbox, 700, 500));
            stage.show();
        }
    
        private String getSystemLogs() {
            StringBuilder logsBuilder = new StringBuilder();
            try (Connection conn = getConnection()) {
                String sql = "SELECT timestamp, username, action FROM system_logs ORDER BY timestamp DESC";
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
    
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("timestamp");
                    String user = rs.getString("username");
                    String action = rs.getString("action");
                    logsBuilder.append(ts.toString())
                            .append(" | User: ").append(user != null ? user : "System")
                            .append(" | Action: ").append(action)
                            .append("\n");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                logsBuilder.append("Error fetching logs: ").append(e.getMessage());
            }
            return logsBuilder.toString();
        }
    
        // ======================= Reports =======================
        private void showReports() {
            Stage stage = new Stage();
            stage.setTitle("Generate Reports");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E0F7FA;");
    
            // User Report Button
            Button btnUserReport = new Button("User Report");
            btnUserReport.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            btnUserReport.setOnAction(e -> generateUserReport());
    
            // Course Enrollment Report Button
            Button btnCourseReport = new Button("Course Enrollment Report");
            btnCourseReport.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            btnCourseReport.setOnAction(e -> generateCourseEnrollmentReport());
    
            vbox.getChildren().addAll(btnUserReport, btnCourseReport);
            stage.setScene(new Scene(vbox, 350, 150));
            stage.show();
        }
    
        // Generate User Report: fetch users and save as CSV
    
        // Generate Course Enrollment Report: fetch enrollments, save as CSV
        // Generate User Report: fetch users, save as CSV, and show bar chart
        private void generateUserReport() {
            String filename = "User_Report_" + System.currentTimeMillis() + ".csv";
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Username,Email,Role,Status\n");
    
            Map<String, Integer> roleCounts = new HashMap<>();
            roleCounts.put("admin", 0);
            roleCounts.put("instructor", 0);
            roleCounts.put("student", 0);
    
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT username, email, role, active FROM users");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String status = rs.getBoolean("active") ? "Active" : "Inactive";
                    String role = rs.getString("role");
                    csvContent.append(rs.getString("username")).append(",")
                            .append(rs.getString("email")).append(",")
                            .append(role).append(",")
                            .append(status).append("\n");
                    roleCounts.put(role, roleCounts.getOrDefault(role, 0) + 1);
                }
                rs.close();
                ps.close();
    
                // Save CSV
                File file = new File(filename);
                java.nio.file.Files.write(file.toPath(), csvContent.toString().getBytes());
    
                // Show chart
                showBarChart("User Roles Distribution", "Roles", "Count", roleCounts);
    
                // Open CSV
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("Report Generated", "User report saved as: " + filename);
                }
    
            } catch (Exception e) {
                showErrorAlert("Error", "Failed to generate user report: " + e.getMessage());
            }
        }
    
        // Generate Course Enrollment Report: fetch courses, save as CSV, and show bar chart
        private void generateCourseEnrollmentReport() {
            String filename = "Course_Enrollment_Report_" + System.currentTimeMillis() + ".csv";
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Course ID,Course Name,Enrolled Students\n");
    
            Map<String, Integer> courseEnrollments = new HashMap<>();
    
            try (Connection conn = getConnection()) {
                PreparedStatement psCourses = conn.prepareStatement(
                        "SELECT c.id, c.name, COUNT(e.student_username) AS enrolled_count " +
                                "FROM courses c LEFT JOIN enrollments e ON c.id=e.course_id " +
                                "GROUP BY c.id, c.name"
                );
                ResultSet rsCourses = psCourses.executeQuery();
                while (rsCourses.next()) {
                    String courseId = rsCourses.getString("id");
                    String courseName = rsCourses.getString("name");
                    int count = rsCourses.getInt("enrolled_count");
                    csvContent.append(courseId).append(",")
                            .append(courseName).append(",")
                            .append(count).append("\n");
                    courseEnrollments.put(courseName, count);
                }
                rsCourses.close();
                psCourses.close();
    
                // Save CSV
                File file = new File(filename);
                java.nio.file.Files.write(file.toPath(), csvContent.toString().getBytes());
    
                // Show chart
                showBarChart("Course Enrollment", "Courses", "Number of Students", courseEnrollments);
    
                // Open CSV
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("Report Generated", "Course enrollment report saved as: " + filename);
                }
    
            } catch (Exception e) {
                showErrorAlert("Error", "Failed to generate course enrollment report: " + e.getMessage());
            }
        }
    
        // Utility method to show bar chart in a new window
        private void showBarChart(String title, String xAxisLabel, String yAxisLabel, Map<String, Integer> data) {
            Stage chartStage = new Stage();
            chartStage.setTitle(title);
    
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel(xAxisLabel);
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel(yAxisLabel);
    
            BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
            barChart.setTitle(title);
    
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
    
            barChart.getData().add(series);
            VBox vbox = new VBox(barChart);
            vbox.setPadding(new Insets(10));
    
            Scene scene = new Scene(vbox, 600, 400);
            chartStage.setScene(scene);
            chartStage.show();
        }
        // ======================= System Settings =======================
        private void showSystemSettings() {
            Stage stage = new Stage();
            stage.setTitle("System Settings");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
    
            // Example settings:
            TextField systemNameField = new TextField();
            systemNameField.setPromptText("System Name");
            CheckBox maintenanceModeCheckbox = new CheckBox("Maintenance Mode");
            maintenanceModeCheckbox.setSelected(false);
            CheckBox registrationAllowedCheckbox = new CheckBox("Allow New Registrations");
            registrationAllowedCheckbox.setSelected(true);
    
            // Load current settings from database (if stored)
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT setting_key, setting_value FROM system_settings");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String key = rs.getString("setting_key");
                    String value = rs.getString("setting_value");
                    switch (key) {
                        case "system_name":
                            systemNameField.setText(value);
                            break;
                        case "maintenance_mode":
                            maintenanceModeCheckbox.setSelected(Boolean.parseBoolean(value));
                            break;
                        case "registration_allowed":
                            registrationAllowedCheckbox.setSelected(Boolean.parseBoolean(value));
                            break;
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
    
            Button btnSave = new Button("Save Settings");
            btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSave.setOnAction(e -> {
                String systemName = systemNameField.getText();
                boolean maintenanceMode = maintenanceModeCheckbox.isSelected();
                boolean registrationAllowed = registrationAllowedCheckbox.isSelected();
    
                try (Connection conn = getConnection()) {
                    // Save or update settings
                    saveOrUpdateSetting(conn, "system_name", systemName);
                    saveOrUpdateSetting(conn, "maintenance_mode", String.valueOf(maintenanceMode));
                    saveOrUpdateSetting(conn, "registration_allowed", String.valueOf(registrationAllowed));
                    logSystemAction(currentUser, "Updated system settings");
                    showAlert("Success", "System settings saved");
                } catch (SQLException ex) {
                    showErrorAlert("Error", ex.getMessage());
                }
            });
    
            vbox.getChildren().addAll(
                    new Label("System Name:"), systemNameField,
                    maintenanceModeCheckbox,
                    registrationAllowedCheckbox,
                    btnSave
            );
    
            stage.setScene(new Scene(vbox, 400, 250));
            stage.show();
        }
    
        private void saveOrUpdateSetting(Connection conn, String key, String value) throws SQLException {
            String checkSql = "SELECT COUNT(*) FROM system_settings WHERE setting_key=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, key);
            ResultSet rs = checkPs.executeQuery();
            rs.next();
            int count = rs.getInt(1);
            rs.close();
            checkPs.close();
    
            if (count > 0) {
                String updateSql = "UPDATE system_settings SET setting_value=? WHERE setting_key=?";
                PreparedStatement ps = conn.prepareStatement(updateSql);
                ps.setString(1, value);
                ps.setString(2, key);
                ps.executeUpdate();
                ps.close();
            } else {
                String insertSql = "INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?)";
                PreparedStatement ps = conn.prepareStatement(insertSql);
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
                ps.close();
            }
        }
    
        // ======================= Notifications =======================
        private void showManageNotifications() {
            Stage stage = new Stage();
            stage.setTitle("Manage Notifications");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
    
            ListView<String> notificationList = new ListView<>();
            loadNotifications(notificationList);
    
            TextField newNotificationField = new TextField();
            newNotificationField.setPromptText("New notification message");
    
            Button btnAdd = new Button("Add Notification");
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAdd.setOnAction(e -> {
                String message = newNotificationField.getText();
                if (!message.trim().isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String sql = "INSERT INTO notifications (message, created_at) VALUES (?, NOW())";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, message);
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Added notification");
                        showAlert("Success", "Notification added");
                        loadNotifications(notificationList);
                        newNotificationField.clear();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Error", "Enter a message");
                }
            });
    
            Button btnDelete = new Button("Delete Selected");
            btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            btnDelete.setOnAction(e -> {
                String selectedMsg = notificationList.getSelectionModel().getSelectedItem();
                if (selectedMsg != null) {
                    try (Connection conn = getConnection()) {
                        String sql = "DELETE FROM notifications WHERE message=? LIMIT 1";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, selectedMsg);
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Deleted notification");
                        showAlert("Success", "Notification deleted");
                        loadNotifications(notificationList);
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Error", "Select a notification to delete");
                }
            });
    
            vbox.getChildren().addAll(
                    new Label("Existing Notifications:"), notificationList,
                    newNotificationField,
                    new HBox(10, btnAdd, btnDelete)
            );
    
            stage.setScene(new Scene(vbox, 500, 400));
            stage.show();
        }
    
        private void loadNotifications(ListView<String> listView) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT message FROM notifications ORDER BY created_at DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(rs.getString("message"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // ======================= Discussion Forums =======================
        private void showDiscussionForums() {
            showInstructorForum();
        }
    
        // ======================= Instructor Dashboard =======================
        // Return a Node (e.g., VBox) representing the instructor dashboard
        private Node showInstructorDashboard(String username) {
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E8F5E9;");
            Button btnManageCourses = new Button("Manage Courses");
            btnManageCourses.setOnAction(e -> showManageCoursesPanel());
            Button btnUploadContent = new Button("Upload Course Content");
            btnUploadContent.setOnAction(e -> showUploadContentDialog(username));
            Button btnAssessments = new Button("Manage Assessments & Quizzes");
            btnAssessments.setOnAction(e -> showManageAssessments());
            Button btnMultiple = new Button("show multiple choice");
            btnMultiple.setOnAction(e -> showManageMultiple());
            Button btnGrade = new Button("Grade Student Submissions");
            btnGrade.setOnAction(e -> showGradeSubmissions());
            Button btnProgress = new Button("View Student Progress");
            btnProgress.setOnAction(e -> showStudentProgress());
            Button btnDiscussion = new Button("Manage Discussion Forums");
            btnDiscussion.setOnAction(e -> showDiscussionForums());
            Button btnEnrolled = new Button("View Enrolled Students");
            btnEnrolled.setOnAction(e -> showEnrolledStudents());
            Button btnCertificates = new Button("Generate Certificates");
            btnCertificates.setOnAction(e -> showGenerateCertificates());
    
            vbox.getChildren().addAll(
                    new Label("Main Functionalities:"), btnManageCourses, btnUploadContent, btnMultiple, btnGrade, btnProgress, btnDiscussion, btnEnrolled, btnCertificates
            );
            return vbox;
        }

        private void showStudentProgress() {
            // Create a new window
            Stage stage = new Stage();
            stage.setTitle("Student Progress");

            VBox vbox = new VBox(15);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E0F7FA;");

            // Fetch list of students
            List<String> studentUsernames = new ArrayList<>();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE role='student'");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    studentUsernames.add(rs.getString("username"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
                return;
            }

            // ComboBox to select student
            ComboBox<String> studentCombo = new ComboBox<>();
            studentCombo.getItems().addAll(studentUsernames);
            studentCombo.setPromptText("Select Student");

            // Progress bar and indicator for overall progress
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(400);
            ProgressIndicator progressIndicator = new ProgressIndicator(0);
            progressIndicator.setPrefSize(50, 50);

            HBox progressBox = new HBox(15, new Label("Overall Progress:"), progressBar, progressIndicator);
            progressBox.setAlignment(Pos.CENTER_LEFT);

            // Table for detailed course progress
            TableView<StudentProgress> table = new TableView<>();
            TableColumn<StudentProgress, String> courseCol = new TableColumn<>("Course");
            courseCol.setCellValueFactory(new PropertyValueFactory<>("courseId"));
            TableColumn<StudentProgress, Integer> progressCol = new TableColumn<>("Progress (%)");
            progressCol.setCellValueFactory(new PropertyValueFactory<>("progressPercentage"));
            table.getColumns().addAll(courseCol, progressCol);
            table.setPrefHeight(300);

            Button btnShow = new Button("Show Progress");
            btnShow.setOnAction(e -> {
                String selectedStudent = studentCombo.getValue();
                if (selectedStudent != null) {
                    double overallProgress = getStudentProgressPercentage(selectedStudent);
                    progressBar.setProgress(overallProgress);
                    progressIndicator.setProgress(overallProgress);
                    loadStudentProgress(table, selectedStudent);
                } else {
                    showErrorAlert("Validation", "Please select a student");
                }
            });

            vbox.getChildren().addAll(new Label("Select a Student:"), studentCombo, progressBox, new Label("Progress Details:"), table, btnShow);

            Scene scene = new Scene(vbox, 700, 500);
            stage.setScene(scene);
            stage.show();
        }
        // Method to show the dialog for adding multiple questions
        private void showManageMultiple() {
            // Open dialog for adding multiple questions
            showAddMultipleQuestionsDialog();
        }
    
        // Dialog to add multiple questions with options
        private void showAddMultipleQuestionsDialog() {
            Stage stage = new Stage();
            stage.setTitle("Add Multiple Choice Questions");
    
            VBox root = new VBox(10);
            root.setPadding(new Insets(15));
            root.setStyle("-fx-background-color: #F0F8FF;");
    
            // Course selection ComboBox
            ComboBox<Course> courseComboBox = new ComboBox<>();
            loadAllCourses(courseComboBox); // Load courses taught by instructor
            courseComboBox.setPromptText("Select Course");
    
            // Question TextArea
            TextArea questionArea = new TextArea();
            questionArea.setPromptText("Enter the question here...");
            questionArea.setPrefRowCount(3);
    
            // Checkbox for multiple answers
            CheckBox multiAnswerChk = new CheckBox("Allow multiple correct answers");
    
            // Options container
            VBox optionsBox = new VBox(10);
            final int initialOptionsCount = 4;
            final TextField[][] optionFields = {new TextField[initialOptionsCount]};
            final CheckBox[][] correctChecks = {new CheckBox[initialOptionsCount]};
    
            for (int i=0; i<initialOptionsCount; i++) {
                optionFields[0][i] = new TextField();
                optionFields[0][i].setPromptText("Option " + (i+1));
                correctChecks[0][i] = new CheckBox("Correct");
                HBox hbox = new HBox(10, optionFields[0][i], correctChecks[0][i]);
                optionsBox.getChildren().add(hbox);
            }
    
            // Button to add more options
            Button btnAddOption = new Button("Add Another Option");
            btnAddOption.setOnAction(e -> {
                int index = optionFields[0].length;
                // Expand arrays
                optionFields[0] = java.util.Arrays.copyOf(optionFields[0], index + 1);
                correctChecks[0] = java.util.Arrays.copyOf(correctChecks[0], index + 1);
                optionFields[0][index] = new TextField();
                optionFields[0][index].setPromptText("Option " + (index+1));
                correctChecks[0][index] = new CheckBox("Correct");
                HBox hbox = new HBox(10, optionFields[0][index], correctChecks[0][index]);
                optionsBox.getChildren().add(hbox);
            });
    
            // Save button
            Button btnSave = new Button("Save Question");
            btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSave.setOnAction(e -> {
                Course selectedCourse = courseComboBox.getValue(); // get selected course
                String questionText = questionArea.getText().trim();
                boolean isMultipleAnswer = multiAnswerChk.isSelected();
    
                if (selectedCourse == null) {
                    showErrorAlert("Validation Error", "Please select a course");
                    return;
                }
    
                if (questionText.isEmpty()) {
                    showErrorAlert("Validation Error", "Question text cannot be empty");
                    return;
                }
    
                // Collect options
                java.util.List<MCQOption> options = new java.util.ArrayList<>();
                for (int i = 0; i< optionFields[0].length; i++) {
                    String optText = optionFields[0][i].getText().trim();
                    if (optText.isEmpty()) {
                        showErrorAlert("Validation Error", "All options must be filled");
                        return;
                    }
                    boolean isCorrect = correctChecks[0][i].isSelected();
                    options.add(new MCQOption(optText, isCorrect));
                }
    
                // Save question with course association
                saveMCQQuestionToCourse(selectedCourse, questionText, options, isMultipleAnswer);
                showAlert("Success", "Question sent to course: " + selectedCourse.getName());
                stage.close();
            });
    
            root.getChildren().addAll(
                    new Label("Select Course:"), courseComboBox,
                    new Label("Question:"), questionArea,
                    multiAnswerChk,
                    new Label("Options:"), optionsBox,
                    btnAddOption,
                    btnSave
            );
    
            Scene scene = new Scene(root, 500, 600);
            stage.setScene(scene);
            stage.show();
        }
    
        private void saveMCQQuestionToCourse(Course selectedCourse, String questionText, List<MCQOption> options, boolean isMultipleAnswer) {
        }
    
        // Method to save question and options to the database
        private void saveMCQQuestion(String questionText, java.util.List<MCQOption> options, boolean isMultipleAnswer) {
            try (Connection conn = getConnection()) {
                // Insert question
                String sqlQ = "INSERT INTO questions (question_text, is_multiple_answer) VALUES (?, ?)";
                PreparedStatement psQ = conn.prepareStatement(sqlQ, Statement.RETURN_GENERATED_KEYS);
                psQ.setString(1, questionText);
                psQ.setBoolean(2, isMultipleAnswer);
                psQ.executeUpdate();
    
                // Get generated question_id
                ResultSet rs = psQ.getGeneratedKeys();
                int questionId = -1;
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }
                rs.close();
                psQ.close();
    
                // Insert options
                String sqlOpt = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
                PreparedStatement psOpt = conn.prepareStatement(sqlOpt);
                for (MCQOption opt : options) {
                    psOpt.setInt(1, questionId);
                    psOpt.setString(2, opt.getOptionText());
                    psOpt.setBoolean(3, opt.isCorrect());
                    psOpt.executeUpdate();
                }
                psOpt.close();
    
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
        // ======================= Course Management for Instructor =======================
        private void showManageCoursesPanel() {
            Stage stage = new Stage();
            stage.setTitle("Manage Courses");
            BorderPane pane = new BorderPane();
    
            TableView<Course> table = new TableView<>();
            TableColumn<Course, String> colName = new TableColumn<>("Name");
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            TableColumn<Course, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
            TableColumn<Course, String> colInstructor = new TableColumn<>("Instructor");
            colInstructor.setCellValueFactory(new PropertyValueFactory<>("instructor"));
            TableColumn<Course, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            table.getColumns().addAll(colName, colDesc, colInstructor, colStatus);
            loadCourses(table);
    
            Button btnAdd = new Button("Add Course");
            btnAdd.setOnAction(e -> showAddCourseDialog(table));
            Button btnUpdate = new Button("Update Course");
            btnUpdate.setOnAction(e -> showUpdateCourseDialog(table));
            Button btnDelete = new Button("Delete Course");
            btnDelete.setOnAction(e -> deleteCourse(table));
            Button btnActivate = new Button("Activate");
            btnActivate.setOnAction(e -> activateCourse(table));
            Button btnDeactivate = new Button("Deactivate");
            btnDeactivate.setOnAction(e -> deactivateCourse(table));
    
            HBox hb = new HBox(10, btnAdd, btnUpdate, btnDelete, btnActivate, btnDeactivate);
            hb.setPadding(new Insets(10));
            pane.setCenter(table);
            pane.setBottom(hb);
    
            Scene scene = new Scene(pane, 800, 600);
            stage.setScene(scene);
            stage.show();
        }
    
        private void loadCourses(TableView<Course> table) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT id, name, description, instructor, status FROM courses");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    table.getItems().add(new Course(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("instructor"),
                            rs.getString("status")
                    ));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showAddCourseDialog(TableView<Course> table) {
            Stage stage = new Stage();
            stage.setTitle("Add Course");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextField nameField = new TextField();
            nameField.setPromptText("Course Name");
            TextArea descArea = new TextArea();
            descArea.setPromptText("Description");
            TextField instructorField = new TextField();
            instructorField.setPromptText("Instructor Username");
            Button btnAdd = new Button("Add Course");
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAdd.setOnAction(e -> {
                String name = nameField.getText();
                String desc = descArea.getText();
                String instructor = instructorField.getText();
                if (!name.isEmpty() && !desc.isEmpty() && !instructor.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String id = "C" + System.currentTimeMillis();
                        String sql = "INSERT INTO courses (id, name, description, instructor, status) VALUES (?, ?, ?, ?, 'active')";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, id);
                        ps.setString(2, name);
                        ps.setString(3, desc);
                        ps.setString(4, instructor);
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Created course: " + name);
                        showAlert("Success", "Course added");
                        loadCourses(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing Data", "Fill all fields");
                }
            });
            vbox.getChildren().addAll(
                    new Label("Course Name:"), nameField,
                    new Label("Description:"), descArea,
                    new Label("Instructor Username:"), instructorField,
                    btnAdd
            );
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void showUpdateCourseDialog(TableView<Course> table) {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a course");
                return;
            }
            Stage stage = new Stage();
            stage.setTitle("Update Course");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextField nameField = new TextField(selected.getName());
            nameField.setPromptText("Course Name");
            TextArea descArea = new TextArea(selected.getDescription());
            descArea.setPromptText("Description");
            TextField instructorField = new TextField(selected.getInstructor());
            instructorField.setPromptText("Instructor");
            Button btnUpdate = new Button("Update");
            btnUpdate.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnUpdate.setOnAction(e -> {
                String name = nameField.getText();
                String desc = descArea.getText();
                String instructor = instructorField.getText();
                if (!name.isEmpty() && !desc.isEmpty() && !instructor.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String sql = "UPDATE courses SET name=?, description=?, instructor=? WHERE id=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, name);
                        ps.setString(2, desc);
                        ps.setString(3, instructor);
                        ps.setString(4, selected.getId());
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Updated course: " + name);
                        showAlert("Success", "Course updated");
                        loadCourses(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing Data", "Fill all");
                }
            });
            vbox.getChildren().addAll(
                    new Label("Course Name:"), nameField,
                    new Label("Description:"), descArea,
                    new Label("Instructor:"), instructorField,
                    btnUpdate
            );
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void deleteCourse(TableView<Course> table) {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a course");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM courses WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getId());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Deleted course: " + selected.getName());
                showAlert("Success", "Course deleted");
                loadCourses(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void activateCourse(TableView<Course> table) {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a course");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "UPDATE courses SET status='active' WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getId());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Activated course: " + selected.getName());
                showAlert("Success", "Course activated");
                loadCourses(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void deactivateCourse(TableView<Course> table) {
            Course selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select a course");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "UPDATE courses SET status='inactive' WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getId());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Deactivated course: " + selected.getName());
                showAlert("Success", "Course deactivated");
                loadCourses(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
    
    
        // ======================= Upload Content for Instructor =======================
        private void showUploadContentDialog(String instructorUsername) {
            Stage stage = new Stage();
            stage.setTitle("Upload Course Content");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
    
            ComboBox<Course> courseComboBox = new ComboBox<>();
            loadAllCourses(courseComboBox, instructorUsername);
            courseComboBox.setPromptText("Select Course");
    
            TextField titleField = new TextField();
            titleField.setPromptText("Content Title");
            TextArea descArea = new TextArea();
            descArea.setPromptText("Description");
            TextField filePathField = new TextField();
            filePathField.setPromptText("Selected file");
            filePathField.setEditable(false); // user shouldn't edit directly
    
            Button btnBrowse = new Button("Browse");
            final File[] selectedFile = new File[1]; // hold selected file
            btnBrowse.setOnAction(e -> {
                FileChooser fc = new FileChooser();
                File file = fc.showOpenDialog(stage);
                if (file != null) {
                    selectedFile[0] = file; // store selected file
                    filePathField.setText(file.getName()); // show filename only
                }
            });
    
            Button btnUpload = new Button("Upload");
            btnUpload.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnUpload.setOnAction(e -> {
                Course course = courseComboBox.getValue();
                String title = titleField.getText();
                String desc = descArea.getText();
                // Make sure a file was selected
                if (course != null && !title.isEmpty() && selectedFile[0] != null) {
                    try (Connection conn = getConnection()) {
                        // Define an upload directory (ensure this exists)
                        String uploadDirPath = "course_content_files";
                        File uploadDir = new File(uploadDirPath);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }
    
                        // Copy file to upload directory
                        String filename = System.currentTimeMillis() + "_" + selectedFile[0].getName();
                        File destFile = new File(uploadDir, filename);
                        java.nio.file.Files.copy(selectedFile[0].toPath(), destFile.toPath());
    
                        // Save filename (or relative path) to database
                        String sql = "INSERT INTO course_content (course_id, title, description, file_path) VALUES (?, ?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, course.getId());
                        ps.setString(2, title);
                        ps.setString(3, desc);
                        ps.setString(4, filename); // store just filename or relative path
                        ps.executeUpdate();
                        ps.close();
    
                        logSystemAction(currentUser, "Uploaded content to course: " + course.getName() + " - " + title);
                        showAlert("Success", "Content uploaded");
                        stage.close();
                    } catch (Exception ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing", "Fill all fields and select a file");
                }
            });
    
            vbox.getChildren().addAll(
                    new Label("Select Course:"), courseComboBox,
                    new Label("Content Title:"), titleField,
                    new Label("Description:"), descArea,
                    new HBox(5, new Label("File:"), filePathField, btnBrowse),
                    btnUpload
            );
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        // ======================= Manage Assessments =======================
        private void showManageAssessments() {
            Stage stage = new Stage();
            stage.setTitle("Manage Assessments");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TableView<Assessment> table = new TableView<>();
            TableColumn<Assessment, String> colTitle = new TableColumn<>("Title");
            colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
            TableColumn<Assessment, String> colDesc = new TableColumn<>("Description");
            colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
            TableColumn<Assessment, String> colCourse = new TableColumn<>("Course");
            colCourse.setCellValueFactory(new PropertyValueFactory<>("courseId")); // or get course name
            table.getColumns().addAll(colTitle, colDesc, colCourse);
            loadAssessments(table);
    
            Button btnAdd = new Button("Add Assessment");
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAdd.setOnAction(e -> {
                showAddAssessmentDialog(table);
                loadAssessments(table); // refresh after add
            });
            Button btnUpdate = new Button("Update Assessment");
            btnUpdate.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnUpdate.setOnAction(e -> {
                showUpdateAssessmentDialog(table);
                loadAssessments(table); // refresh after update
            });
            Button btnDelete = new Button("Delete Assessment");
            btnDelete.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            btnDelete.setOnAction(e -> {
                deleteAssessment(table);
                loadAssessments(table); // refresh after delete
            });
    
            HBox hb = new HBox(10, btnAdd, btnUpdate, btnDelete);
            hb.setPadding(new Insets(10));
            vbox.getChildren().addAll(new Label("Assessments:"), table, hb);
            Scene scene = new Scene(vbox, 600, 500);
            stage.setScene(scene);
            stage.show();
        }
    
        private void loadAssessments(TableView<Assessment> table) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT id, course_id, title, description FROM assessments");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    table.getItems().add(new Assessment(
                            rs.getString("id"),
                            rs.getString("course_id"),
                            rs.getString("title"),
                            rs.getString("description")
                    ));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showAddAssessmentDialog(TableView<Assessment> table) {
            Stage stage = new Stage();
            stage.setTitle("Add Assessment");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ComboBox<Course> courseBox = new ComboBox<>();
            loadAllCourses(courseBox);
            courseBox.setPromptText("Select Course");
            TextField titleField = new TextField();
            titleField.setPromptText("Title");
            TextArea descArea = new TextArea();
            descArea.setPromptText("Description");
            Button btnAdd = new Button("Add");
            btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnAdd.setOnAction(e -> {
                Course course = courseBox.getValue();
                String title = titleField.getText();
                String desc = descArea.getText();
                if (course != null && !title.isEmpty() && !desc.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String id = "A" + System.currentTimeMillis();
                        PreparedStatement ps = conn.prepareStatement("INSERT INTO assessments (id, course_id, title, description) VALUES (?, ?, ?, ?)");
                        ps.setString(1, id);
                        ps.setString(2, course.getId());
                        ps.setString(3, title);
                        ps.setString(4, desc);
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Added assessment: " + title + " for course: " + course.getName());
                        showAlert("Success", "Assessment added");
                        loadAssessments(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing Data", "Fill all");
                }
            });
            vbox.getChildren().addAll(
                    new Label("Select Course:"), courseBox,
                    new Label("Title:"), titleField,
                    new Label("Description:"), descArea,
                    btnAdd
            );
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void showUpdateAssessmentDialog(TableView<Assessment> table) {
            Assessment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select assessment");
                return;
            }
            Stage stage = new Stage();
            stage.setTitle("Update Assessment");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextField titleField = new TextField(selected.getTitle());
            titleField.setPromptText("Title");
            TextArea descArea = new TextArea(selected.getDescription());
            descArea.setPromptText("Description");
            Button btnUpdate = new Button("Update");
            btnUpdate.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnUpdate.setOnAction(e -> {
                String title = titleField.getText();
                String desc = descArea.getText();
                if (!title.isEmpty() && !desc.isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String sql = "UPDATE assessments SET title=?, description=? WHERE id=?";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, title);
                        ps.setString(2, desc);
                        ps.setString(3, selected.getId());
                        ps.executeUpdate();
                        ps.close();
                        logSystemAction(currentUser, "Updated assessment: " + title);
                        showAlert("Success", "Assessment updated");
                        loadAssessments(table);
                        stage.close();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                } else {
                    showErrorAlert("Missing Data", "Fill all");
                }
            });
            vbox.getChildren().addAll(new Label("Title:"), titleField, new Label("Description:"), descArea, btnUpdate);
            stage.setScene(new Scene(vbox, 400, 250));
            stage.show();
        }
    
        private void deleteAssessment(TableView<Assessment> table) {
            Assessment selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showErrorAlert("Error", "Select assessment");
                return;
            }
            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM assessments WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, selected.getId());
                ps.executeUpdate();
                ps.close();
                logSystemAction(currentUser, "Deleted assessment: " + selected.getTitle());
                showAlert("Success", "Assessment deleted");
                loadAssessments(table);
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // ======================= Grade Submissions =======================
        private void showGradeSubmissions() {
            Stage stage = new Stage();
            stage.setTitle("Grade Student Submissions");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TableView<Submission> table = new TableView<>();
            loadSubmissions(table);
            TableColumn<Submission, String> colStudent = new TableColumn<>("Student");
            colStudent.setCellValueFactory(new PropertyValueFactory<>("studentUsername"));
            TableColumn<Submission, String> colAssessment = new TableColumn<>("Assessment");
            colAssessment.setCellValueFactory(new PropertyValueFactory<>("assessmentId"));
            TableColumn<Submission, String> colGrade = new TableColumn<>("Grade");
            colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
    
            table.getColumns().addAll(colStudent, colAssessment, colGrade);
            table.setPrefHeight(400);
    
            Button btnGrade = new Button("Grade Selected");
            btnGrade.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnGrade.setOnAction(e -> {
                Submission selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showGradeDialog(selected);
                } else {
                    showErrorAlert("Error", "Select a submission");
                }
            });
            vbox.getChildren().addAll(new Label("Submissions:"), table, btnGrade);
            stage.setScene(new Scene(vbox, 600, 500));
            stage.show();
        }
        private void loadSubmissions(TableView<Submission> table) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT id, student_username, assessment_id, submission_text, grade FROM submissions");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    table.getItems().add(new Submission(
                            rs.getString("id"),
                            rs.getString("student_username"),
                            rs.getString("assessment_id"),
                            rs.getString("submission_text"),
                            rs.getString("grade")
                    ));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showGradeDialog(Submission submission) {
            Stage stage = new Stage();
            stage.setTitle("Grade Submission");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            Label lblStudent = new Label("Student: " + submission.getStudentUsername());
            Label lblAssessment = new Label("Assessment ID: " + submission.getAssessmentId());
            TextField gradeField = new TextField();
            gradeField.setPromptText("Enter grade");
            gradeField.setText(submission.getGrade() != null ? submission.getGrade() : "");
            Button btnSave = new Button("Save Grade");
            btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSave.setOnAction(e -> {
                String grade = gradeField.getText();
                try (Connection conn = getConnection()) {
                    String sql = "UPDATE submissions SET grade=? WHERE id=?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, grade);
                    ps.setString(2, submission.getId());
                    ps.executeUpdate();
                    ps.close();
                    logSystemAction(currentUser, "Graded submission: " + submission.getId() + " - Grade: " + grade);
                    showAlert("Success", "Grade saved");
                    stage.close();
                } catch (SQLException ex) {
                    showErrorAlert("Error", ex.getMessage());
                }
            });
            vbox.getChildren().addAll(lblStudent, lblAssessment, new Label("Grade:"), gradeField, btnSave);
            stage.setScene(new Scene(vbox, 400, 200));
            stage.show();
        }
    
    
    
        // ======================= Student Progress =======================
        private void showProgressForStudent(String studentUsername) {
            Stage stage = new Stage();
            stage.setTitle("Progress for " + studentUsername);

            VBox vbox = new VBox(15);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E0F7FA;");

            // Fetch overall progress
            double overallProgress = getStudentProgressPercentage(studentUsername);

            ProgressBar progressBar = new ProgressBar(overallProgress);
            progressBar.setPrefWidth(400);
            ProgressIndicator progressIndicator = new ProgressIndicator(overallProgress);
            progressIndicator.setPrefSize(50, 50);

            HBox progressBox = new HBox(15, new Label("Overall Progress:"), progressBar, progressIndicator);
            progressBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Detailed progress per course
            TableView<StudentProgress> table = new TableView<>();
            loadStudentProgress(table, studentUsername);

            TableColumn<StudentProgress, String> colCourse = new TableColumn<>("Course");
            colCourse.setCellValueFactory(new PropertyValueFactory<>("courseId"));

            TableColumn<StudentProgress, Integer> colProgress = new TableColumn<>("Progress (%)");
            colProgress.setCellValueFactory(new PropertyValueFactory<>("progressPercentage"));

            table.getColumns().addAll(colCourse, colProgress);
            table.setPrefHeight(300);

            Button btnRefresh = new Button("Refresh");
            btnRefresh.setOnAction(e -> {
                double newProgress = getStudentProgressPercentage(studentUsername);
                progressBar.setProgress(newProgress);
                progressIndicator.setProgress(newProgress);
                loadStudentProgress(table, studentUsername);
            });

            vbox.getChildren().addAll(progressBox, new Label("Progress Details:"), table, btnRefresh);

            Scene scene = new Scene(vbox, 700, 500);
            stage.setScene(scene);
            stage.show();
        }
        // New method to display progress bars for the student
       
        // Helper method to load detailed progress
        private void loadStudentProgress(TableView<StudentProgress> table, String studentUsername) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT c.id, c.name FROM courses c "
                        + "JOIN enrollments e ON c.id=e.course_id "
                        + "WHERE e.student_username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, studentUsername);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String courseId = rs.getString("id");
                    String courseName = rs.getString("name");
                    int progress = (int) (getStudentProgressPercentage(studentUsername) * 100);
                    // Alternatively, compute per course for more accuracy
                    table.getItems().add(new StudentProgress(studentUsername, courseId, progress));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }

        // Placeholder for calculating overall progress
        private double getStudentOverallProgress(String studentUsername) {
            // Implement logic to compute overall progress, e.g., average of course progresses
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT AVG(progress_percentage) FROM student_progress WHERE student_username=?");
                ps.setString(1, studentUsername);
                ResultSet rs = ps.executeQuery();
                double avg = 0;
                if (rs.next()) {
                    avg = rs.getDouble(1) / 100.0; // converting percentage to 0-1
                }
                rs.close();
                ps.close();
                return avg;
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
                return 0;
            }
        }

        private void loadStudentProgress(TableView<StudentProgress> table) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT student_username, course_id, progress_percentage FROM student_progress"
                );
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    table.getItems().add(new StudentProgress(
                            rs.getString("student_username"),
                            rs.getString("course_id"),
                            rs.getInt("progress_percentage")
                    ));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
        // ======================= Discussion Forum =======================
        private void showInstructorForum() {
            Stage stage = new Stage();
            stage.setTitle("Manage Discussion Forums");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E8F5E9;");
            ListView<String> forums = new ListView<>();
            loadForums(forums);
            forums.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-size: 13px;");
            Button btnOpen = new Button("Open Selected");
            btnOpen.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnOpen.setOnAction(e -> {
                String selected = forums.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showDiscussionForum(selected);
                } else {
                    showErrorAlert("Error", "Select a forum");
                }
            });
            Button btnCreate = new Button("Create New");
            btnCreate.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnCreate.setOnAction(e -> showCreateForumDialog(forums));
            vbox.getChildren().addAll(forums, new HBox(10, btnOpen, btnCreate));
            Scene scene = new Scene(vbox, 400, 300);
            stage.setScene(scene);
            stage.show();
        }
    
        private void loadForums(ListView<String> listView) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT name FROM discussion_forums");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(rs.getString("name"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showDiscussionForum(String forumName) {
            Stage stage = new Stage();
            stage.setTitle("Forum: " + forumName);
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ListView<String> messages = new ListView<>();
            loadForumMessages(forumName, messages);
            messages.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-size: 13px;");
            TextField msgField = new TextField();
            msgField.setPromptText("Type message");
            Button btnSend = new Button("Send");
            btnSend.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSend.setOnAction(e -> {
                String msg = msgField.getText();
                if (!msg.trim().isEmpty()) {
                    try (Connection conn = getConnection()) {
                        String sql = "INSERT INTO forum_messages (forum_name, message, sender) VALUES (?, ?, ?)";
                        PreparedStatement ps = conn.prepareStatement(sql);
                        ps.setString(1, forumName);
                        ps.setString(2, msg);
                        ps.setString(3, "Instructor");
                        ps.executeUpdate();
                        ps.close();
                        loadForumMessages(forumName, messages);
                        logSystemAction(currentUser, "Posted message in forum: " + forumName);
                        msgField.clear();
                    } catch (SQLException ex) {
                        showErrorAlert("Error", ex.getMessage());
                    }
                }
            });
            vbox.getChildren().addAll(messages, new HBox(10, msgField, btnSend));
            stage.setScene(new Scene(vbox, 500, 400));
            stage.show();
        }
    
        private void loadForumMessages(String forumName, ListView<String> messages) {
            messages.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT sender, message FROM forum_messages WHERE forum_name=?");
                ps.setString(1, forumName);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    messages.getItems().add(rs.getString("sender") + ": " + rs.getString("message"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showCreateForumDialog(ListView<String> forums) {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setTitle("Create Forum");
            dlg.setHeaderText("Enter forum name");
            Optional<String> res = dlg.showAndWait();
            res.ifPresent(name -> {
                try (Connection conn = getConnection()) {
                    String sql = "INSERT INTO discussion_forums (name) VALUES (?)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, name);
                    ps.executeUpdate();
                    ps.close();
                    logSystemAction(currentUser, "Created forum: " + name);
                    loadForums(forums);
                } catch (SQLException e) {
                    showErrorAlert("Error", e.getMessage());
                }
            });
        }
    
        // ======================= Enrolled Students =======================
        private void showEnrolledStudents() {
            Stage stage = new Stage();
            stage.setTitle("Enrolled Students");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ListView<String> students = new ListView<>();
            loadEnrolledStudents(students);
            students.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            vbox.getChildren().addAll(new Label("Students enrolled:"), students);
            stage.setScene(new Scene(vbox, 400, 300));
            stage.show();
        }
    
        private void loadEnrolledStudents(ListView<String> listView) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT username FROM users WHERE role='student'");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(rs.getString("username"));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // ======================= Generate Certificates =======================
        private void showGenerateCertificates() {
            // Prompt for the username
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Generate Certificate");
            dialog.setHeaderText("Enter Student Username");
            Optional<String> result = dialog.showAndWait();

            result.ifPresent(username -> {
                try (Connection conn = getConnection()) {
                    // Check if user exists and is a student
                    PreparedStatement ps = conn.prepareStatement("SELECT username, email FROM users WHERE username=? AND role='student'");
                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String studentName = rs.getString("username");
                        String email = rs.getString("email");
                        String courseName = "Sample Course"; // Replace with actual course info if needed
                        String dateStr = java.time.LocalDate.now().toString();

                        // Create the certificate text
                        String certificateText = generateCertificateText(studentName, courseName, dateStr);

                        // Display in an Alert with a TextArea
                        showCertificateInDialog(certificateText);

                        logSystemAction(currentUser, "Generated certificate for user: " + username);
                    } else {
                        showErrorAlert("Error", "Student not found");
                    }

                    rs.close();
                    ps.close();

                } catch (Exception e) {
                    showErrorAlert("Error", "Failed to generate certificate: " + e.getMessage());
                }
            });
        }

        // Helper method to create the certificate string
        private String generateCertificateText(String username, String courseName, String date) {
            StringBuilder sb = new StringBuilder();
            sb.append("=====================================\n");
            sb.append("        Certificate of Completion    \n");
            sb.append("=====================================\n\n");
            sb.append("This certifies that\n\n");
            sb.append("        ").append(username).append("\n\n");
            sb.append("has successfully completed the course\n\n");
            sb.append("        ").append(courseName).append("\n\n");
            sb.append("Date of Completion: ").append(date).append("\n\n");
            sb.append("=====================================\n");
            sb.append("        Congratulations!             \n");
            sb.append("=====================================\n");
            return sb.toString();
        }

        // Helper method to show the certificate in a dialog with a TextArea
        private void showCertificateInDialog(String certificateText) {
            TextArea textArea = new TextArea(certificateText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setFont(Font.font("Monospaced", 12));

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Certificate");
            dialog.getDialogPane().setContent(textArea);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        }

        // ======================= Student Dashboard =======================
        private Node showStudentDashboard(String username) {
            Stage stage = new Stage();
            stage.setTitle("Student Dashboard - " + username);
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E0F7FA;");
            Button btnMyCourses = new Button("View Enrolled Courses");
            btnMyCourses.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnMyCourses.setOnAction(e -> showEnrolledCourses(username));
            Button btnMaterials = new Button("Access Course Materials");
            btnMaterials.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnMaterials.setOnAction(e -> showCourseMaterials(username));
            Button btnAssessments = new Button("Take Quizzes & Assessments");
            btnAssessments.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnAssessments.setOnAction(e -> showAssessmentsToTake(username));
            Button btnSubmissions = new Button("Submit Assignments");
            btnSubmissions.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnSubmissions.setOnAction(e -> showSubmitAssignments(username));
            Button btnGrades = new Button("View Grades & Progress");
            btnGrades.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnGrades.setOnAction(e -> showGradesAndProgress(username));
            Button btnForum = new Button("Participate in Discussions");
            btnForum.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnForum.setOnAction(e -> showStudentDiscussionForum(username));
            Button btnCertificates = new Button("Download Certificates");
            btnCertificates.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            btnCertificates.setOnAction(e -> showCertificates(username));
            Button btnProfile = new Button("Update Profile");
            btnProfile.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
            Button btnTakeInstructorQuestions = new Button("Answer Instructor Questions");
            btnTakeInstructorQuestions.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnTakeInstructorQuestions.setOnAction(e -> showTakeInstructorQuestions());
            vbox.getChildren().add(btnTakeInstructorQuestions);
            btnProfile.setOnAction(e -> showUpdateProfile(username));
    
            vbox.getChildren().addAll(
                    new Label("Main Functionalities:"), btnMyCourses, btnMaterials, btnAssessments, btnSubmissions, btnGrades, btnForum, btnCertificates, btnProfile
            );
            Scene scene = new Scene(vbox, 400, 600);
            stage.setScene(scene);
            stage.show();
            return null;
        }

        private void showTakeInstructorQuestions() {
        }

        private void showEnrolledCourses(String username) {
            Stage stage = new Stage();
            stage.setTitle("Enrolled Courses");
            ListView<Course> listView = new ListView<>();
            loadEnrolledCourses(listView, username);
            listView.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            VBox vbox = new VBox(10, new Label("Enrolled Courses:"), listView);
            vbox.setPadding(new Insets(10));
            stage.setScene(new Scene(vbox, 600, 400));
            stage.show();
        }
    
        private void loadEnrolledCourses(ListView<Course> listView, String username) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT c.id, c.name, c.description, c.instructor, c.status "
                        + "FROM courses c JOIN enrollments e ON c.id=e.course_id WHERE e.student_username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(
                            new Course(rs.getString("id"), rs.getString("name"), rs.getString("description"), rs.getString("instructor"), rs.getString("status"))
                    );
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
    
        private void showCourseMaterials(String username) {
            Stage stage = new Stage();
            stage.setTitle("Access Course Materials");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ListView<Course> courseList = new ListView<>();
            loadEnrolledCourses(courseList, username);
            Button btnView = new Button("View Materials");
            btnView.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnView.setOnAction(e -> {
                Course course = courseList.getSelectionModel().getSelectedItem();
                if (course != null) {
                    showCourseContent(course);
                } else {
                    showErrorAlert("Error", "Select a course");
                }
            });
            vbox.getChildren().addAll(new Label("Enrolled Courses:"), courseList, btnView);
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void showCourseContent(Course course) {
            Stage stage = new Stage();
            stage.setTitle("Course Materials - " + course.getName());
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
    
            // ListView to display materials
            ListView<CourseMaterial> contentList = new ListView<>();
            loadCourseMaterials(course, contentList);
    
            // Button to open selected file
            Button btnOpenFile = new Button("Open Selected File");
            btnOpenFile.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnOpenFile.setOnAction(e -> {
                CourseMaterial selected = contentList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openFile(selected.getFilePath());
                } else {
                    showErrorAlert("Error", "Select a file to open");
                }
            });
    
            vbox.getChildren().addAll(new Label("Materials:"), contentList, btnOpenFile);
            stage.setScene(new Scene(vbox, 600, 400));
            stage.show();
        }
    
        // Helper class to represent course materials
        public class CourseMaterial {
            private String title;
            private String filePath; // filename or relative path
    
            public CourseMaterial(String title, String filePath) {
                this.title = title;
                this.filePath = filePath;
            }
    
            public String getTitle() { return title; }
            public String getFilePath() { return filePath; }
    
            @Override
            public String toString() {
                return title; // display title in ListView
            }
        }
    
        // Load materials from database into ListView
        private void loadCourseMaterials(Course course, ListView<CourseMaterial> listView) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT title, file_path FROM course_content WHERE course_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, course.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String title = rs.getString("title");
                    String filePath = rs.getString("file_path");
                    listView.getItems().add(new CourseMaterial(title, filePath));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // Open file with default application
        private void openFile(String filePath) {
            try {
                File file = new File("course_content_files/" + filePath); // path to your files directory
                if (!file.exists()) {
                    showErrorAlert("File Not Found", "The file does not exist: " + filePath);
                    return;
                }
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showErrorAlert("Unsupported", "Desktop operations are not supported on this system");
                }
            } catch (Exception e) {
                showErrorAlert("Error", "Could not open file: " + e.getMessage());
            }
        }
    
        private void loadCourseContent(Course course, ListView<String> listView) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT title, file_path FROM course_content WHERE course_id=?");
                ps.setString(1, course.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(rs.getString("title") + " (" + rs.getString("file_path") + ")");
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // Show assessments from instructor for students
        private void showAssessmentsToTake(String username) {
            Stage stage = new Stage();
            stage.setTitle("Available Assessments & Quizzes");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ListView<Assessment> assessmentList = new ListView<>();
            loadAssessmentsForStudent(assessmentList, username);
            assessmentList.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            Button btnTake = new Button("Take Selected Assessment");
            btnTake.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnTake.setOnAction(e -> {
                Assessment assessment = assessmentList.getSelectionModel().getSelectedItem();
                if (assessment != null) {
                    showAssessmentTakingForm(assessment, username);
                } else {
                    showErrorAlert("Error", "Select an assessment");
                }
            });
            vbox.getChildren().addAll(new Label("Assessments:"), assessmentList, btnTake);
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void loadAssessmentsForStudent(ListView<Assessment> listView, String username) {
            listView.getItems().clear();
            try (Connection conn = getConnection()) {
                String sql = "SELECT a.id, a.course_id, a.title, a.description "
                        + "FROM assessments a "
                        + "JOIN courses c ON a.course_id=c.id "
                        + "JOIN enrollments e ON c.id=e.course_id "
                        + "WHERE e.student_username=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    listView.getItems().add(new Assessment(
                            rs.getString("id"),
                            rs.getString("course_id"),
                            rs.getString("title"),
                            rs.getString("description")
                    ));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        private void showAssessmentTakingForm(Assessment assessment, String username) {
            Stage stage = new Stage();
            stage.setTitle("Take Assessment: " + assessment.getTitle());
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            Label lblAssessment = new Label("Assessment: " + assessment.getTitle());
            TextArea answers = new TextArea();
            answers.setPromptText("Enter your responses...");
            answers.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            Button btnSubmit = new Button("Submit");
            btnSubmit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSubmit.setOnAction(e -> {
                String response = answers.getText();
                try (Connection conn = getConnection()) {
                    String sql = "INSERT INTO submissions (id, student_username, assessment_id, submission_text, grade) VALUES (?, ?, ?, ?, NULL)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, "S" + System.currentTimeMillis());
                    ps.setString(2, username);
                    ps.setString(3, assessment.getId());
                    ps.setString(4, response);
                    ps.executeUpdate();
                    ps.close();
                    logSystemAction(currentUser, "Submitted assessment: " + assessment.getTitle() + " by user: " + username);
                    showAlert("Submitted", "Assessment submitted");
                    stage.close();
                } catch (SQLException ex) {
                    showErrorAlert("Error", ex.getMessage());
                }
            });
            vbox.getChildren().addAll(new Label("Assessment: " + assessment.getTitle()), answers, btnSubmit);
            stage.setScene(new Scene(vbox, 400, 300));
            stage.show();
        }
    
        private void showSubmitAssignments(String username) {
            Stage stage = new Stage();
            stage.setTitle("Submit Assignments");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            ListView<Assessment> assessmentList = new ListView<>();
            loadAssessmentsForStudent(assessmentList, username);
            assessmentList.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            Button btnSubmit = new Button("Submit Selected");
            btnSubmit.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnSubmit.setOnAction(e -> {
                Assessment selected = assessmentList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showAssignmentSubmissionForm(selected, username);
                } else {
                    showErrorAlert("Error", "Select an assignment");
                }
            });
            vbox.getChildren().addAll(new Label("Assignments:"), assessmentList, btnSubmit);
            stage.setScene(new Scene(vbox, 400, 400));
            stage.show();
        }
    
        private void showAssignmentSubmissionForm(Assessment assignment, String username) {
            Stage stage = new Stage();
            stage.setTitle("Submit Assignment: " + assignment.getTitle());
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            TextArea submission = new TextArea();
            submission.setPromptText("Enter your response...");
            submission.setStyle("-fx-background-color: #FFFFFF; -fx-font-size: 13px;");
            Button btnSubmit = new Button("Submit");
            btnSubmit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSubmit.setOnAction(e -> {
                String response = submission.getText();
                try (Connection conn = getConnection()) {
                    String sql = "INSERT INTO submissions (id, student_username, assessment_id, submission_text, grade) VALUES (?, ?, ?, ?, NULL)";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, "S" + System.currentTimeMillis());
                    ps.setString(2, username);
                    ps.setString(3, assignment.getId());
                    ps.setString(4, response);
                    ps.executeUpdate();
                    ps.close();
                    logSystemAction(currentUser, "Submitted assignment: " + assignment.getTitle() + " by user: " + username);
                    showAlert("Submitted", "Assignment submitted");
                    stage.close();
                } catch (SQLException ex) {
                    showErrorAlert("Error", ex.getMessage());
                }
            });
            vbox.getChildren().addAll(new Label("Assignment: " + assignment.getTitle()), submission, btnSubmit);
            stage.setScene(new Scene(vbox, 400, 300));
            stage.show();
        }
    
        private void showGradesAndProgress(String username) {
            Stage stage = new Stage();
            stage.setTitle("Grades & Progress");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E0F7FA;");
            TableView<GradeProgress> table = new TableView<>();
            loadGradesProgress(table, username);
            TableColumn<GradeProgress, String> colAssessment = new TableColumn<>("Assessment");
            colAssessment.setCellValueFactory(new PropertyValueFactory<>("assessmentTitle"));
            TableColumn<GradeProgress, String> colGrade = new TableColumn<>("Grade");
            colGrade.setCellValueFactory(new PropertyValueFactory<>("grade"));
            table.getColumns().addAll(colAssessment, colGrade);
            vbox.getChildren().addAll(new Label("Grades & Progress:"), table);
            stage.setScene(new Scene(vbox, 600, 400));
            stage.show();
        }
    
        private void loadGradesProgress(TableView<GradeProgress> table, String username) {
            table.getItems().clear();
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT a.title, s.grade FROM submissions s JOIN assessments a ON s.assessment_id=a.id WHERE s.student_username=?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    table.getItems().add(new GradeProgress(rs.getString("title"), rs.getString("grade")));
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
    
        // ======================= Student Discussion =======================
        private void showStudentDiscussionForum(String username) {
            Stage stage = new Stage();
            stage.setTitle("Participate in Discussions");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #E8F5E9;");
            ListView<String> forums = new ListView<>();
            loadForums(forums);
            forums.setStyle("-fx-control-inner-background: #FFFFFF; -fx-font-size: 13px;");
            Button btnOpen = new Button("Open Selected");
            btnOpen.setStyle("-fx-background-color: #009688; -fx-text-fill: white;");
            btnOpen.setOnAction(e -> {
                String selected = forums.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    showDiscussionForum(selected);
                } else {
                    showErrorAlert("Error", "Select a forum");
                }
            });
            vbox.getChildren().addAll(forums, new HBox(10, btnOpen));
            Scene scene = new Scene(vbox, 400, 300);
            stage.setScene(scene);
            stage.show();
        }
    
        private void showAddMCQQuestionDialog() {
            Stage stage = new Stage();
            stage.setTitle("Add Multiple Choice Question");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(15));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
    
            // Question text area
            TextArea questionArea = new TextArea();
            questionArea.setPromptText("Enter the question here...");
    
            // Options (4 options example)
            TextField[] optionFields = new TextField[4];
            CheckBox[] correctChecks = new CheckBox[4];
    
            for (int i = 0; i < 4; i++) {
                optionFields[i] = new TextField();
                optionFields[i].setPromptText("Option " + (i + 1));
                correctChecks[i] = new CheckBox("Correct");
                HBox hb = new HBox(10, optionFields[i], correctChecks[i]);
                vbox.getChildren().add(hb);
            }
    
            // Checkbox for multiple answers
            CheckBox multiAnswerChk = new CheckBox("Allow multiple correct answers");
    
            // Save button
            Button btnSave = new Button("Save Question");
            btnSave.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSave.setOnAction(e -> {
                String questionText = questionArea.getText().trim();
                if (questionText.isEmpty()) {
                    showErrorAlert("Validation Error", "Question text cannot be empty");
                    return;
                }
    
                boolean isMultipleAnswer = multiAnswerChk.isSelected();
    
                MCQOption[] options = new MCQOption[4];
                for (int i = 0; i < 4; i++) {
                    String optText = optionFields[i].getText().trim();
                    if (optText.isEmpty()) {
                        showErrorAlert("Validation Error", "All options must be filled");
                        return;
                    }
                    boolean isCorrect = correctChecks[i].isSelected();
                    options[i] = new MCQOption(optText, isCorrect);
                }
    
                // Save the question and options to DB
                saveMCQQuestion(questionText, options, isMultipleAnswer);
                stage.close();
                showAlert("Success", "Question saved");
            });
    
            vbox.getChildren().addAll(multiAnswerChk, btnSave);
            Scene scene = new Scene(vbox, 400, 400);
            stage.setScene(scene);
            stage.show();
        }
    
    
    
        private void saveMCQQuestion(String questionText, MCQOption[] options, boolean isMultipleAnswer) {
            try (Connection conn = getConnection()) {
                // Insert question
                String sqlQ = "INSERT INTO questions (question_text, is_multiple_answer) VALUES (?, ?)";
                PreparedStatement psQ = conn.prepareStatement(sqlQ, Statement.RETURN_GENERATED_KEYS);
                psQ.setString(1, questionText);
                psQ.setBoolean(2, isMultipleAnswer);
                psQ.executeUpdate();
    
                // Get generated question ID
                ResultSet rs = psQ.getGeneratedKeys();
                int questionId = -1;
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }
                rs.close();
                psQ.close();
    
                // Insert options
                String sqlOpt = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
                PreparedStatement psOpt = conn.prepareStatement(sqlOpt);
                for (MCQOption opt : options) {
                    psOpt.setInt(1, questionId);
                    psOpt.setString(2, opt.getOptionText());
                    psOpt.setBoolean(3, opt.isCorrect());
                    psOpt.executeUpdate();
                }
                psOpt.close();
    
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
        }
        public void seedDataIfEmpty() {
            try (Connection conn = getConnection()) {
                // Check if the table is empty
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM student_progress");
                rs.next();
                int count = rs.getInt("total");
                rs.close();

                if (count == 0) {
                    // Insert sample data
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO student_progress (student_username, course_id, progress_percentage) VALUES (?, ?, ?)"
                    );
                    // Sample data for student1
                    ps.setString(1, "student1");
                    ps.setString(2, "C001");
                    ps.setInt(3, 50);
                    ps.executeUpdate();

                    ps.setString(1, "student1");
                    ps.setString(2, "C002");
                    ps.setInt(3, 80);
                    ps.executeUpdate();

                    // Sample data for student2
                    ps.setString(1, "student2");
                    ps.setString(2, "C001");
                    ps.setInt(3, 20);
                    ps.executeUpdate();

                    ps.close();

                    System.out.println("Sample progress data inserted.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private double getStudentProgressPercentage(String studentUsername) {
            try (Connection conn = getConnection()) {
                // Total assessments assigned
                String totalSql = "SELECT COUNT(*) FROM assessments a "
                        + "JOIN courses c ON a.course_id=c.id "
                        + "JOIN enrollments e ON c.id=e.course_id "
                        + "WHERE e.student_username=?";
                PreparedStatement totalPs = conn.prepareStatement(totalSql);
                totalPs.setString(1, studentUsername);
                ResultSet rsTotal = totalPs.executeQuery();
                int totalAssessments = 0;
                if (rsTotal.next()) {
                    totalAssessments = rsTotal.getInt(1);
                }
                rsTotal.close();
                totalPs.close();

                if (totalAssessments == 0) {
                    return 0; // no assessments assigned
                }

                // Assessments graded/submitted by the student
                String completedSql = "SELECT COUNT(*) FROM submissions s "
                        + "JOIN assessments a ON s.assessment_id=a.id "
                        + "JOIN courses c ON a.course_id=c.id "
                        + "JOIN enrollments e ON c.id=e.course_id "
                        + "WHERE e.student_username=? AND s.grade IS NOT NULL";
                PreparedStatement completedPs = conn.prepareStatement(completedSql);
                completedPs.setString(1, studentUsername);
                ResultSet rsCompleted = completedPs.executeQuery();
                int completedCount = 0;
                if (rsCompleted.next()) {
                    completedCount = rsCompleted.getInt(1);
                }
                rsCompleted.close();
                completedPs.close();

                return (double) completedCount / totalAssessments;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        }
        // ======================= Certificates =======================
        private void showCertificates(String username) {
            showAlert("Certificates", "Download certificates placeholder");
        }

        private void showMultipleChoiceQuestions(String username) {
            Stage stage = new Stage();
            stage.setTitle("Answer Multiple Choice Questions");
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(15));
            vbox.setStyle("-fx-background-color: #F0F8FF;");

            // Load questions
            List<mcqquestion> questions = loadMCQQuestions();

            // For each question, create a label and checkboxes or radio buttons
            List<ToggleGroup> toggleGroups = new ArrayList<>();

            for (mcqquestion q : questions) {
                Label lblQuestion = new Label(q.getQuestionText());
                lblQuestion.setWrapText(true);
                vbox.getChildren().add(lblQuestion);

                if (q.isMultipleAnswer()) {
                    // Multiple answers: checkboxes
                    List<CheckBox> checkBoxes = new ArrayList<>();
                    try (Connection conn = getConnection()) {
                        PreparedStatement ps = conn.prepareStatement("SELECT option_text, is_correct FROM options WHERE question_id=?");
                        ps.setInt(1, q.getQuestionId());
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            CheckBox cb = new CheckBox(rs.getString("option_text"));
                            checkBoxes.add(cb);
                            vbox.getChildren().add(cb);
                        }
                        rs.close();
                        ps.close();
                    } catch (SQLException e) {
                        showErrorAlert("Error", e.getMessage());
                    }
                } else {
                    // Single answer: radio buttons
                    ToggleGroup tg = new ToggleGroup();
                    toggleGroups.add(tg);
                    try (Connection conn = getConnection()) {
                        PreparedStatement ps = conn.prepareStatement("SELECT option_text, is_correct FROM options WHERE question_id=?");
                        ps.setInt(1, q.getQuestionId());
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            RadioButton rb = new RadioButton(rs.getString("option_text"));
                            rb.setToggleGroup(tg);
                            vbox.getChildren().add(rb);
                        }
                        rs.close();
                        ps.close();
                    } catch (SQLException e) {
                        showErrorAlert("Error", e.getMessage());
                    }
                }
            }

            Button btnSubmit = new Button("Submit Answers");
            btnSubmit.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            btnSubmit.setOnAction(e -> {
                // Collect answers and evaluate
                boolean allCorrect = true;
                int index = 0;
                for (mcqquestion q : questions) {
                    if (q.isMultipleAnswer()) {
                        // Check checkboxes (not shown here; you'd need to store the checkboxes)
                        // For simplicity, assume correct answers are stored and checked
                    } else {
                        ToggleGroup tg = toggleGroups.get(index);
                        RadioButton selected = (RadioButton) tg.getSelectedToggle();
                        if (selected == null || !isAnswerCorrect(selected.getText(), q.getQuestionId())) {
                            allCorrect = false;
                        }
                    }
                    index++;
                }
                if (allCorrect) {
                    showAlert("Result", "All answers correct! Well done.");
                } else {
                    showAlert("Result", "Some answers are incorrect. Please try again.");
                }
                stage.close();
            });
            vbox.getChildren().add(btnSubmit);

            Scene scene = new Scene(new ScrollPane(vbox), 600, 600);
            stage.setScene(scene);
            stage.show();
        }

        private List<mcqquestion> loadMCQQuestions() {
            return List.of();
        }

        private boolean isAnswerCorrect(String selectedOption, int questionId) {
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT is_correct FROM options WHERE question_id=? AND option_text=?");
                ps.setInt(1, questionId);
                ps.setString(2, selectedOption);
                ResultSet rs = ps.executeQuery();
                boolean correct = false;
                if (rs.next()) {
                    correct = rs.getBoolean("is_correct");
                }
                rs.close();
                ps.close();
                return correct;
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
                return false;
            }
        }

        public class mcqquestion {
            private int questionId;

            private String questionText;
            private boolean isMultipleAnswer;

            public mcqquestion(int questionId, String questionText, boolean isMultipleAnswer) {
                this.questionId = questionId;
                this.questionText = questionText;
                this.isMultipleAnswer = isMultipleAnswer;
            }

            public int getQuestionId() { return questionId; }
            public String getQuestionText() { return questionText; }
            public boolean isMultipleAnswer() { return isMultipleAnswer; }
        }

        // ======================= Update Profile =======================
        private void showUpdateProfile(String username) {
            Stage stage = new Stage();
            stage.setTitle("Update Profile - " + username);
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(20));
            vbox.setStyle("-fx-background-color: #F0F8FF;");
            try (Connection conn = getConnection()) {
                PreparedStatement ps = conn.prepareStatement("SELECT email, role FROM users WHERE username=?");
                ps.setString(1, username);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    TextField emailField = new TextField(rs.getString("email"));
                    emailField.setPromptText("Email");
                    PasswordField pwField = new PasswordField();
                    pwField.setPromptText("New Password");
                    Button btnUpdate = new Button("Update Profile");
                    btnUpdate.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                    btnUpdate.setOnAction(e -> {
                        String newEmail = emailField.getText();
                        String newPW = pwField.getText();
                        try (Connection conn2 = getConnection()) {
                            if (!newEmail.isEmpty()) {
                                String sql2 = "UPDATE users SET email=? WHERE username=?";
                                PreparedStatement ps2 = conn2.prepareStatement(sql2);
                                ps2.setString(1, newEmail);
                                ps2.setString(2, username);
                                ps2.executeUpdate();
                                ps2.close();
                            }
                            if (!newPW.isEmpty()) {
                                String sql2 = "UPDATE users SET password=? WHERE username=?";
                                PreparedStatement ps2 = conn2.prepareStatement(sql2);
                                ps2.setString(1, hashPassword(newPW));
                                ps2.setString(2, username);
                                ps2.executeUpdate();
                                ps2.close();
                            }
                            logSystemAction(currentUser, "Updated profile for user: " + username);
                            showAlert("Success", "Profile updated");
                            stage.close();
                        } catch (SQLException e2) {
                            showErrorAlert("Error", e2.getMessage());
                        }
                    });
                    vbox.getChildren().addAll(new Label("Email:"), emailField, new Label("New Password:"), pwField, btnUpdate);
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                showErrorAlert("Error", e.getMessage());
            }
            Scene scene = new Scene(vbox, 400, 250);
            stage.setScene(scene);
            stage.show();
        }
    }