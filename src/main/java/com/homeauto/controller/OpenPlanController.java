package com.homeauto.controller;

import com.homeauto.config.StageManager;
import com.homeauto.view.FxmlView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.ImageCursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.net.URL;
import java.util.*;


@Controller
public class OpenPlanController implements Initializable {

	@Lazy
	@Autowired
	StageManager stageManager;

	@FXML
	private BorderPane bpaneWindowCreate;

	@FXML
	private ToolBar toolbarHeader;

	@FXML
	private Button btnHideBackground;

	@FXML
	private Button btnEditPlan;

	@FXML
	private VBox vboxIcons;

	@FXML
	private Button btnDefaultCursor;

	@FXML
	private Button btnLamp;

	@FXML
	private Button btnThermo;

	@FXML
	private Button btnWall;

	@FXML
	private Button btnTrashCan;

	@FXML
	private TitledPane tpaneCenter;

	@FXML
	private AnchorPane apaneDrawingField;

	@FXML
	private Pane paneDrawingField;

	@FXML
	private ImageView imgBackground;

	@FXML
	private Slider sliderTemperature;

	@FXML
	private Text txtSliderTemperature;

	@FXML
	private Text txtThermo1;

	@FXML
	private Text txtThermo2;

	@FXML
	private Text txtThermo3;

	@FXML
	private HBox hboxFooter;

	@FXML
	private Button btnCancel;

	@FXML
	private Button btnSave;

	@FXML
	private Button btnConfirmName;

	@FXML
	private TextField txtfldName;

	@FXML
	private ImageView imgNameCheck;


	//////////////////////////////// VARIABLES ////////////////////////////////
	private final HomeController homeController = new HomeController();

	// Vars for File saving
	private FileOutputStream out;
	private PrintWriter printWriter;
	private BufferedReader bufferedReader;
	private File savesPath;
	private final String pathSuffix = ".txt";

	// Vars for drawing
	public static List<WallPoint> listPoints;
	public static List<WallLine> listLines;
	public static List<SmartLamp> listLamps;
	public static List<SmartThermo> listThermos;
	WallPoint pointStart, pointEnd;
	WallLine line;
	SmartLamp lamp;
	SmartThermo thermo;
	private Group groupObjects = new Group();
	DoubleProperty pointStartX;
	DoubleProperty pointStartY;
	DoubleProperty pointEndX;
	DoubleProperty pointEndY;
	DoubleProperty lampPosX, lampPosY;
	DoubleProperty thermoPosX, thermoPosY;
	final int widthIcons = 32, heightIcons = 32;

	private double mouseStartX, mouseStartY;
	private double mouseEndX, mouseEndY;
	private double currentPosX, currentPosY;
	private static int idWall, idLamp, idThermo;
	private static boolean mouseOnWall, mouseOnLamp, mouseOnThermo;
	private static char[][] arrPhysicalLamps;
	private static char[][] arrPhysicalThermos;
	private char currentLamp;
	private char currentThermo;
	private static char correspondingTextToDelete;

	// Vars for Drag and Drop
	private static boolean moveIconsMode = false;
	private static boolean insertLampMode = false;
	private static boolean insertThermoMode = false;
	private static boolean insertWallMode = false;
	private static boolean insertTrashMode = false;

	// Vars for other
	private Alert alert;
	private boolean onlyValidCharsUsed;
	private static boolean isEditing;
	private boolean backgroundIsHidden;

	// Vars different than CreateController
	private static File planToOpen;


	//////////////////////////////// METHODS ////////////////////////////////
	/*
	  void cancelDrawing() Closes the drawing window and returns to the main menu
	  The user is asked again if he really wants to perform this action and is asked to click on the yes or no button.
	  If the user clicks yes, the stageManager changes the scene and the user returns to the main menu.
	 */
	@FXML
	void cancelDrawing(ActionEvent event) {
		alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Warnung");
		alert.setHeaderText("Das Erstellen des Grundrisses wird abgebrochen und der Fortschritt verworfen.");
		alert.setContentText("Soll der Vorgang wirklich abgebrochen werden?");
		ButtonType buttonTypeConfirm = new ButtonType("Ja");
		ButtonType buttonTypeCancel = new ButtonType("Nein", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(buttonTypeConfirm, buttonTypeCancel);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == buttonTypeConfirm) {
			deleteAllObjects();
			paneDrawingField.getChildren().addAll(groupObjects);
			apaneDrawingField.getScene().setCursor(Cursor.DEFAULT);
			insertDefault(new ActionEvent());
			homeController.getComm().disconnect();
			stageManager.switchScene(FxmlView.HOME);
		}
	}

