package me.hexian000.filehistory.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.ResourceBundle;

class Utils {

	private final static ResourceBundle locale = ResourceBundle.getBundle("me.hexian000.filehistory.ui.locale");

	static String getLocalizedString(@NotNull String key) {
		return locale.getString(key);
	}

	static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
		java.util.List<Object> options = new ArrayList<>();
		Object defaultOption;
		switch (optionType) {
		case JOptionPane.OK_CANCEL_OPTION:
			options.add(UIManager.getString("OptionPane.okButtonText"));
			options.add(UIManager.getString("OptionPane.cancelButtonText"));
			defaultOption = UIManager.getString("OptionPane.cancelButtonText");
			break;
		case JOptionPane.YES_NO_OPTION:
			options.add(UIManager.getString("OptionPane.yesButtonText"));
			options.add(UIManager.getString("OptionPane.noButtonText"));
			defaultOption = UIManager.getString("OptionPane.noButtonText");
			break;
		case JOptionPane.YES_NO_CANCEL_OPTION:
			options.add(UIManager.getString("OptionPane.yesButtonText"));
			options.add(UIManager.getString("OptionPane.noButtonText"));
			options.add(UIManager.getString("OptionPane.cancelButtonText"));
			defaultOption = UIManager.getString("OptionPane.cancelButtonText");
			break;
		default:
			throw new IllegalArgumentException("Unknown optionType " + optionType);
		}
		return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE,
				null, options.toArray(), defaultOption);
	}
}
