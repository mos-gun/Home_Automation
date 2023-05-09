package com.homeauto.portComm;

import jssc.*;

import java.util.Scanner;

@SuppressWarnings("serial")
public class Communication implements SerialPortEventListener {

	
	//////////////////////////////// VARIABLES ////////////////////////////////
	Scanner scanner = new Scanner(System.in);
	
	// Vars for serial port
	private SerialPort serialPort;
	private final int baudRate = SerialPort.BAUDRATE_38400;
	private final int dataBits = SerialPort.DATABITS_8;
	private final int stopBits = SerialPort.STOPBITS_1;
	private final int parityBits = SerialPort.PARITY_NONE;
	private static boolean portConnected;

	// other Vars
	private String line;
	private static String temperature;
	private static char toggledLamp;
	private static boolean newTemperatureReceived;
	private static boolean newLightToggleReceived;


	//////////////////////////////// METHODS ////////////////////////////////
	//Constructor
	public Communication() {
		portConnected = false;
		newTemperatureReceived = false;
		newLightToggleReceived = false;
	}

	/*
	public void serialEvent(SerialPortEvent event)
	This method receives messages from the serial port . It checks for 'R', 'Y' and 'G'
	which stand for our 3 LEDs. Depending on whether one of the 3 characters is present in
	the message, it is set to true. It also checks the temperature and if the length of the
	message is 18 we know that the temperature was received and so we set the parameter for
	newTemperatureReceived to true. This allows us to continue working with the auxiliary
	variables in other methods.
	* */
	public void serialEvent(SerialPortEvent event) {
		try {
			line = serialPort.readString();

			if (line.length() == 18) {
				temperature = line;
				newTemperatureReceived = true;
			}
			if (line.contains("R")) {
				toggledLamp = 'R';
				newLightToggleReceived = true;
			}
			if (line.contains("Y")) {
				toggledLamp = 'Y';
				newLightToggleReceived = true;
			}
			if (line.contains("G")) {
				toggledLamp = 'G';
				newLightToggleReceived = true;
			}
		} catch (SerialPortException e) {
			e.printStackTrace();
			System.out.println("line couldn't be read!!!!!!!!!!!!!!!!");
		}
	}

	/*
	public void connect(String portUart)
	This method establishes the connection with the UART using the statically
	defined parameters baudRate, dataBits, stopBits and parityBits
	*/
	public void connect(String portUart) {
		serialPort = new SerialPort(portUart);
		try {
			serialPort.openPort();
			serialPort.setParams(baudRate, dataBits, stopBits, parityBits);
			serialPort.addEventListener(this);
			portConnected = true;
		} catch (SerialPortException e) {
			e.printStackTrace();
			System.out.println("serial port cant be openend!!!!!!!!!!!!!!!!");
		}
	}

	/*
	 *This method closes the connection to the UART
	 * */
	public void disconnect() {
		try {
			serialPort.closePort();
			portConnected = false;
		} catch (SerialPortException e) {
			System.out.println("not able to disconnect port!!!!!!!!!!!!!!!!");
		}
	}

	/*
	 * With this method it is possible to send messages to the UART
	 * */
	public void writeToPort(String text) {
		try {
			serialPort.writeString(text);
		} catch (SerialPortException e) {
			System.out.println("not able to write to port!!!!!!!!!!!!!!!!");
		}
	}


	//////////////////////////////// METHODS (GETTER & SETTER)////////////////////////////////
	public String[] getAvailableSerialPorts() {
		String[] portNames = SerialPortList.getPortNames();
		if (portNames.length == 0) {
			return null;
		}
		
		return portNames;
	}

	public boolean isPortConnected() {
		return portConnected;
	}

	public void setPortConnected(boolean portConnected) {
		this.portConnected = portConnected;
	}

	public char getToggledLamp() {
		return toggledLamp;
	}

	public void setToggledLamp(char toggledLamp) {
		Communication.toggledLamp = toggledLamp;
	}

	public String getTemperature() {
		return temperature;
	}

	public void setTemperature(String temperature) {
		Communication.temperature = temperature;
	}

	public boolean isNewTemperatureReceived() {
		return newTemperatureReceived;
	}

	public void setNewTemperatureReceived(boolean newTemperatureReceived) {
		Communication.newTemperatureReceived = newTemperatureReceived;
	}

	public boolean isNewLightToggleReceived() {
		return newLightToggleReceived;
	}

	public void setNewLightToggleReceived(boolean newLightToggleReceived) {
		Communication.newLightToggleReceived = newLightToggleReceived;
	}
	
}