	/* void checkInput(KeyEvent event)Checks the input for saving the name of the .txt
	 	file that is created when you want to save your drawing.
	 */
	@FXML
	void checkInput(KeyEvent event) {
		imgNameCheck.setVisible(true);
		if (txtfldName.getText().contains("\\") || txtfldName.getText().contains("/") ||
				txtfldName.getText().contains(":") || txtfldName.getText().contains("*") ||
				txtfldName.getText().contains("?") || txtfldName.getText().contains("\"") ||
				txtfldName.getText().contains("<") || txtfldName.getText().contains(">") ||
				txtfldName.getText().contains("|")) {
			onlyValidCharsUsed = false;
			imgNameCheck.setImage(new Image("images/false.png"));
			Tooltip tooltip = new Tooltip("Ungültige Symbole: \n\\ / : * ? \" < > |");
			Tooltip.install(imgNameCheck, tooltip);
		} else {
			onlyValidCharsUsed = true;
			imgNameCheck.setImage(new Image("images/correct.png"));
		}

		if (txtfldName.getText().equals(tpaneCenter.getText())) {
			btnConfirmName.setDisable(true);
		} else {
			btnConfirmName.setDisable(false);
		}
	}

	/*void confirmName(ActionEvent event)similar to
	void checkInput(KeyEvent event)is checked here if
	the input from the user is correct
	*/
	@FXML
	void confirmName(ActionEvent event) {
		if (txtfldName.getText().equals("")) {
			tpaneCenter.setTextFill(Color.RED);
			tpaneCenter.setText("<Bezeichnung des Grundrisses>");

			alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("Das Eingabefeld ist leer.");
			alert.setContentText("Bitte eine gültige Bezeichnung eingeben.");
			alert.showAndWait();
		} else if (! onlyValidCharsUsed) {
			alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText("Die Eingabe enthält ungültige Symbole.");
			alert.setContentText("Die Bezeichnung darf keines der folgenden Zeichen enthalten:\n\\ / : * ? \" < > |");
			alert.showAndWait();
		} else {
			tpaneCenter.setTextFill(Color.GREEN);
			tpaneCenter.setText(txtfldName.getText());
			btnConfirmName.setDisable(true);
		}
	}

	/*
	void editPlan(ActionEvent event)
	This method checks if the user is in edit mode.
	The If condition realizes that the buttons are hidden
	and can no longer be clicked when the user is not in edit mode
	*/
	@FXML
	void editPlan(ActionEvent event) {
		if (! isEditing) {
			tpaneCenter.setDisable(false);
			btnLamp.setDisable(false);
			btnThermo.setDisable(false);
			btnWall.setDisable(false);
			btnTrashCan.setDisable(false);
			txtfldName.setDisable(false);
			btnConfirmName.setDisable(false);
			btnSave.setDisable(false);

			btnDefaultCursor.setVisible(true);
			btnLamp.setVisible(true);
			btnThermo.setVisible(true);
			btnWall.setVisible(true);
			btnTrashCan.setVisible(true);
			txtfldName.setVisible(true);
			btnConfirmName.setVisible(true);
			btnSave.setVisible(true);

			imgBackground.setImage(new Image("images/Gitterlinie.png"));
			btnHideBackground.setText("Gitter ausblenden");
			backgroundIsHidden = false;

			sliderTemperature.setVisible(false);
			txtSliderTemperature.setVisible(false);

			btnEditPlan.setText("Zum Ansichtsmodus");
			isEditing = true;
		} else {
			btnLamp.setDisable(true);
			btnThermo.setDisable(true);
			btnWall.setDisable(true);
			btnTrashCan.setDisable(true);
			txtfldName.setDisable(true);
			btnConfirmName.setDisable(true);
			btnSave.setDisable(true);

			btnDefaultCursor.setVisible(false);
			btnLamp.setVisible(false);
			btnThermo.setVisible(false);
			btnWall.setVisible(false);
			btnTrashCan.setVisible(false);
			txtfldName.setVisible(false);
			btnConfirmName.setVisible(false);
			btnSave.setVisible(false);
			imgNameCheck.setVisible(false);

			imgBackground.setImage(null);
			btnHideBackground.setText("Gitter einblenden");
			backgroundIsHidden = true;

			sliderTemperature.setVisible(true);
			txtSliderTemperature.setVisible(true);

			btnEditPlan.setText("Zum Bearbeitungsmodus");
			isEditing = false;
		}
	}

	/*
	  void hideBackground(ActionEvent event)
	* with this method the user can change the background image of the drawing field.
	* There is only the option with grid background or an empty background.
	* */
	@FXML
	void hideBackground(ActionEvent event) {
		if (! backgroundIsHidden) {
			imgBackground.setImage(null);
			btnHideBackground.setText("Gitter einblenden");
			backgroundIsHidden = true;
		} else {
			imgBackground.setImage(new Image("images/Gitterlinie.png"));
			btnHideBackground.setText("Gitter ausblenden");
			backgroundIsHidden = false;
		}
	}

	/*
	 void insertDefault(ActionEvent event)
	 represents the logical normal default state of the cursors
	 it shows that the user can only move objects
	* */
	@FXML
	void insertDefault(ActionEvent event) {
		moveIconsMode = true;
		insertLampMode = false;
		insertThermoMode = false;
		insertWallMode = false;
		insertTrashMode = false;
	}

