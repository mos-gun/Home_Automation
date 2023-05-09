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
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.net.URL;
import java.util.*;


@Controller
public class CreateController implements Initializable {

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
	private Button btnLoadTemplate;

	@FXML
	private Button btnClear;

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
	public WallPoint pointStart, pointEnd;
	public WallLine line;
	public SmartLamp lamp;
	public SmartThermo thermo;
	private Group groupObjects = new Group();
	public DoubleProperty pointStartX;
	public DoubleProperty pointStartY;
	public DoubleProperty pointEndX;
	public DoubleProperty pointEndY;
	public DoubleProperty lampPosX, lampPosY;
	public DoubleProperty thermoPosX, thermoPosY;
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

	// Vars for Drag and Drop of Objects
	private static boolean moveIconsMode = false;
	private static boolean insertLampMode = false;
	private static boolean insertThermoMode = false;
	private static boolean insertWallMode = false;
	private static boolean insertTrashMode = false;

	// Vars for other
	private Alert alert;
	private boolean onlyValidCharsUsed;
	private boolean isTemplate;
	private boolean backgroundIsHidden;


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
			stageManager.switchScene(FxmlView.HOME);
		}
	}

	/*
	 * void checkInput(KeyEvent event)Checks the input for saving the name of the .txt
	 * file that is created when you want to save your drawing.
	 * */
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

	/*
	  void clearDrawing(ActionEvent event)
	* deletes all objects on the drawing field so that the user can directly
	* delete everything at once and does not have to delete everything individually.
	* */
	@FXML
	void clearDrawing(ActionEvent event) {
		alert = new Alert(Alert.AlertType.WARNING);
		alert.setTitle("Warnung");
		alert.setHeaderText("Möchten Sie die bisherigen Fortschritte verwerfen?");
		alert.setContentText("Dieser Vorgang kann nicht wieder rückgängig gemacht werden.");
		ButtonType buttonConfirm = new ButtonType("Ja");
		ButtonType buttonCancel = new ButtonType("Nein", ButtonBar.ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(buttonConfirm, buttonCancel);
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == buttonConfirm) {
			deleteAllObjects();
			stageManager.switchScene(FxmlView.CREATE);
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
	void loadTemplate(ActionEvent event) throws IOException
	this method opens a ready-made drawing template that has already been created
	with the name "vorlage_nicht_loeschen". All previously created objects are
	uploaded using the coordinates in the .txt file. In front of the coordinates
	is the name of the object, so our program can distinguish between the different
	types of objects (wall, lamp and thermostat).This method gives the user an
	approximate impression of what a drawing might look like
	* */
	@FXML
	void loadTemplate(ActionEvent event) throws IOException {
		if (! isTemplate) {
			alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle("Warnung");
			alert.setHeaderText("Sie sind im Begriff, die Vorlage zu öffnen.");
			alert.setContentText("Durch das Öffnen der Vorlage gehen alle bisherigen Fortschritte verloren. " +
					"Sind Sie sicher?");
			ButtonType buttonTypeConfirm = new ButtonType("Ja");
			ButtonType buttonTypeCancel = new ButtonType("Nein", ButtonBar.ButtonData.CANCEL_CLOSE);
			alert.getButtonTypes().setAll(buttonTypeConfirm, buttonTypeCancel);
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == buttonTypeConfirm) {
				stageManager.switchScene(FxmlView.CREATE);
				deleteAllObjects();
				File filePath = new File(savesPath + "\\vorlage_nicht_loeschen" + pathSuffix);
				if (! filePath.exists())
					System.out.println("Vorlage kann nicht geladen werden, da es nicht existiert!");
				else {
					bufferedReader = new BufferedReader(new FileReader(filePath));
					String txt;
					String[] arrInput;

					while ((txt = bufferedReader.readLine()) != null) {
						arrInput = txt.split(";");
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
								idLamp = Integer.parseInt(arrInput[4]);
								lamp = new SmartLamp(lampPosX, lampPosY, widthIcons, heightIcons, currentLamp, idLamp);
								groupObjects.getChildren().add(lamp);
								listLamps.add(lamp);
								break;
							case "thermo":
								thermoPosX = new SimpleDoubleProperty(Double.parseDouble(arrInput[1]));
								thermoPosY = new SimpleDoubleProperty(Double.parseDouble(arrInput[2]));
								currentThermo = arrInput[3].charAt(0);
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
					bufferedReader.close();
				}
				paneDrawingField.getChildren().addAll(groupObjects);
				for (SmartLamp lamp : listLamps)
					System.out.println(lamp.getX() + " || " + lamp.getY() + " -> " + lamp.getIdLamp());
				System.out.println("DONE");
				tpaneCenter.setTextFill(Color.BLUE);
				tpaneCenter.setText("Vorlage (nicht veränderbar)");
				tpaneCenter.setDisable(true);
				btnLamp.setDisable(true);
				btnThermo.setDisable(true);
				btnWall.setDisable(true);
				btnTrashCan.setDisable(true);
				txtfldName.setDisable(true);
				btnConfirmName.setDisable(true);
				btnSave.setDisable(true);
				btnCancel.setDisable(true);
				btnClear.setDisable(true);
				btnLoadTemplate.setText("Vorlage beenden");
				isTemplate = true;
			}
		} else {
			deleteAllObjects();
			stageManager.switchScene(FxmlView.CREATE);
		}
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
					alert.setHeaderText("Eine Datei mit der Bezeichnung \"" + txtfldName.getText() +
							"\" existiert bereits.");
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
				alert.setHeaderText("Die Datei konnte nicht gespeichert werden, da die eingegebene\nBezeichnung \"" +
						txtfldName.getText() + "\" nicht bestätigt wurde.");
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

	/*
	  public void mouseReleased(MouseEvent event)
	  is responsible for dropping the object
	* */
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

	public void setMoveIconsMode(boolean defaultMode) {
		this.moveIconsMode = defaultMode;
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

	public boolean isInsertTrashMode() {
		return insertTrashMode;
	}

	public void setInsertTrashMode(boolean insertTrashMode) {
		CreateController.insertTrashMode = insertTrashMode;
	}

	public boolean isInsertWallMode() {
		return insertWallMode;
	}

	public void setInsertWallMode(boolean wallMode) {
		this.insertWallMode = wallMode;
	}

	public Group getGroupObjects() {
		return groupObjects;
	}

	public void setGroupObjects(Group groupObjects) {
		this.groupObjects = groupObjects;
	}

	public Pane getPaneDrawingField() {
		return paneDrawingField;
	}

	public void setPaneDrawingField(Pane paneDrawingField) {
		this.paneDrawingField = paneDrawingField;
	}

	public char[][] getArrPhysicalLamps() {
		return arrPhysicalLamps;
	}

	public void setArrPhysicalLamps(char[][] arrPhysicalLamps) {
		CreateController.arrPhysicalLamps = arrPhysicalLamps;
	}

	public char[][] getArrPhysicalThermos() {
		return arrPhysicalThermos;
	}

	public void setArrPhysicalThermos(char[][] arrPhysicalThermos) {
		CreateController.arrPhysicalThermos = arrPhysicalThermos;
	}

	public Text getTxtThermo1() {
		return txtThermo1;
	}

	public void setTxtThermo1(Text txtThermo1) {
		this.txtThermo1 = txtThermo1;
	}

	public Text getTxtThermo2() {
		return txtThermo2;
	}

	public void setTxtThermo2(Text txtThermo2) {
		this.txtThermo2 = txtThermo2;
	}

	public Text getTxtThermo3() {
		return txtThermo3;
	}

	public void setTxtThermo3(Text txtThermo3) {
		this.txtThermo3 = txtThermo3;
	}

	public char getCorrespondingTextToDelete() {
		return correspondingTextToDelete;
	}

	public void setCorrespondingTextToDelete(char correspondingTextToDelete) {
		this.correspondingTextToDelete = correspondingTextToDelete;
	}

	public StageManager getStageManager() {
		return stageManager;
	}

	public void setStageManager(StageManager stageManager) {
		this.stageManager = stageManager;
	}


	//////////////////////////////// METHODS (SPECIAL)////////////////////////////////
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

		tpaneCenter.setTextFill(Color.RED);
		onlyValidCharsUsed = true;
		isTemplate = false;
		backgroundIsHidden = false;

		insertDefault(new ActionEvent());

		listPoints = new ArrayList<>();
		listLines = new ArrayList<>();
		listLamps = new ArrayList<>();
		listThermos = new ArrayList<>();
		groupObjects = new Group();
		paneDrawingField.getChildren().addAll(groupObjects);
		idWall = idLamp = idThermo = 0;
		mouseOnWall = false;
		mouseOnLamp = false;
		mouseOnThermo = false;

		currentLamp = '-';
		arrPhysicalLamps = new char[][]{{'R', '0'}, {'Y', '0'}, {'G', '0'}};
		currentThermo = '-';
		arrPhysicalThermos = new char[][]{{'1', '0'}, {'2', '0'}, {'3', '0'}};
		correspondingTextToDelete = '-';

		checkForThermoPosChanges();
	}

}


/*The class WallLine extends Line realizes the lines which should represent the walls*/
class WallLine extends Line {

	private final HomeController homeController = new HomeController();
	private final CreateController createController = new CreateController();
	private final OpenPlanController openPlanController = new OpenPlanController();
	int idLine;
	boolean selected = false;
	int lineThickness = 4;


	//////////////////////////////// METHODS ////////////////////////////////
	//Constructor
	public WallLine(DoubleProperty startX, DoubleProperty startY, DoubleProperty endX, DoubleProperty endY, int id) {
		idLine = id;
		startXProperty().bind(startX);
		startYProperty().bind(startY);
		endXProperty().bind(endX);
		endYProperty().bind(endY);
		setStrokeWidth(lineThickness);
		enableMouseActions();
	}

	/*
	private void enableMouseActions()
	This method first checks if the user is in the homeController window or in the createControl window and then if the
	user is also on the drawing field. Only if both conditions are fulfilled the user is able to draw walls.
	Therefore the user is not able to create wall objects in the toolbar or on the window frame.
	* */
	private void enableMouseActions() {
		setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertWallMode()) {
						createController.setMouseOnWall(true);
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertWallMode()) {
						openPlanController.setMouseOnWall(true);
					}
				}
			}
		});
		
	/*
	public void handle(MouseEvent event)
	In this method it is checked which window the user is currently in.
	Depending on the window, work continues with createController or with
	the home controller object, via the automatically assigned id of the wall objects
	* */
		setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertWallMode()) {
						if (! selected) {
							for (WallLine line : createController.getListLines()) {
								if (line.idLine == WallLine.this.idLine) {
									WallLine.this.setStroke(Color.RED);
									WallLine.this.toFront();
								} else {
									line.setStroke(Color.BLACK);
									line.setSelected(false);
								}
							}
							for (WallPoint point : createController.getListPoints()) {
								if (point.getIdPoint() == WallLine.this.idLine) {
									point.setStroke(Color.RED);
									point.toFront();
								} else point.setStroke(Color.BLACK);
							}
							selected = true;
						} else {
							WallLine.this.setStroke(Color.BLACK);
							for (WallPoint point : createController.getListPoints()) {
								if (point.getIdPoint() == WallLine.this.idLine) point.setStroke(Color.BLACK);
							}
							selected = false;
						}
					} else if (createController.isInsertTrashMode()) {
						for (WallLine line : createController.getListLines()) {
							if (line.idLine == WallLine.this.idLine) {

								WallLine.this.setVisible(false);
							}
						}
						for (WallPoint point : createController.getListPoints()) {
							if (point.getIdPoint() == WallLine.this.idLine) {

								point.setVisible(false);
							}
						}

						createController.getListLines().removeIf(line -> line.idLine == WallLine.this.idLine);
						createController.getListPoints().removeIf(point -> point.getIdPoint() == WallLine.this.idLine);
						createController.getGroupObjects().getChildren().remove(WallLine.this);
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertWallMode()) {
						if (! selected) {
							for (WallLine line : openPlanController.getListLines()) {
								if (line.idLine == WallLine.this.idLine) {
									WallLine.this.setStroke(Color.RED);
									WallLine.this.toFront();
								} else {
									line.setStroke(Color.BLACK);
									line.setSelected(false);
								}
							}
							for (WallPoint point : openPlanController.getListPoints()) {
								if (point.getIdPoint() == WallLine.this.idLine) {
									point.setStroke(Color.RED);
									point.toFront();
								} else point.setStroke(Color.BLACK);
							}
							selected = true;
						} else {
							WallLine.this.setStroke(Color.BLACK);
							for (WallPoint point : openPlanController.getListPoints()) {
								if (point.getIdPoint() == WallLine.this.idLine) point.setStroke(Color.BLACK);
							}
							selected = false;
						}
					} else if (openPlanController.isInsertTrashMode()) {
						for (WallLine line : openPlanController.getListLines()) {
							if (line.idLine == WallLine.this.idLine) {

								WallLine.this.setVisible(false);
							}
						}
						for (WallPoint point : openPlanController.getListPoints()) {
							if (point.getIdPoint() == WallLine.this.idLine) {

								point.setVisible(false);
							}
						}

						openPlanController.getListLines().removeIf(line -> line.idLine == WallLine.this.idLine);
						openPlanController.getListPoints().removeIf(point -> point.getIdPoint() == WallLine.this.idLine);
						openPlanController.getGroupObjects().getChildren().remove(WallLine.this);
					}
				}
			}
		});
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public int getIdLine() {
		return idLine;
	}

	public void setIdLine(int id) {
		this.idLine = id;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}


