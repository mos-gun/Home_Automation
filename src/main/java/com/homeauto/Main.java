package com.homeauto;

import com.homeauto.config.StageManager;
import com.homeauto.view.FxmlView;
import javafx.application.Application;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main extends Application {

	protected ConfigurableApplicationContext springContext;
	protected StageManager stageManager;

	
	public static void main(final String[] args) {
		Application.launch(args);
	}

	
	@Override
	public void init() throws Exception {
		springContext = springBootApplicationContext();
	}

	@Override
	public void start(Stage stage) throws Exception {
		stageManager = springContext.getBean(StageManager.class, stage);
		displayInitialScene();
		stage.setResizable(false);
	}

	@Override
	public void stop() throws Exception {
		springContext.close();
	}

	/*
	 * Nützlich für das Überschreiben dieser Methode durch Subklassen, welche die erste Szene,
	 * die nach dem Start der Applikation angezeigt werden soll, verändern möchten.
	 * Beispiel: Auf das Hauptfenster bezogene Tests.
	 */
	protected void displayInitialScene() {
		stageManager.switchScene(FxmlView.HOME);
	}

	private ConfigurableApplicationContext springBootApplicationContext() {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
		String[] args = getParameters().getRaw().stream().toArray(String[]::new);
		builder.headless(false);
		return builder.run(args);
	}

}