	/*
	void insertLamp(ActionEvent event)
	represents the logical lamp mode, in this state the user can place lamps
	* */
	@FXML
	void insertLamp(ActionEvent event) {
		insertLampMode = true;
		moveIconsMode = false;
		insertThermoMode = false;
		insertWallMode = false;
		insertTrashMode = false;
	}

	/*
	void insertThermo(ActionEvent event)
	represents the logical thermo mode, in this state the user can place thermostats
	* */
	@FXML
	void insertThermo(ActionEvent event) {
		insertThermoMode = true;
		moveIconsMode = false;
		insertLampMode = false;
		insertWallMode = false;
		insertTrashMode = false;
	}

	/*
	void insertWall(ActionEvent event)
	represents the logical wall mode, in this state the user can draw walls,
	 move wall objects as well as connect wall objects
	* */
	@FXML
	void insertWall(ActionEvent event) {
		insertWallMode = true;
		moveIconsMode = false;
		insertLampMode = false;
		insertThermoMode = false;
		insertTrashMode = false;
	}

	/*
	void insertTrash(ActionEvent event)
	represents the logical trashcan mode, in this state the user is able to delete single objects
	* */
	@FXML
	void insertTrash(ActionEvent event) {
		insertWallMode = false;
		moveIconsMode = false;
		insertLampMode = false;
		insertThermoMode = false;
		insertTrashMode = true;
	}