/*
 * class WallPoint extends Circle
 * This class represents the end points of a wall object. A wall object basically consists of 3 objects,
 * two endpoints which are each marked by a circle and the wall which is represented by a line.
 * This class deals with the circles
 * */
class WallPoint extends Circle {

	private final HomeController homeController = new HomeController();
	private final CreateController createController = new CreateController();
	private final OpenPlanController openPlanController = new OpenPlanController();
	int idPoint;
	static int pointRadius = 10;


	//////////////////////////////// METHODS ////////////////////////////////
	//Constructor
	public WallPoint(DoubleProperty centerX, DoubleProperty centerY, Color color, int id) {
		super(centerX.get(), centerY.get(), pointRadius);
		idPoint = id;

		setFill(Color.TRANSPARENT);
		setStroke(Color.BLACK);
		centerX.bind(centerXProperty());
		centerY.bind(centerYProperty());
		enableMouseActions();
	}

	/*This method allows the user to move the wall object at the end points. */
	private void enableMouseActions() {
		double[] arrPosXY = new double[2];

		setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertWallMode()) {
						System.out.println("point pressed");
						createController.setMouseOnWall(true);
						arrPosXY[0] = getCenterX() - mouseEvent.getX();        // difference of mouseclickpos <-> middle of point
						arrPosXY[1] = getCenterY() - mouseEvent.getY();
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertWallMode()) {
						System.out.println("point pressed");
						openPlanController.setMouseOnWall(true);
						arrPosXY[0] = getCenterX() - mouseEvent.getX();        // difference of mouseclickpos <-> middle of point
						arrPosXY[1] = getCenterY() - mouseEvent.getY();
					}
				}
			}
		});
		
		/*
		setOnMouseReleased(new EventHandler<MouseEvent>()
		Saves the new position of the moved object and displays it again in
		the terminal so it's easy to test because you get an overview if everything
		is done in the background
		*/
		setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertWallMode()) {
						WallPoint.this.toFront();
						handleCollisions(WallPoint.this);

						for (WallPoint point : createController.getListPoints()) {
							System.out.println(point.toString() + " -> id: " + point.getIdPoint());
						}
						System.out.println("this point has id: " + WallPoint.this.getIdPoint());
						System.out.println("# of points after released: " + createController.getListPoints().size());
						System.out.println();
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertWallMode()) {
						WallPoint.this.toFront();
						handleCollisions(WallPoint.this);

						for (WallPoint point : openPlanController.getListPoints()) {
							System.out.println(point.toString() + " -> id: " + point.getIdPoint());
						}
						System.out.println("this point has id: " + WallPoint.this.getIdPoint());
						System.out.println("# of points after released: " + openPlanController.getListPoints().size());
						System.out.println();
					}
				}
			}
		});
		
		/*
		  public void mouseDragged(MouseEvent event)
		* This method allows the user to move objects by holding down the left mouse button.
		* Depending on which object the user moves, as soon as the user releases the mouse state,
		* the coordinates of the mouse are passed to the object
		* (using the variables currentPosX and currentPosY),
		* so our program stays up to date with the current object positions.
		* Specific in this class is the method for the wallpoints
		* */
		setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertWallMode()) {
						double newX = mouseEvent.getX() + arrPosXY[0];
						if (newX > 0 && newX < getScene().getWidth()) {
							setCenterX(newX);
						}
						double newY = mouseEvent.getY() + arrPosXY[1];
						if (newY > 0 && newY < getScene().getHeight()) {
							setCenterY(newY);
						}
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertWallMode()) {
						double newX = mouseEvent.getX() + arrPosXY[0];
						if (newX > 0 && newX < getScene().getWidth()) {
							setCenterX(newX);
						}
						double newY = mouseEvent.getY() + arrPosXY[1];
						if (newY > 0 && newY < getScene().getHeight()) {
							setCenterY(newY);
						}
					}
				}
			}
		});
	}

	/*This method takes care of collisions of the wall points.
	  It is possible that wall points can overlap each other to get a visual connection
	*/
	private void handleCollisions(WallPoint wallPoint) {
		if (homeController.isInsideWindowCreate()) {
			for (WallPoint point : createController.getListPoints()) {
				if (! point.equals(wallPoint)) {
					if (wallPoint.intersects(point.getBoundsInLocal())) {
						wallPoint.setCenterX(point.getCenterX());
						wallPoint.setCenterY(point.getCenterY());
						break;
					}
				}
			}
			/*This part of the code is for the openPlanController it does exactly
			the same as the above code only for the OpenPlan window
			*/
		} else if (homeController.isInsideWindowOpenPlan()) {
			for (WallPoint point : openPlanController.getListPoints()) {
				if (! point.equals(wallPoint)) {
					if (wallPoint.intersects(point.getBoundsInLocal())) {
						wallPoint.setCenterX(point.getCenterX());
						wallPoint.setCenterY(point.getCenterY());
						break;
					}
				}
			}
		}
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public int getIdPoint() {
		return idPoint;
	}

	public void setIdPoint(int idPoint) {
		this.idPoint = idPoint;
	}

}


