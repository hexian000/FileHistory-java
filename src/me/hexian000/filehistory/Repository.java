package me.hexian000.filehistory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Repository implements Consumer<WatcherEvent> {
	private static final DateFormat ISO8601 =
			new SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss'Z'", Locale.getDefault());
	private static final Pattern FILENAME = Pattern.compile(
			"^(.*?)(\\.[^.]*)?$");
	private static final Pattern REPOSITORY_FILENAME = Pattern.compile(
			"^(.*?) \\((\\d{4}-\\d{2}-\\d{2}T\\d{2}_\\d{2}_\\d{2}Z)\\)(\\.[^.]*)?$");

	static {
		ISO8601.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private final BlockingQueue<String> tasks;
	private final Logger log;
	private final Thread backupThread;
	private final String path;
	private boolean closed = false;

	public Repository(String path) throws IOException {
		this(path, null);
	}

	public Repository(String path, Logger logger) throws IOException {
		log = logger;
		File repo = new File(path);
		if (!repo.exists()) {
			if (!repo.mkdirs()) {
				throw new IOException("mkdirs failed: " + repo.toString());
			}
		} else if (!repo.isDirectory()) {
			throw new IOException("path must be a directory: " + repo.toString());
		}
		this.path = path;
		tasks = new LinkedBlockingQueue<>();
		backupThread = new Thread(() -> {
			try {
				while (!closed && !Thread.interrupted()) {
					String task = tasks.take();
					if ("".equals(task)) {
						return;
					}
					try {
						backup(task);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (InterruptedException ignored) {
			}
		});
		backupThread.start();
	}

	private static String sanitizeFilename(String name) {
		return name.replaceAll("[:\\\\/*?|<>]", "_");
	}

	private static String[] sanitizePath(String path) {
		String[] parts = path.split(Pattern.quote(File.separator));
		for (int i = 0; i < parts.length; i++) {
			parts[i] = sanitizeFilename(parts[i]);
		}
		return parts;
	}

	@Override
	public void accept(WatcherEvent watcherEvent) {
		if (closed) {
			throw new IllegalStateException("repository is closed");
		}
		switch (watcherEvent.getEvent()) {
		case WatcherEvent.EVENT_CREATE:
		case WatcherEvent.EVENT_MODIFY:
			try {
				backup(watcherEvent.getPath());
			} catch (IOException e) {
				e.printStackTrace();
				log.error(e.getLocalizedMessage());
			}
			break;
		}
	}

	private String getRepositoryName(File file, Date date) {
		String[] parts = sanitizePath(file.getAbsolutePath());

		Matcher m = FILENAME.matcher(parts[parts.length - 1]);
		if (!m.find()) {
			throw new IllegalArgumentException("pattern mismatch");
		}
		String ext = m.group(2);
		parts[parts.length - 1] = m.group(1) + " (" + ISO8601.format(date) + ")" + (ext != null ? ext : "");

		return Paths.get(path, parts).toString();
	}

	public void fetchVersion(File file, Date version, File to) throws IOException {
		Files.copy(Paths.get(getRepositoryName(file, version)), to.toPath(),
				StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
	}

	public void deleteVersion(File file, Date version) throws IOException {
		Files.delete(Paths.get(getRepositoryName(file, version)));
	}

	public List<Date> listVersions(File file) {
		String[] parts = sanitizePath(file.getAbsolutePath());

		String filename = sanitizeFilename(file.getName());
		List<Date> result = new ArrayList<>();

		File parent = new File(Paths.get(path, parts).toString()).getParentFile();
		if (parent.exists() && parent.isDirectory()) {
			File[] files = parent.listFiles();
			if (files != null) {
				for (File f : files) {
					if (!f.isFile()) {
						continue;
					}
					Matcher m = REPOSITORY_FILENAME.matcher(f.getName());
					if (!m.find()) {
						final String message = "Non repository file: " + f.getAbsolutePath();
						System.err.println(message);
						log.warning(message);
						continue;
					}
					String name = m.group(1), ext = m.group(3);
					if (ext != null) {
						name += ext;
					}
					if (!filename.equals(name)) {
						continue;
					}
					try {
						result.add(ISO8601.parse(m.group(2)));
					} catch (ParseException ignored) {
					}
				}
			}
		}
		return result;
	}

	private void backup(String name) throws IOException {
		final File file = new File(name);
		if (!file.exists() || !file.isFile()) {
			return;
		}
		String repoPath = getRepositoryName(file, new Date(file.lastModified()));
		final File repoFile = new File(repoPath);
		if (repoFile.exists()) { // already has a backup
			return;
		}
		final File parent = repoFile.getParentFile();
		if (!parent.exists()) {
			if (!parent.mkdirs()) {
				throw new IOException("mkdirs failed: " + parent.toString());
			}
		}
		Files.copy(file.toPath(), repoFile.toPath(),
				StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		log.info(file.toString() + " -> " + repoPath + "");
	}

	public void close() {
		if (!closed) {
			closed = true;
			backupThread.interrupt();
			try {
				backupThread.join();
			} catch (InterruptedException ignored) {
			}
		}
	}
}