	/*
	  void saveDrawing(ActionEvent event) throws IOException
	* With this method the user can save his drawing. It is first checked whether
	* the drawing is empty or not. If the drawing is empty, the user is informed
	* and nothing is saved. If the user enters a name that already exists in the
	* folder, the user will be informed and who will be asked to overwrite the file.
	* Before the user can save the file he has to click on the btnConfirmName
	* otherwise he is not able to save the drawing.
	* */
	@FXML
	void saveDrawing(ActionEvent event) throws IOException {
		if (btnConfirmName.isDisable()) {
			if (listLines.isEmpty() && listPoints.isEmpty()) {
				alert = new Alert(Alert.AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText("Es wurde kein Grundriss gezeichnet.");
				alert.setContentText("Es kann nur ein existierender Grundriss gespeichert werden.\n" +
						"Zeichnen Sie einen Grundriss und versuchen Sie es erneut.");
				alert.showAndWait();
			} else {
				File filePath = new File(savesPath + "\\" + txtfldName.getText() + pathSuffix);
				if ((filePath.exists())) {
					System.out.println("file already exists! really overwrite?");
					alert = new Alert(Alert.AlertType.WARNING);
					alert.setTitle("Warnung");
					alert.setHeaderText("Eine Datei mit der Bezeichnung \"" + txtfldName.getText() + "\" existiert bereits.");
					alert.setContentText("Soll die Datei überschrieben werden?");
					ButtonType buttonTypeOverwrite = new ButtonType("Überschreiben");
					ButtonType buttonTypeCancel = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
					alert.getButtonTypes().setAll(buttonTypeOverwrite, buttonTypeCancel);
					Optional<ButtonType> result = alert.showAndWait();
					if (result.get() == buttonTypeOverwrite) {
						writeToFile(filePath);
						popupFileSaved(filePath);
					}
				} else {
					writeToFile(filePath);
					popupFileSaved(filePath);
				}
			}
		} else {
			System.out.println("error, name not confirmed! please confirm!");
			alert = new Alert(Alert.AlertType.ERROR);
			alert.setTitle("Fehler");
			if (tpaneCenter.getTextFill() == Color.RED && txtfldName.getText().equals("")) {
				alert.setHeaderText("Die Datei konnte nicht gespeichert werden, da keine Bezeichnung\neingegeben wurde.");
				alert.setContentText("Bitte eine gültige Bezeichnung eingeben und bestätigen.");
			} else {
				alert.setHeaderText("Die Datei konnte nicht gespeichert werden, da die eingegebene\nBezeichnung \"" + txtfldName.getText() + "\" nicht bestätigt wurde.");
				alert.setContentText("Bitte die Eingabe bestätigen und erneut versuchen.");
			}
			alert.showAndWait();
		}
	}

	/*
	 * public void backToDefaultCursor(MouseEvent event)
	 * This method resets the mouse cursor to its default mode
	 * */
	@FXML
	public void backToDefaultCursor(MouseEvent event) {
		apaneDrawingField.getScene().setCursor(Cursor.DEFAULT);
		paneDrawingField.getScene().setCursor(Cursor.DEFAULT);
	}

	/*
	 * public void placeLamp(MouseEvent event)
	 * This method changes the image of the mouse cursor to a lamp.
	 * This should show the user that he is in lamp mode.
	 * */
	@FXML
	public void placeLamp(MouseEvent event) {
		Image lampImage = new Image("images/licht2.png");
		apaneDrawingField.getScene().setCursor(new ImageCursor(lampImage));
		paneDrawingField.getScene().setCursor(new ImageCursor(lampImage));
	}

	/*
	 * public void placeThermo(MouseEvent event)
	 * This method changes the image of the mouse cursor to a thermostat.
	 * This should show the user that he is in thermomode.
	 * */
	@FXML
	public void placeThermo(MouseEvent event) {
		Image thermoImage = new Image("images/thermo3.png");
		apaneDrawingField.getScene().setCursor(new ImageCursor(thermoImage));
		paneDrawingField.getScene().setCursor(new ImageCursor(thermoImage));
	}

	/*public void placeTrashCan(MouseEvent event)
	  This method changes the image of the mouse cursor to a trashcan.
	  This should show the user that he is in trashmode.
	* */
	@FXML
	public void placeTrashCan(MouseEvent event) {
		Image canImage = new Image("images/deleteCursor.png");
		apaneDrawingField.getScene().setCursor(new ImageCursor(canImage));
		paneDrawingField.getScene().setCursor(new ImageCursor(canImage));
	}

	/*
	 * public void placeWall(MouseEvent event)
	 * This method changes the image of the mouse cursor to a hand.
	 * This should show the user that he is in  wallmode.
	 * */
	@FXML
	public void placeWall(MouseEvent event) {
		Image kreis = new Image("images/leftHand.png");
		apaneDrawingField.getScene().setCursor(new ImageCursor(kreis));
		paneDrawingField.getScene().setCursor(new ImageCursor(kreis));
	}

	/*
  	public void writeToFile(File filePath) throws IOException
  	This method writes to the stored .txt file. listLines,listLamp
  	and listThermos are ArrayLists in which automatically after creation
  	of an object, the respective object is inserted. All lists are read out
  	so we can call all objects and get the X and Y coordinates of the objects
  	by the methods getStartX() and getStartY(). These are then written into
  	the .txt file using a structure determined by us.

    Example for wall:
    wall;x-coordinate;y-coordinate; id
	* */
	public void writeToFile(File filePath) throws IOException {
		out = new FileOutputStream(filePath);
		printWriter = new PrintWriter(out);
		for (WallLine line : listLines) {
			printWriter.println("wall;" + line.getStartX() + ";" + line.getStartY() + ";" +
					line.getEndX() + ";" + line.getEndY() + ";" + line.idLine);
		}

		for (SmartLamp lamp : listLamps) {
			printWriter.println("lamp;" + lamp.getX() + ";" + lamp.getY() + ";" + lamp.getCorrespondingPhysicalLamp() +
					";" + lamp.getIdLamp());
		}

		for (SmartThermo thermo : listThermos) {
			printWriter.println("thermo;" + thermo.getX() + ";" + thermo.getY() + ";" +
					thermo.getCorrespondingPhysicalThermo() + ";" + thermo.getIdThermo());
		}

		printWriter.close();
		out.close();
	}

	/*
	  private void popupFileSaved(File filePath)
	  This method notifies the user that his drawing has been successfully saved.
	  This way the user is confident that the file has been saved.
	* */
	private void popupFileSaved(File filePath) {
		System.out.println("file successfully saved!");
		alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText("Grundriss \"" + txtfldName.getText() + "\" erfolgreich gespeichert.");
		alert.setContentText("Die Datei befindet sich im folgenden Pfad:\n" + filePath.getAbsolutePath());
		alert.showAndWait();
	}

	/*
		public void mousePressed(MouseEvent event)
		This method works on the different logical modes that we have.
		The mouse coordinates are assigned to our auxiliary variables
		mouseStartX and mouseStartY. Depending on the mode (insertWallMode,insertLampMode,
		insertThermoMode,moveIconsMode,insertTrashMode) the user can create the different
		objects here by pressing the mouse. Then the objects are inserted into the ArrayLists.
		Because no objects are created with moveIconsMode and insertTrashMode, they are empty in the query.
	* */
	public void mousePressed(MouseEvent event) {
		mouseStartX = event.getX();
		mouseStartY = event.getY();

		if (insertWallMode) {
			if (! mouseOnWall) {
				pointStartX = new SimpleDoubleProperty(mouseStartX);
				pointStartY = new SimpleDoubleProperty(mouseStartY);
				pointEndX = new SimpleDoubleProperty(mouseStartX);
				pointEndY = new SimpleDoubleProperty(mouseStartY);
				idWall += 1;
				pointStart = new WallPoint(pointStartX, pointStartY, Color.BLACK, idWall);
				pointEnd = new WallPoint(pointEndX, pointEndY, Color.BLACK, idWall);
				line = new WallLine(pointStartX, pointStartY, pointEndX, pointEndY, idWall);
				line.toBack();
				pointStart.toFront();
				pointEnd.toFront();
				groupObjects.getChildren().add(pointStart);
				groupObjects.getChildren().add(pointEnd);
				groupObjects.getChildren().add(line);
				listPoints.add(pointStart);
				listPoints.add(pointEnd);
				listLines.add(line);
				handleCollisions(pointStart);
			}
		} else if (insertLampMode) {
			if (! mouseOnLamp) {
				lampPosX = new SimpleDoubleProperty(mouseStartX);
				lampPosY = new SimpleDoubleProperty(mouseStartY);
				idLamp += 1;
				currentLamp = '-';
				for (int i = 0; i < arrPhysicalLamps.length; i++) {
					if (arrPhysicalLamps[i][1] == '0') {
						currentLamp = arrPhysicalLamps[i][0];
						arrPhysicalLamps[i][1] = '1';
						break;
					}
				}
				if (currentLamp == '-') {
					alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Information");
					alert.setHeaderText("Maximale Anzahl von Lampen sind bereits plaziert.");
					alert.setContentText("Es können zurzeit nicht mehr als 3 Lampen je Grundriss plaziert werden.");
					alert.showAndWait();
				} else {
					lamp = new SmartLamp(lampPosX, lampPosY, widthIcons, heightIcons, currentLamp, idLamp);
					lamp.toFront();
					groupObjects.getChildren().add(lamp);
					listLamps.add(lamp);
				}
			}
		} else if (insertThermoMode) {
			if (! mouseOnThermo) {
				thermoPosX = new SimpleDoubleProperty(mouseStartX);
				thermoPosY = new SimpleDoubleProperty(mouseStartY);
				idThermo += 1;
				currentThermo = '-';
				for (int i = 0; i < arrPhysicalThermos.length; i++) {
					if (arrPhysicalThermos[i][1] == '0') {
						currentThermo = arrPhysicalThermos[i][0];
						arrPhysicalThermos[i][1] = '1';
						break;
					}
				}
				if (currentThermo == '-') {
					alert = new Alert(Alert.AlertType.INFORMATION);
					alert.setTitle("Information");
					alert.setHeaderText("Maximale Anzahl von Heizungen sind bereits plaziert.");
					alert.setContentText("Es können zurzeit nicht mehr als 3 Heizungen je Grundriss plaziert werden.");
					alert.showAndWait();
				} else {
					thermo = new SmartThermo(thermoPosX, thermoPosY, widthIcons, heightIcons, currentThermo, idThermo);
					thermo.toFront();
					groupObjects.getChildren().add(thermo);
					listThermos.add(thermo);
				}
			}
		} else if (moveIconsMode) {

		} else if (insertTrashMode) {

		}
	}

	/*
	  public void mouseDragged(MouseEvent event)
	* This method allows the user to move objects by holding down the left mouse button.
	* Depending on which object the user moves, as soon as the user releases the mouse state,
	* the coordinates of the mouse are passed to the object (using the variables currentPosX and currentPosY),
	* so our program stays up to date with the current object positions.
	* */
	public void mouseDragged(MouseEvent event) {
		currentPosX = event.getX();
		currentPosY = event.getY();

		if (insertWallMode) {
			if (! mouseOnWall) {
				pointEnd.setCenterX(currentPosX);
				pointEnd.setCenterY(currentPosY);
			}
		} else if (insertLampMode) {
			if (! mouseOnLamp) {
				lamp.setX(currentPosX);
				lamp.setY(currentPosY);
			}
		} else if (insertThermoMode) {
			if (! mouseOnThermo) {
				thermo.setX(currentPosX);
				thermo.setY(currentPosY);
			}
		} else if (moveIconsMode) {

		}
	}

	public void mouseReleased(MouseEvent event) {
		mouseEndX = event.getX();
		mouseEndY = event.getY();

		if (insertWallMode) {
			if (! mouseOnWall) {
				pointEnd.setCenterX(mouseEndX);
				pointEnd.setCenterY(mouseEndY);
				handleCollisions(pointEnd);
			}
			mouseOnWall = false;
		} else if (insertLampMode) {
			if (! mouseOnLamp) {
				lamp.setX(mouseEndX);
				lamp.setY(mouseEndY);
				for (SmartLamp lamp : listLamps) {
					System.out.println("lamp: " + lamp.getX() + " || " + lamp.getY() + " -> " + lamp.idLamp);
				}
				System.out.println();
			}
			mouseOnLamp = false;
		} else if (insertThermoMode) {
			if (! mouseOnThermo) {
				thermo.setX(mouseEndX);
				thermo.setY(mouseEndY);

				if (currentThermo == '1') {
					txtThermo1.setX(mouseEndX);
					txtThermo1.setY(mouseEndY + 40);
					txtThermo1.setVisible(true);
				} else if (currentThermo == '2') {
					txtThermo2.setX(mouseEndX);
					txtThermo2.setY(mouseEndY + 40);
					txtThermo2.setVisible(true);
				} else if (currentThermo == '3') {
					txtThermo3.setX(mouseEndX);
					txtThermo3.setY(mouseEndY + 40);
					txtThermo3.setVisible(true);
				}

				for (SmartThermo thermo : listThermos) {
					System.out.println("thermo: " + thermo.getX() + " || " + thermo.getY() + " -> " + thermo.idThermo);
				}
				System.out.println();
			}
			mouseOnThermo = false;
		} else if (moveIconsMode) {

		}
	}

	/*
	private void handleCollisions(WallPoint wallPoint)
	This method makes it possible that we connect the endpoints of a wall object with other wall objects.
	This method is called on line 689 after you have created a wall object. We have chosen this method
	because it looks better graphically on the drawing field than if individual end points are not properly
	connected, creating a user-friendly connection between the end points. The connection is basically nothing
	more than an overlap of the endpoint coordinates.
	* */
	private void handleCollisions(WallPoint wallPoint) {
		for (WallPoint point : getListPoints()) {
			if (! point.equals(wallPoint)) {
				if (wallPoint.intersects(point.getBoundsInLocal())) {
					wallPoint.setCenterX(point.getCenterX());
					wallPoint.setCenterY(point.getCenterY());
					break;
				}
			}
		}
	}

	/*
	  private void deleteAllObjects()
	* This method assigns a new empty ArrayList object to each list.
	* Thus all objects stored in the lists disappear
	* */
	private void deleteAllObjects() {
		listPoints = new ArrayList<>();
		listLines = new ArrayList<>();
		listLamps = new ArrayList<>();
		listThermos = new ArrayList<>();
		groupObjects = new Group();
	}

	/*
	  private void drawPlan(File filePath) throws IOException
	* This method allows you to read the .txt file to be read out and the
	* corresponding drawing to be created dynamically.
	* Based on our previously defined pattern we have developed
	* an algorithm that first looks up which object it is and then
	* requests the coordinates and the ID and then creates an object
	* and gets the previously obtained parameters. Afterwards it is added
	* to the ArrayList
	* */
	private void drawPlan(File filePath) throws IOException {
		deleteAllObjects();

		bufferedReader = new BufferedReader(new FileReader(filePath));
		String txt;
		String[] arrInput;

		while ((txt = bufferedReader.readLine()) != null) {
			arrInput = txt.split(";");
			if (arrInput.length != 5 && arrInput.length != 6) {
				// invalid input; nothing happens with the read row
			} else {
				switch (arrInput[0]) {
					case "wall":
						pointStartX = new SimpleDoubleProperty(Double.parseDouble(arrInput[1]));
						pointStartY = new SimpleDoubleProperty(Double.parseDouble(arrInput[2]));
						pointEndX = new SimpleDoubleProperty(Double.parseDouble(arrInput[3]));
						pointEndY = new SimpleDoubleProperty(Double.parseDouble(arrInput[4]));
						idWall = Integer.parseInt(arrInput[5]);
						pointStart = new WallPoint(pointStartX, pointStartY, Color.BLACK, idWall);
						pointEnd = new WallPoint(pointEndX, pointEndY, Color.BLACK, idWall);
						line = new WallLine(pointStartX, pointStartY, pointEndX, pointEndY, idWall);
						groupObjects.getChildren().add(pointStart);
						groupObjects.getChildren().add(pointEnd);
						groupObjects.getChildren().add(line);
						listPoints.add(pointStart);
						listPoints.add(pointEnd);
						listLines.add(line);
						break;
					case "lamp":
						lampPosX = new SimpleDoubleProperty(Double.parseDouble(arrInput[1]));
						lampPosY = new SimpleDoubleProperty(Double.parseDouble(arrInput[2]));
						currentLamp = arrInput[3].charAt(0);
						if (currentLamp == 'R') arrPhysicalLamps[0][1] = '1';
						else if (currentLamp == 'Y') arrPhysicalLamps[1][1] = '1';
						else if (currentLamp == 'G') arrPhysicalLamps[2][1] = '1';
						idLamp = Integer.parseInt(arrInput[4]);
						lamp = new SmartLamp(lampPosX, lampPosY, widthIcons, heightIcons, currentLamp, idLamp);
						groupObjects.getChildren().add(lamp);
						listLamps.add(lamp);
						break;
					case "thermo":
						thermoPosX = new SimpleDoubleProperty(Double.parseDouble(arrInput[1]));
						thermoPosY = new SimpleDoubleProperty(Double.parseDouble(arrInput[2]));
						currentThermo = arrInput[3].charAt(0);
						if (currentThermo == '1') {
							arrPhysicalThermos[0][1] = '1';
							txtThermo1.setVisible(true);
						} else if (currentThermo == '2') {
							arrPhysicalThermos[1][1] = '1';
							txtThermo2.setVisible(true);
						} else if (currentThermo == '3') {
							arrPhysicalThermos[2][1] = '1';
							txtThermo3.setVisible(true);
						}
						idThermo = Integer.parseInt(arrInput[4]);
						thermo = new SmartThermo(thermoPosX, thermoPosY, widthIcons, heightIcons, currentThermo, idThermo);
						groupObjects.getChildren().add(thermo);
						listThermos.add(thermo);
						break;
					default:
						System.out.println("invalid read line (not a wall, lamp or thermo)");
						break;
				}
			}
		}
		paneDrawingField.getChildren().addAll(groupObjects);
		paneDrawingField.toBack();

		for (WallPoint point : listPoints) {
			System.out.println(point.getCenterX() + " || " + point.getCenterY() + " -> " + point.getIdPoint());
		}
		for (SmartLamp lamp : listLamps) {
			System.out.println(lamp.getX() + " || " + lamp.getY() + " -> " + lamp.getIdLamp());
		}
		for (SmartThermo thermo : listThermos) {
			System.out.println(thermo.getX() + " || " + thermo.getY() + " -> " + thermo.getIdThermo());
		}
		System.out.println("DONE");
		bufferedReader.close();
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public List<WallPoint> getListPoints() {
		return listPoints;
	}

	public void setListPoints(List<WallPoint> listPoints) {
		this.listPoints = listPoints;
	}

	public List<WallLine> getListLines() {
		return listLines;
	}

	public void setListLines(List<WallLine> listLines) {
		CreateController.listLines = listLines;
	}

	public List<SmartLamp> getListLamps() {
		return listLamps;
	}

	public void setListLamps(List<SmartLamp> listLamps) {
		CreateController.listLamps = listLamps;
	}

	public List<SmartThermo> getListThermos() {
		return listThermos;
	}

	public void setListThermos(List<SmartThermo> listThermos) {
		CreateController.listThermos = listThermos;
	}

	public int getId() {
		return idWall;
	}

	public void setId(int id) {
		this.idWall = id;
	}

	public boolean isMouseOnWall() {
		return mouseOnWall;
	}

	public void setMouseOnWall(boolean mouseOnWall) {
		this.mouseOnWall = mouseOnWall;
	}

	public boolean isMouseOnLamp() {
		return mouseOnLamp;
	}

	public void setMouseOnLamp(boolean mouseOnLamp) {
		this.mouseOnLamp = mouseOnLamp;
	}

	public boolean isMouseOnThermo() {
		return mouseOnThermo;
	}

	public void setMouseOnThermo(boolean mouseOnThermo) {
		this.mouseOnThermo = mouseOnThermo;
	}

	public boolean isMoveIconsMode() {
		return moveIconsMode;
	}

	public void setMoveIconsMode(boolean moveIconsMode) {
		this.moveIconsMode = moveIconsMode;
	}

	public boolean isInsertLampMode() {
		return insertLampMode;
	}

	public void setInsertLampMode(boolean lampMode) {
		this.insertLampMode = lampMode;
	}

	public boolean isInsertThermoMode() {
		return insertThermoMode;
	}

	public void setInsertThermoMode(boolean thermoMode) {
		this.insertThermoMode = thermoMode;
	}

	public boolean isInsertWallMode() {
		return insertWallMode;
	}

	public void setInsertWallMode(boolean wallMode) {
		this.insertWallMode = wallMode;
	}

	public boolean isInsertTrashMode() {
		return insertTrashMode;
	}

	public void setInsertTrashMode(boolean insertTrashMode) {
		OpenPlanController.insertTrashMode = insertTrashMode;
	}

	public Group getGroupObjects() {
		return groupObjects;
	}

	public void setGroupObjects(Group groupObjects) {
		this.groupObjects = groupObjects;
	}

	public boolean isEditing() {
		return isEditing;
	}

	public void setEditing(boolean editing) {
		isEditing = editing;
	}

	public char[][] getArrPhysicalLamps() {
		return arrPhysicalLamps;
	}

	public void setArrPhysicalLamps(char[][] arrPhysicalLamps) {
		OpenPlanController.arrPhysicalLamps = arrPhysicalLamps;
	}

	public char[][] getArrPhysicalThermos() {
		return arrPhysicalThermos;
	}

	public void setArrPhysicalThermos(char[][] arrPhysicalThermos) {
		OpenPlanController.arrPhysicalThermos = arrPhysicalThermos;
	}

	public char getCorrespondingTextToDelete() {
		return correspondingTextToDelete;
	}

	public void setCorrespondingTextToDelete(char correspondingTextToDelete) {
		OpenPlanController.correspondingTextToDelete = correspondingTextToDelete;
	}


	//////////////////////////////// METHODS (SPECIAL)////////////////////////////////
	/*This method processes the information it has received from the port.
	This method is responsible for outputting the temperature as well as changing
	the temperature icon depending on the temperature the icon changes. There are 5
	different statuses between 0 and 35
	*/
	private void checkForPortInputPeriodically() {
		Timeline checkPortInput = new Timeline(new KeyFrame(Duration.millis(500), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (homeController.getComm().isNewTemperatureReceived()) {
					System.out.println("temp: " + homeController.getComm().getTemperature());
					String temperature = homeController.getComm().getTemperature().replace("Celcius", "°C");
					txtThermo1.setText(temperature);
					txtThermo2.setText(temperature);
					txtThermo3.setText(temperature);
					int temperatureAsInt = Integer.parseInt(temperature.substring(1, 4));
					System.out.println(temperatureAsInt);
					for (SmartThermo thermo : listThermos) {
						if (temperatureAsInt < 15) thermo.setImage(new Image("images/thermo1.png"));
						else if (temperatureAsInt < 20) thermo.setImage(new Image("images/thermo2.png"));
						else if (temperatureAsInt < 25) thermo.setImage(new Image("images/thermo3.png"));
						else if (temperatureAsInt < 30) thermo.setImage(new Image("images/thermo4.png"));
						else if (temperatureAsInt < 35) thermo.setImage(new Image("images/thermo5.png"));
					}
					homeController.getComm().setNewTemperatureReceived(false);
				}

				if (homeController.getComm().isNewLightToggleReceived()) {
					System.out.println("Lamp " + homeController.getComm().getToggledLamp() + " toggled");
					for (SmartLamp lamp : listLamps) {
						if (lamp.getCorrespondingPhysicalLamp() == homeController.getComm().getToggledLamp()) {
							if (lamp.isOn) {
								lamp.setImage(new Image("images/licht_aus_" + homeController.getComm().getToggledLamp() + ".png"));
								lamp.setOn(false);
							} else {
								lamp.setImage(new Image("images/licht_an_" + homeController.getComm().getToggledLamp() + ".png"));
								lamp.setOn(true);
							}
							break;
						}
					}
					homeController.getComm().setNewLightToggleReceived(false);
				}
			}
		}));
		checkPortInput.setCycleCount(Timeline.INDEFINITE);
		checkPortInput.play();
	}

