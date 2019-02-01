package me.hexian000.filehistory;

import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
public class Logger {
	private final Consumer<String> logger;

	public Logger(Consumer<String> logger) {this.logger = logger;}

	public void info(String message) {
		if (logger != null) {
			logger.accept("[INFO ] " + message);
		}
	}

	public void warning(String message) {
		if (logger != null) {
			logger.accept("[WARN ] " + message);
		}
	}

	public void error(String message) {
		if (logger != null) {
			logger.accept("[ERROR] " + message);
		}
	}
}
