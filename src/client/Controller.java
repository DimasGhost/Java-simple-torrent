package client;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Pair;
import myutils.SocketHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public TextField ipField;

    @FXML
    private TextField portField;

    @FXML
    private Button downloadButton;

    @FXML
    private Label statusLabel;

    @FXML
    private ProgressBar downloadProgress;

    @FXML
    private ListView filesListView;
    private ListProperty<String> filesListProperty = new SimpleListProperty<>();
    private ArrayList<String> filesList = new ArrayList<String>();

    Socket socket;
    SocketHelper helper;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        downloadButton.setOnMouseClicked(event -> addDownload());
    }

    private void finishDownload(String filename, long size, File file) {
        filesList.add(filename + " (" + size / 10000 / 100f + "MB) -> " + file.getAbsolutePath());
        filesListProperty.set(FXCollections.observableArrayList(filesList));
        filesListView.itemsProperty().bind(filesListProperty);

        resetInterface();
    }

    private void download(String filename, long size) {
        statusLabel.setText("Waiting for directory choice");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save file");
        fileChooser.setInitialFileName(filename);
        File file = fileChooser.showSaveDialog(null);
        if (file == null) {
            cancelDownload();
            return;
        }

        String status = "Downloading file " + filename + " (" + size / 10000 / 100f + "MB)";
        status += " to path " + file.getAbsolutePath();
        statusLabel.setText(status);

        new Thread(() -> {
            try {
                helper.writeString(filename);

                FileOutputStream fout = new FileOutputStream(file);
                byte[] bytes;
                long done = 0;
                while ((bytes = helper.readData()) != null) {
                    fout.write(bytes, 0, bytes.length);
                    done += bytes.length;
                    long finalDone = done;
                    Platform.runLater(() -> downloadProgress.setProgress(finalDone * 1.0 / size));
                }
                helper.out.writeInt(0);
                fout.close();

                Platform.runLater(() -> finishDownload(filename, size, file));
            } catch (Exception ex) {
                Platform.runLater(this::onDisconnect);
            }
        }).start();
    }

    private void askForDownload(String filename, long size) {
        statusLabel.setText("Waiting for confirmation");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Download confirmation");
        alert.setHeaderText("Download confrmation");
        String question = "Are you sure you want to download file " + filename;
        question += " (" + size / 10000 / 100f + "MB)?";
        alert.setContentText(question);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent() || result.get() != ButtonType.OK) {
            cancelDownload();
            return;
        }

        download(filename, size);
    }

    private void showFilesChoice(List<Pair<String, Long>> files) {
        statusLabel.setText("Waiting for choice");

        List <String> choices = new ArrayList<>();
        for (Pair<String, Long> file : files)
            choices.add(file.getKey() + " (" + file.getValue() / 10000 / 100f + "MB)");
        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("File choice");
        dialog.setHeaderText("File choice");
        dialog.setContentText("Choose file to download");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) {
            cancelDownload();
            return;
        }

        String filename = null;
        long size = 0;
        for (int i = 0; i < choices.size(); ++i)
            if (result.get().equals(choices.get(i))) {
                filename = files.get(i).getKey();
                size = files.get(i).getValue();
            }
        askForDownload(filename, size);
    }

    private void cancelDownload() {
        try {
            socket.close();
        } catch (IOException ignored) { }
        resetInterface();
    }

    private void onDisconnect() {
        System.out.println("DISC");
        resetInterface();
    }

    private void resetInterface() {
        ipField.setDisable(false);
        portField.setDisable(false);
        downloadButton.setDisable(false);
        statusLabel.setText("");
        downloadProgress.setProgress(0);
    }

    private void addDownload() {
        ipField.setDisable(true);
        portField.setDisable(true);
        downloadButton.setDisable(true);
        statusLabel.setText("Loading files list");

        new Thread(() -> {
            try {
                socket = new Socket(ipField.getText(), Integer.parseInt(portField.getText()));
                helper = new SocketHelper(socket);
                List<Pair<String, Long>> files = new ArrayList<>();
                int filesCnt = helper.in.readInt();
                for (int i = 0; i < filesCnt; ++i)
                    files.add(new Pair<>(helper.readString(), helper.in.readLong()));
                Platform.runLater(() -> showFilesChoice(files));
            } catch (Exception ex) {
                Platform.runLater(this::onDisconnect);
            }
        }).start();
    }
}