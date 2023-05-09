package com.homeauto.view;

import java.util.ResourceBundle;

public enum FxmlView {

	HOME {
		@Override
		public String getTitle() {
			return getStringFromResourceBundle("home.title");
		}

		@Override
		public String getFxmlFile() {
			return "/fxml/Home.fxml";
		}
	},

	CREATE {
		@Override
		public String getTitle() {
			return getStringFromResourceBundle("create.title");
		}

		@Override
		public String getFxmlFile() {
			return "/fxml/Create.fxml";
		}
	},
	
	OPENPLAN {
		@Override
		public String getTitle() {
			return getStringFromResourceBundle("openplan.title");
		}

		@Override
		public String getFxmlFile() {
			return "/fxml/OpenPlan.fxml";
		}
	};

	public abstract String getTitle();

	public abstract String getFxmlFile();

	String getStringFromResourceBundle(String key) {
		return ResourceBundle.getBundle("Bundle").getString(key);
	}

}