//class SmartLamp extends ImageView
class SmartLamp extends ImageView {

	private final HomeController homeController = new HomeController();
	private final CreateController createController = new CreateController();
	private final OpenPlanController openPlanController = new OpenPlanController();
	int idLamp;
	char correspondingPhysicalLamp;
	boolean isOn = false;


	//////////////////////////////// METHODS ////////////////////////////////
	//Constructor
	public SmartLamp(DoubleProperty xPos, DoubleProperty yPos, int width, int height, char correspondingPhysicalLamp, int id) {
		idLamp = id;

		if (homeController.isInsideWindowCreate())
			this.setImage(new Image("images/licht_an_" + correspondingPhysicalLamp + ".png"));
		else this.setImage(new Image("images/licht_aus_" + correspondingPhysicalLamp + ".png"));
		this.setX(xPos.doubleValue());
		this.setY(yPos.doubleValue());
		this.correspondingPhysicalLamp = correspondingPhysicalLamp;
		xPos.bind(xProperty());
		yPos.bind(yProperty());
		SmartLamp.this.setFitWidth(width);
		SmartLamp.this.setFitHeight(height);
		enableMouseActions();
	}

	/*
	 * private void enableMouseActions()
	 * This method allows the user to move lamp objects by clicking and holding down the lamp icon.
	 * By holding down the left mouse button, the user is able to move the lamp icon
	 * */
	private void enableMouseActions() {
		double[] arrPosXY = new double[2];
		setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						arrPosXY[0] = getX() - event.getX();
						arrPosXY[1] = getY() - event.getY();
						createController.setMouseOnLamp(true);
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						arrPosXY[0] = getX() - event.getX();
						arrPosXY[1] = getY() - event.getY();
						openPlanController.setMouseOnLamp(true);
					}
				}
			}
		});

		/*
		 * This part of the method allows the tester to get an overview of the situation after
		 * lamps have moved objects. Through the System.out.println the tester gets a feedback
		 * with which he can work to make sure that in the background everything is correct with
		 * the coordinates and ids.
		 * */
		setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						SmartLamp.this.toFront();

						for (SmartLamp lamp : createController.getListLamps()) {
							System.out.println(lamp.getX() + " || " + lamp.getY() + " -> id: " + lamp.getIdLamp());
						}
						System.out.println("this lamp has id: " + SmartLamp.this.getIdLamp());
						System.out.println("# of lamps after released: " + createController.getListLamps().size());
						System.out.println();
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						SmartLamp.this.toFront();

						for (SmartLamp lamp : openPlanController.getListLamps()) {
							System.out.println(lamp.getX() + " || " + lamp.getY() + " -> id: " + lamp.getIdLamp());
						}
						System.out.println("this lamp has id: " + SmartLamp.this.getIdLamp());
						System.out.println("# of lamps after released: " + openPlanController.getListLamps().size());
						System.out.println();
					}
				}
			}
		});

		/*
		 * This part of the method allows you to delete lamp icons. The delete function works in
		 * the scheme that the object is first set invisible and not clickable, then it is deleted from the lists.
		 * This method is used because we have no update of the scene. If we would not set the object invisible and
		 * not clickable the object would only disappear after the drawing field has been saved and reloaded.
		 * This procedure gives the user the illusion that the object has completely disappeared.
		 * */
		setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertTrashMode()) {
						for (SmartLamp lamp : createController.getListLamps()) {
							if (lamp.idLamp == SmartLamp.this.idLamp) {
								SmartLamp.this.setVisible(false);
								SmartLamp.this.setDisable(true);
								for (int i = 0; i < createController.getArrPhysicalLamps().length; i++) {
									if (createController.getArrPhysicalLamps()[i][0] == SmartLamp.this.correspondingPhysicalLamp) {
										createController.getArrPhysicalLamps()[i][1] = '0';
									}
								}
							}
						}
						createController.getListLamps().removeIf(lamp -> lamp.getIdLamp() == SmartLamp.this.idLamp);
						createController.getGroupObjects().getChildren().remove(SmartLamp.this);
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertTrashMode()) {
						for (SmartLamp lamp : openPlanController.getListLamps()) {
							if (lamp.idLamp == SmartLamp.this.idLamp) {
								SmartLamp.this.setVisible(false);
								SmartLamp.this.setDisable(true);
								for (int i = 0; i < openPlanController.getArrPhysicalLamps().length; i++) {
									if (openPlanController.getArrPhysicalLamps()[i][0] == SmartLamp.this.correspondingPhysicalLamp) {
										openPlanController.getArrPhysicalLamps()[i][1] = '0';
									}
								}
							}
						}
						openPlanController.getListLamps().removeIf(lamp -> lamp.getIdLamp() == SmartLamp.this.idLamp);
						openPlanController.getGroupObjects().getChildren().remove(SmartLamp.this);
					}
					/*This query allows a graphical modification of the lamp. Switching the lamp
					on and off not only changes the state of the hardware but also the image of the
					lamp icon on the application
					* */
					if (! openPlanController.isEditing()) {
						homeController.getComm().writeToPort(String.valueOf(correspondingPhysicalLamp));
						if (! isOn) {
							SmartLamp.this.setImage(new Image("images/licht_an_" + correspondingPhysicalLamp + ".png"));
							isOn = true;
							System.out.println("light " + correspondingPhysicalLamp + " turned on");
						} else {
							SmartLamp.this.setImage(new Image("images/licht_aus_" + correspondingPhysicalLamp + ".png"));
							isOn = false;
							System.out.println("light " + correspondingPhysicalLamp + " turned off");
						}
					}
				}
			}
		});
		
		/*This method allows you to update the mouse coordinates.
		First it is checked if the mouse is in the correct window,
		then it is ensured that the mouse is really in the scene.
		This is made possible by saying that it is greater than 0 and
		than the size of the field. This results in the if condition
		newX >0 && newX<getScene().getHeight()*/
		setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						double newX = event.getX() + arrPosXY[0];
						if (newX > 0 && newX < getScene().getWidth()) {
							setX(newX);
						}
						double newY = event.getY() + arrPosXY[1];
						if (newY > 0 && newY < getScene().getHeight()) {
							setY(newY);
						}
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						double newX = event.getX() + arrPosXY[0];
						if (newX > 0 && newX < getScene().getWidth()) {
							setX(newX);
						}
						double newY = event.getY() + arrPosXY[1];
						if (newY > 0 && newY < getScene().getHeight()) {
							setY(newY);
						}
					}
				}
			}
		});
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public int getIdLamp() {
		return idLamp;
	}

	public void setIdLamp(int idLamp) {
		this.idLamp = idLamp;
	}

	public char getCorrespondingPhysicalLamp() {
		return correspondingPhysicalLamp;
	}

	public void setCorrespondingPhysicalLamp(char correspondingPhysicalLamp) {
		this.correspondingPhysicalLamp = correspondingPhysicalLamp;
	}

	public boolean isOn() {
		return isOn;
	}

	public void setOn(boolean on) {
		isOn = on;
	}

}


