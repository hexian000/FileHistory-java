package me.hexian000.filehistory.ui;

import javax.swing.*;

class Main {

	public static void main(String[] args) {
		System.err.println(Version.TITLE + " " + Version.format());
		System.err.println("  " + Version.COPYRIGHT);
		System.err.println();

		try {
			// Set System L&F
			UIManager.setLookAndFeel(
					UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException |
				ClassNotFoundException |
				InstantiationException |
				IllegalAccessException e) {
			e.printStackTrace();
		}

		WatcherDaemon watcherDaemon = null;
		if (args.length == 2) {
			if (args[1].startsWith(args[0])) {
				System.err.println("Error: repository must not be a sub-directory of watch");
				System.err.println();
			} else {
				watcherDaemon = new WatcherDaemon(args[0], args[1]);
			}
		}
		if (watcherDaemon == null) {
			watcherDaemon = new WatcherDaemon("", "");
		}
		watcherDaemon.pack();
		watcherDaemon.setLocationRelativeTo(null);
		watcherDaemon.setVisible(true);
	}
}
