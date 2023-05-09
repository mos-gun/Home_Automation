package com.homeauto.controller;

import com.homeauto.config.StageManager;
import com.homeauto.portComm.Communication;
import com.homeauto.view.FxmlView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.ResourceBundle;


@Controller
public class HomeController implements Initializable {

	@Lazy
	@Autowired
	StageManager stageManager;

	@FXML
	private GridPane gpaneWindowHome;

	@FXML
	private Button btnCreate;

	@FXML
	private Button btnOpenPlan;

	@FXML
	private Button btnExit;

	@FXML
	private ChoiceBox<String> choiceboxComm;

	@FXML
	private Button btnCheckChoice;

	@FXML
	private ImageView imgPortChoice;


	//////////////////////////////// VARIABLES ////////////////////////////////
	private FileChooser fileChooser;
	private FileChooser.ExtensionFilter extensionFilter;
	private static File savesPath;
	private static File planToOpen;
	private static boolean insideWindowCreate;
	private static boolean insideWindowOpenPlan;
	private static Communication comm;
	private static String chosenCOM;


	//////////////////////////////// METHODS ////////////////////////////////
	@FXML
	void checkPortChoice(ActionEvent event) {
		if (choiceboxComm.getSelectionModel().isEmpty()) {
			imgPortChoice.setImage(new Image("images/false.png"));
			Tooltip tooltip = new Tooltip("Bitte wählen Sie einen gültigen Port aus.");
			Tooltip.install(imgPortChoice, tooltip);
		} else {
			chosenCOM = choiceboxComm.getValue();
			btnOpenPlan.setDisable(false);
			System.out.println(chosenCOM);
			imgPortChoice.setImage(new Image("images/correct.png"));
			Tooltip tooltip = new Tooltip("Auswahl bestätigt: " + chosenCOM);
			Tooltip.install(imgPortChoice, tooltip);
		}
		imgPortChoice.setVisible(true);
	}

	@FXML
	void exitProgram(ActionEvent event) {
		Alert alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Warnung");
		alert.setHeaderText("Sie sind im Begriff, das Programm zu beenden.");
		alert.setContentText("Möchten Sie die Anwendung schließen?");
		ButtonType buttonConfirm = new ButtonType("Ja");
		ButtonType buttonCancel = new ButtonType("Nein", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(buttonConfirm, buttonCancel);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == buttonConfirm) System.exit(0);
	}

	@FXML
	void goToCreate(ActionEvent event) {
		insideWindowCreate = true;
		insideWindowOpenPlan = false;
		stageManager.switchScene(FxmlView.CREATE);
	}

	@FXML
	void openPlan(ActionEvent event) {
		fileChooser = new FileChooser();
		configureFileChooser(fileChooser);
		File file = fileChooser.showOpenDialog(gpaneWindowHome.getScene().getWindow());
		if (file != null) {
			openFile(file);
		} else {
			System.out.println("no file chosen");
		}
	}
	
	private void configureFileChooser(final FileChooser fileChooser) {
		fileChooser.setTitle("Grundriss öffnen...");
		fileChooser.setInitialDirectory(savesPath);
		extensionFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		fileChooser.getExtensionFilters().add(extensionFilter);
	}

	private void openFile(File file) {
		planToOpen = file;
		insideWindowOpenPlan = true;
		insideWindowCreate = false;
		stageManager.switchScene(FxmlView.OPENPLAN);
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public File getSavesPath() {
		return savesPath;
	}

	public void setSavesPath(File savesPath) {
		this.savesPath = savesPath;
	}

	public File getPlanToOpen() {
		return planToOpen;
	}

	public void setPlanToOpen(File planToOpen) {
		this.planToOpen = planToOpen;
	}

	public boolean isInsideWindowCreate() {
		return insideWindowCreate;
	}

	public boolean isInsideWindowOpenPlan() {
		return insideWindowOpenPlan;
	}

	public Communication getComm() {
		return comm;
	}

	public void setComm(Communication comm) {
		this.comm = comm;
	}

	public String getChosenCOM() {
		return chosenCOM;
	}

	public void setChosenCOM(String chosenCOM) {
		HomeController.chosenCOM = chosenCOM;
	}


	//////////////////////////////// METHODS (SPECIAL)////////////////////////////////
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		savesPath = new File(System.getProperty("user.home"), "HomeAuto/savestates");

		if (savesPath.mkdirs()) System.out.println(savesPath + " created!");
		else System.out.println("save directory not created! already exists!\n-> " + savesPath);
		Path sourceDirectory = Paths.get("src\\main\\resources\\savestates\\vorlage_nicht_loeschen.txt");
		Path targetDirectory = Paths.get(savesPath + "\\vorlage_nicht_loeschen.txt");
		try {
			Files.copy(sourceDirectory, targetDirectory, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("template could not be copied!!!");
		}

		chosenCOM = "-";
		comm = new Communication();
		ObservableList<String> ports = FXCollections.observableArrayList(comm.getAvailableSerialPorts());
		choiceboxComm.setItems(ports);

		btnOpenPlan.setDisable(true);
		imgPortChoice.setVisible(false);

	}
	
}
