package me.hexian000.filehistory.ui;

import me.hexian000.filehistory.Repository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

class RepositoryBrowser extends JDialog {
	private static final DateFormat DISPLAY_FORMAT =
			new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

	static {
		DISPLAY_FORMAT.setTimeZone(TimeZone.getDefault());
	}

	private JPanel contentPane;
	private JButton buttonClose;
	private JList<String> listVersions;
	private JTextField textFile;
	private JButton buttonBrowseFile;
	private JButton buttonSave;
	private JButton buttonDelete;

	private final Repository repository;

	private RepositoryBrowser(final Repository repository, final String startPath) {
		this.repository = repository;

		setContentPane(contentPane);
		setModal(true);
		setTitle(Utils.getLocalizedString("repository_browser.title"));
		getRootPane().setDefaultButton(buttonBrowseFile);
		listVersions.setModel(new DefaultListModel<>());

		buttonClose.addActionListener(e -> onClose());

		buttonBrowseFile.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(Version.TITLE);
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setCurrentDirectory(new File(startPath));
			chooser.setFileHidingEnabled(false);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				refreshList(chooser.getSelectedFile());
			}
		});

		buttonSave.addActionListener(e -> {
			if (listVersions.getSelectedIndex() == -1) {
				return;
			}
			Date date;
			try {
				date = DISPLAY_FORMAT.parse(listVersions.getSelectedValue());
			} catch (ParseException ex) {
				ex.printStackTrace();
				return;
			}


			File repoFile = new File(textFile.getText());
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle(Utils.getLocalizedString("repository_browser.dialog_title_save"));
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setSelectedFile(repoFile);
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				if (file.exists()) {
					if (Utils.showConfirmDialog(this,
							Utils.getLocalizedString("repository_browser.dialog_overwrite"),
							Utils.getLocalizedString("dialog_title_confirm"),
							JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
						return;
					}
				}
				try {
					repository.fetchVersion(repoFile, date, chooser.getSelectedFile());
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(),
							Utils.getLocalizedString("dialog_title_error"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		buttonDelete.addActionListener(e -> {
			if (listVersions.getSelectedIndex() == -1) {
				return;
			}
			Date date;
			try {
				date = DISPLAY_FORMAT.parse(listVersions.getSelectedValue());
			} catch (ParseException ex) {
				ex.printStackTrace();
				return;
			}

			if (Utils.showConfirmDialog(this,
					Utils.getLocalizedString("repository_browser.dialog_confirm_no_undone"),
					Utils.getLocalizedString("dialog_title_confirm"),
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				try {
					repository.deleteVersion(new File(textFile.getText()), date);
				} catch (IOException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, ex.getLocalizedMessage(),
							Utils.getLocalizedString("dialog_title_error"),
							JOptionPane.ERROR_MESSAGE);
				} finally {
					refreshList(new File(textFile.getText()));
				}
			}
		});

		// call onClose() when cross is clicked
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onClose();
			}
		});

		// call onClose() on ESCAPE
		contentPane.registerKeyboardAction(e -> onClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	}

	private void refreshList(File file) {
		if (!file.exists() || !file.isFile()) {
			((DefaultListModel<String>) listVersions.getModel()).clear();
			return;
		}

		final java.util.List<Date> list = repository.listVersions(file);
		textFile.setText(file.toString());
		final DefaultListModel<String> model = (DefaultListModel<String>) listVersions.getModel();
		model.clear();
		for (Date date : list) {
			model.addElement(DISPLAY_FORMAT.format(date));
		}
	}

	private void onClose() {
		dispose();
	}

	static void show(Component parent, Repository repository, String startPath) {
		RepositoryBrowser dialog = new RepositoryBrowser(repository, startPath);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}
}