////This class deals with the thermostats
class SmartThermo extends ImageView {

	private final HomeController homeController = new HomeController();
	private final CreateController createController = new CreateController();
	private final OpenPlanController openPlanController = new OpenPlanController();
	int idThermo;
	char correspondingPhysicalThermo;


	//////////////////////////////// METHODS ////////////////////////////////
	//Constructor
	public SmartThermo(DoubleProperty xPos, DoubleProperty yPos, int width, int height, char correspondingPhysicalThermo, int id) {
		idThermo = id;
		this.setImage(new Image("images/thermo3.png"));
		this.setX(xPos.doubleValue());
		this.setY(yPos.doubleValue());
		this.correspondingPhysicalThermo = correspondingPhysicalThermo;
		xPos.bind(xProperty());
		yPos.bind(yProperty());
		SmartThermo.this.setFitWidth(width);
		SmartThermo.this.setFitHeight(height);

		enableMouseActions();
	}

	private void enableMouseActions() {
		double[] arrPosXY = new double[2];

		setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						arrPosXY[0] = getX() - event.getX();
						arrPosXY[1] = getY() - event.getY();
						createController.setMouseOnThermo(true);
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						arrPosXY[0] = getX() - event.getX();
						arrPosXY[1] = getY() - event.getY();
						openPlanController.setMouseOnThermo(true);
					}
				}

			}
		});

		/*
		 * This part of the method allows you to delete thermostat icons. The delete function works in
		 * the scheme that the object is first set invisible and not clickable, then it is deleted from the lists.
		 * This method is used because we have no update of the scene. If we would not set the object invisible and
		 * not clickable the object would only disappear after the drawing field has been saved and reloaded.
		 * This procedure gives the user the illusion that the object has completely disappeared.
		 * It´s basicly the same method as for the lamp and for the wall object
		 * */
		setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isInsertTrashMode()) {
						for (SmartThermo thermo : createController.getListThermos()) {
							if (thermo.idThermo == SmartThermo.this.idThermo) {
								SmartThermo.this.setVisible(false);
								SmartThermo.this.setDisable(true);
								for (int i = 0; i < createController.getArrPhysicalThermos().length; i++) {
									if (createController.getArrPhysicalThermos()[i][0] == SmartThermo.this.correspondingPhysicalThermo) {
										createController.getArrPhysicalThermos()[i][1] = '0';
									}
								}
							}
						}
						createController.getListThermos().removeIf(thermo -> thermo.getIdThermo() == SmartThermo.this.idThermo);
						createController.getGroupObjects().getChildren().remove(SmartThermo.this);
						createController.setCorrespondingTextToDelete(SmartThermo.this.correspondingPhysicalThermo);
					}
					/*
					This part of the method is used to control the thermostat as well as the extinguishing is
					determined by the ID of the thermostat that is being activated. If you switch the thermostat off,
					a '0' is inserted at this point (similar to the lamps with the difference that you can only have 3 lamps).
					*/
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isInsertTrashMode()) {
						for (SmartThermo thermo : openPlanController.getListThermos()) {
							if (thermo.idThermo == SmartThermo.this.idThermo) {
								SmartThermo.this.setVisible(false);
								SmartThermo.this.setDisable(true);
								for (int i = 0; i < openPlanController.getArrPhysicalThermos().length; i++) {
									if (openPlanController.getArrPhysicalThermos()[i][0] == SmartThermo.this.correspondingPhysicalThermo) {
										openPlanController.getArrPhysicalThermos()[i][1] = '0';
									}
								}
							}
						}
						openPlanController.getListThermos().removeIf(thermo -> thermo.getIdThermo() == SmartThermo.this.idThermo);
						openPlanController.getGroupObjects().getChildren().remove(SmartThermo.this);
						openPlanController.setCorrespondingTextToDelete(SmartThermo.this.correspondingPhysicalThermo);
					}
				}
			}
		});

		/*
		 * This part of the method allows the tester to get an overview of the situation after
		 * lamps have moved objects. Through the System.out.println the tester gets a feedback
		 * with which he can work to make sure that in the background everything is correct with
		 * the coordinates and ids.The same was done above in the SmartLamp and Wall classes
		 * */
		setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						SmartThermo.this.toFront();

						for (SmartThermo thermo : createController.getListThermos()) {
							System.out.println(thermo.getX() + " || " + thermo.getY() + " -> id: " + thermo.getIdThermo());
						}
						System.out.println("this thermo has id: " + SmartThermo.this.getIdThermo());
						System.out.println("# of thermos after released: " + createController.getListThermos().size());
						System.out.println();
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						SmartThermo.this.toFront();

						for (SmartThermo thermo : openPlanController.getListThermos()) {
							System.out.println(thermo.getX() + " || " + thermo.getY() + " -> id: " + thermo.getIdThermo());
						}
						System.out.println("this thermo has id: " + SmartThermo.this.getIdThermo());
						System.out.println("# of thermos after released: " + openPlanController.getListThermos().size());
						System.out.println();
					}
				}
			}
		});
		
		/*This method allows you to update the mouse coordinates.
		First it is checked if the mouse is in the correct window,
		then it is ensured that the mouse is really in the scene.
		This is made possible by saying that it is greater than 0 and
		than the size of the field. This results in the if condition
		newX >0 && newX<getScene().getHeight()
		At the same time it is possible to move the thermostats
		by clicking and holding them and moving them
		*/
		setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (homeController.isInsideWindowCreate()) {
					if (createController.isMoveIconsMode()) {
						double newX = event.getX() + arrPosXY[0];

						if (newX > 0 && newX < getScene().getWidth()) {
							setX(newX);
						}

						double newY = event.getY() + arrPosXY[1];

						if (newY > 0 && newY < getScene().getHeight()) {
							setY(newY);
						}
					}
				} else if (homeController.isInsideWindowOpenPlan()) {
					if (openPlanController.isMoveIconsMode()) {
						double newX = event.getX() + arrPosXY[0];
						if (newX > 0 && newX < getScene().getWidth()) {
							setX(newX);
						}
						double newY = event.getY() + arrPosXY[1];
						if (newY > 0 && newY < getScene().getHeight()) {
							setY(newY);
						}
					}
				}
			}
		});
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public int getIdThermo() {
		return idThermo;
	}

	public void setIdThermo(int idThermo) {
		this.idThermo = idThermo;
	}

	public char getCorrespondingPhysicalThermo() {
		return correspondingPhysicalThermo;
	}

	public void setCorrespondingPhysicalThermo(char correspondingPhysicalThermo) {
		this.correspondingPhysicalThermo = correspondingPhysicalThermo;
	}

}