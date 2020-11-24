package com.anonymous;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.xml.internal.ws.streaming.XMLStreamReaderUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.imageio.ImageIO;
import javax.swing.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.rmi.server.ExportException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by Adam on 10.04.2018.
 */
public class MainWindow extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    private final String dirPath = System.getProperty("user.home") + "\\" + "AppData\\Local\\Inuidisse";
    private Stage primaryStage;
    private String defaultPath = "";
    private boolean isTray = false;
    private boolean isAutorun = false;
    private Button runBtn;
    private DirWatcher dirWatcher;

    java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
    java.awt.TrayIcon trayIcon;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Inuidisse Alert System");
        primaryStage.getIcons().add(new javafx.scene.image.Image(MainWindow.class.getResourceAsStream("/resources/icon.png")));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_LEFT);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 5, 5, 20));

        initWindow();

        Label dirConf = new Label("Please select Your Windbot folder:");
        dirConf.setFont(Font.font("default", FontWeight.BOLD, 12));
        dirConf.setTextFill(javafx.scene.paint.Color.color(1,1,1)); //white color
        grid.add(dirConf, 0, 0, 2,1);

        Label pathLabel = new Label("Path:");
        pathLabel.setTextFill(javafx.scene.paint.Color.color(1,1,1)); //white color
        grid.add(pathLabel, 0, 1);

        TextField pathTextField = new TextField();
        pathTextField.setMinWidth(140);
        pathTextField.setText(defaultPath);
        pathTextField.setOnAction((event) -> {
            defaultPath = pathTextField.getText();
            updateConfigFile();
        });
        grid.add(pathTextField, 1, 1);

        Button browseBtn = new Button("browse");
        browseBtn.setMinSize(60, 20);
        browseBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                } catch (Exception ex){
                    System.err.println("Browse directory error");
                }

                JFileChooser f = new JFileChooser();

                try {
                    f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    f.showSaveDialog(null);
                } catch (Exception ex) {
                    return;
                }

                pathTextField.setText(f.getSelectedFile().toString());
            }
        });
        grid.add(browseBtn, 2, 1);

        runBtn = new Button("Run");
        runBtn.setMinSize(100, 20);
        runBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                if (runBtn.getText().equals("Run"))
                {
                    runBtn.setText("Stop");
                    pathTextField.setDisable(true);
                    browseBtn.setDisable(true);

                    dirWatcher = new DirWatcher(pathTextField.getText());
                    dirWatcher.start();
                }
                else
                {
                    runBtn.setText("Run");
                    pathTextField.setDisable(false);
                    browseBtn.setDisable(false);
                    dirWatcher.stop();
                }

                updateConfigFile();
            }
        });
        grid.add(runBtn, 2, 4);

        CheckBox trayBox = new CheckBox();
        trayBox.setText("Keep in tray");
        trayBox.setTextFill(javafx.scene.paint.Color.color(1,1,1)); //white color
        trayBox.setSelected(isTray);
        trayBox.setOnAction((event) -> {
            boolean selected = trayBox.isSelected();
            isTray = selected;
            updateConfigFile();
            changeTraySet();
        });

        CheckBox autoStartBox = new CheckBox();
        autoStartBox.setText("Start with Windows");
        autoStartBox.setTextFill(javafx.scene.paint.Color.color(1,1,1)); //white color
        autoStartBox.setSelected(isAutorun);
        autoStartBox.setOnAction((event) -> {
            boolean selected = autoStartBox.isSelected();
            isAutorun = selected;
            updateConfigFile();


            String startupPathString = System.getProperty("user.home") + "\\" + "AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";
            Path startupPath = Paths.get(startupPathString);
            File file = new  File(MainWindow.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            Path filePath = Paths.get(file.toString());
            System.out.println(file);
            try {
                if (isAutorun) {
                    Files.copy(filePath, startupPath.resolve(filePath.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
                else {
                    Files.delete(Paths.get(startupPath.toString() + "\\" + filePath.getFileName()));
                }
            } catch (IOException e) {
                System.out.println("Copy file to startup error");
            }
        });


        primaryStage.iconifiedProperty().addListener((observable, oldValue, iconified) -> {
            if (iconified) {
                if (isTray) {
                    primaryStage.close();
                }
            }
        });

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            public void handle(WindowEvent we) {
                if (dirWatcher != null) {
                    dirWatcher.stop();
                }
                System.out.println("Stage is closing");
            }
        });

        grid.add(trayBox, 0, 3, 4, 1);
        grid.add(autoStartBox, 0, 4, 4, 1);

        startFromAutorun();

        grid.setStyle("-fx-background-image: url(resources/lucio.jpg);\n" +
                "    -fx-background-repeat: stretch;   \n" +
                "    -fx-background-size: 358 168;\n" +
                "    -fx-background-position: center center;\n" +
                "    -fx-effect: dropshadow(three-pass-box, black, 30, 0.5, 0, 0);");

        Scene scene = new Scene(grid, 350, 160);
        //scene.getStylesheets().addAll(this.getClass().getResource("resources/style.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void addAppToTray() {
        try {
            java.awt.Toolkit.getDefaultToolkit();

            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
            }

            java.awt.Image image = null;
            try {
                image = ImageIO.read(getClass().getResource("/resources/icon.png"));
            }
            catch (IOException ex) {
                System.err.println("Getting trayIcon from resource failed");
            }

            trayIcon = new java.awt.TrayIcon(image);
            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            java.awt.MenuItem openItem = new java.awt.MenuItem("Open");
            openItem.addActionListener(event -> Platform.runLater(this::showStage));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(event -> {
                dirWatcher.stop();
                Platform.exit();
                tray.remove(trayIcon);
            });

            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setImageAutoSize(true);
            trayIcon.setPopupMenu(popup);
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("Unable to init system tray");
            //e.printStackTrace();
        }
    }



    private void disableTray() {
        tray.remove(trayIcon);
    }

    private void showStage() {
        if (primaryStage != null) {
            primaryStage.setIconified(false);
            primaryStage.show();
            primaryStage.toFront();
        }
    }

    private void initWindow() {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            Scanner file = new Scanner(new FileReader(dirPath + "\\config.txt"));
            if (!file.hasNext()) {
                return;
            }
            defaultPath = file.next();
            if (defaultPath.equals("EMPTY")) {
                defaultPath = "";
            }
            if (file.next().equals("YES")) {
                isTray = true;
                Platform.setImplicitExit(false);
                javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
            }
            else {
                Platform.setImplicitExit(true);
                javax.swing.SwingUtilities.invokeLater(this::disableTray);
            }
            if (file.next().equals("YES")) {
                isAutorun = true;
            }
        }
        catch (IOException e) {
            System.out.println("Reading config.txt file error");
        }
    }

    private void updateConfigFile() {
        List<String> lines = new ArrayList<String>();
        if (!defaultPath.equals("")) {
            lines.add(defaultPath);
        }
        else {
            lines.add("EMPTY");
        }
        if (isTray) {
            lines.add("YES");
        }
        else {
            lines.add("NO");
        }
        if (isAutorun) {
            lines.add("YES");
        }
        else {
            lines.add("NO");
        }
        Path file = Paths.get(dirPath + "/config.txt");
        try {
            Files.write(file, lines, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            System.err.println("Config file write error" + ex.getMessage());
        }
    }

    private void changeTraySet() {
        if (isTray) {
            Platform.setImplicitExit(false);
            javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
        }
        else {
            Platform.setImplicitExit(true);
            javax.swing.SwingUtilities.invokeLater(this::disableTray);
        }
    }

    private void startFromAutorun() {
        if (isAutorun) {
            runBtn.fire();
        }
    }
}