	/*
	  private void checkForThermoPosChanges()
	* changes the position of the thermostat so that the message which is
	* the temperature is not displayed on the thermostat icon
	* */
	private void checkForThermoPosChanges() {
		Timeline checkForThermo = new Timeline(new KeyFrame(Duration.millis(25), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				for (SmartThermo thermo : listThermos) {
					if (thermo.getCorrespondingPhysicalThermo() == '1') {
						txtThermo1.setX(thermo.getX());
						txtThermo1.setY(thermo.getY() + 40);
					} else if (thermo.getCorrespondingPhysicalThermo() == '2') {
						txtThermo2.setX(thermo.getX());
						txtThermo2.setY(thermo.getY() + 40);
					} else if (thermo.getCorrespondingPhysicalThermo() == '3') {
						txtThermo3.setX(thermo.getX());
						txtThermo3.setY(thermo.getY() + 40);
					}
				}
				if (correspondingTextToDelete != '-') {
					if (correspondingTextToDelete == '1') txtThermo1.setVisible(false);
					if (correspondingTextToDelete == '2') txtThermo2.setVisible(false);
					if (correspondingTextToDelete == '3') txtThermo3.setVisible(false);
					correspondingTextToDelete = '-';
				}
			}
		}));
		checkForThermo.setCycleCount(Timeline.INDEFINITE);
		checkForThermo.play();
	}

	/*
	 * arrPhysicalLamps and arrPhysicalThermos Represent the physical elements
	 * 'R', 'Y', 'G' stand for the red, yellow and green LED, the '0' behind it
	 * stands for the lamp being off. If the lamp is switched on the '0' is
	 * changed into a '1'. Similarly with the thermostats '1', '2' and '3'
	 * are the 3 thermostats that you can have maximum, the '0' behind them
	 * indicates that they are switched off, they are changed to a '1' when
	 * they are switched on.
	 * */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		savesPath = homeController.getSavesPath();

		insertDefault(new ActionEvent());
		onlyValidCharsUsed = true;
		isEditing = false;
		backgroundIsHidden = false;

		listPoints = new ArrayList<>();
		listLines = new ArrayList<>();
		listLamps = new ArrayList<>();
		listThermos = new ArrayList<>();
		groupObjects = new Group();
		idWall = idLamp = idThermo = 0;
		mouseOnWall = false;
		mouseOnLamp = false;
		mouseOnThermo = false;

		planToOpen = homeController.getPlanToOpen();
		System.out.println("plan to open: " + planToOpen);
		tpaneCenter.setTextFill(Color.BLUE);
		tpaneCenter.setText(planToOpen.getName().substring(0, planToOpen.getName().length() - 4));

		currentLamp = '-';
		arrPhysicalLamps = new char[][]{{'R', '0'}, {'Y', '0'}, {'G', '0'}};
		currentThermo = '-';
		arrPhysicalThermos = new char[][]{{'1', '0'}, {'2', '0'}, {'3', '0'}};
		correspondingTextToDelete = '-';

		try {
			drawPlan(planToOpen);
		} catch (IOException e) {
			System.out.println("not able to load plan!");
		}

		btnLamp.setDisable(true);
		btnThermo.setDisable(true);
		btnWall.setDisable(true);
		btnTrashCan.setDisable(true);
		txtfldName.setDisable(true);
		btnConfirmName.setDisable(true);
		btnSave.setDisable(true);

		btnDefaultCursor.setVisible(false);
		btnLamp.setVisible(false);
		btnThermo.setVisible(false);
		btnWall.setVisible(false);
		btnTrashCan.setVisible(false);
		txtfldName.setVisible(false);
		btnConfirmName.setVisible(false);
		btnSave.setVisible(false);
		imgBackground.setImage(null);
		btnHideBackground.setText("Gitter einblenden");
		backgroundIsHidden = true;

		System.out.println("chosen port: " + homeController.getChosenCOM());
		homeController.getComm().connect(homeController.getChosenCOM());

		checkForPortInputPeriodically();
		checkForThermoPosChanges();

	}

}