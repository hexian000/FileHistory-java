package me.hexian000.filehistory.ui;

import me.hexian000.filehistory.EventFilter;
import me.hexian000.filehistory.Repository;
import me.hexian000.filehistory.Watcher;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

class WatcherDaemon extends JFrame {
	private JPanel contentPane;
	private JButton buttonStart;
	private JButton buttonExit;
	private JTextField textFieldRepository;
	private JTextField textFieldWatch;
	private JList<String> listLog;
	private JButton buttonBrowseRepo;
	private JButton buttonBrowseWatch;
	private JButton buttonClearLog;
	private JButton buttonRepoBrowser;

	private Repository repository;
	private Runnable closeAll;

	WatcherDaemon(String repoPath, String watchPath) {
		setContentPane(contentPane);
		setTitle(Version.TITLE + " " + Version.format());
		getRootPane().setDefaultButton(buttonStart);
		textFieldRepository.setText(repoPath);
		textFieldWatch.setText(watchPath);

		listLog.setModel(new DefaultListModel<>());

		buttonStart.addActionListener(e -> onStart());

		buttonExit.addActionListener(e -> onExit());

		buttonBrowseRepo.addActionListener(e -> {
			File folder = chooseFolder();
			if (folder != null) {
				textFieldRepository.setText(folder.toString());
			}
		});

		buttonBrowseWatch.addActionListener(e -> {
			File folder = chooseFolder();
			if (folder != null) {
				textFieldWatch.setText(folder.toString());
			}
		});

		buttonClearLog.addActionListener(e -> ((DefaultListModel<String>) listLog.getModel()).clear());

		buttonRepoBrowser.addActionListener(e -> RepositoryBrowser.show(this, repository, textFieldWatch.getText()));

		// call onExit() when cross is clicked
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onExit();
			}
		});

		// call onExit() on ESCAPE
		contentPane.registerKeyboardAction(e -> onExit(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	private File chooseFolder() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(Version.TITLE);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	private final Consumer<String> logger = line -> SwingUtilities.invokeLater(() -> {
		final DefaultListModel<String> listModel = (DefaultListModel<String>) listLog.getModel();
		if (listModel.size() >= 256) {
			listModel.removeElementAt(0);
		}
		listModel.addElement(line);
		final int lastIndex = listModel.size() - 1;
		listLog.setSelectedIndex(lastIndex);
		listLog.ensureIndexIsVisible(lastIndex);
	});

	private void onStart() {
		final File watchDir = new File(textFieldWatch.getText());
		if (!watchDir.exists() || !watchDir.isDirectory()) {
			JOptionPane.showMessageDialog(this,
					Utils.getLocalizedString("watcher_daemon.dialog_watch_not_exist"),
					Utils.getLocalizedString("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		Repository r;
		try {
			r = new Repository(textFieldRepository.getText(), logger);
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getLocalizedMessage(),
					Utils.getLocalizedString("dialog_title_error"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		repository = r;
		EventFilter filter = new EventFilter(repository);
		Watcher watcher;
		try {
			watcher = new Watcher(textFieldWatch.getText(), filter, logger);
		} catch (Exception e) {
			e.printStackTrace();
			logger.accept("[ERROR] Error occurred: " + e.getMessage());
			return;
		}
		logger.accept("[INFO ] " + watcher.getWatchCount() + " watches created.");

		watcher.start();

		textFieldRepository.setEnabled(false);
		textFieldWatch.setEnabled(false);
		buttonBrowseRepo.setEnabled(false);
		buttonBrowseWatch.setEnabled(false);
		buttonStart.setEnabled(false);
		buttonRepoBrowser.setEnabled(true);

		closeAll = () -> {
			watcher.interrupt();
			try {
				watcher.join();
			} catch (InterruptedException ignored) {
			}
			filter.close();
			repository.close();
		};

		Runtime.getRuntime().addShutdownHook(new Thread(closeAll));
	}

	private void onExit() {
		if (closeAll != null) {
			closeAll.run();
		}

		dispose();
	}
}
