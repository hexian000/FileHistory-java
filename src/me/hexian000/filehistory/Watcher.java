package me.hexian000.filehistory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Watcher extends Thread {
	private final Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
	private final WatchService watchService;
	private final Consumer<WatcherEvent> consumer;
	private final Logger log;

	public Watcher(String path, Consumer<WatcherEvent> consumer, Logger logger) throws IOException {
		this.consumer = consumer;
		log = logger;
		watchService = FileSystems.getDefault().newWatchService();
		Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				register(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				consumer.accept(new WatcherEvent(WatcherEvent.EVENT_CREATE, file.toAbsolutePath().toString()));
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public int getWatchCount() {
		return keys.size();
	}

	private void register(Path path) {
		try {
			WatchKey key = path.register(watchService,
					StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_CREATE);
			keys.put(key, path);
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Watch failed: " + e.getMessage());
		}
	}

	private void processEvents() {
		while (!isInterrupted()) {
			WatchKey watchKey;
			try {
				watchKey = watchService.take();
			} catch (InterruptedException e) {
				break;
			}
			if (watchKey == null) {
				continue;
			}
			final Path dir = keys.get(watchKey);
			if (dir == null) {
				continue;
			}
			for (WatchEvent<?> event : watchKey.pollEvents()) {
				Path path = dir.resolve((Path) event.context());
				String pathStr = path.toAbsolutePath().toString();
				if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
					if (new File(pathStr).isDirectory()) {
						log.info("New watch: " + pathStr);
						register(path);
					}
					consumer.accept(new WatcherEvent(WatcherEvent.EVENT_CREATE, pathStr));
				} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
					consumer.accept(new WatcherEvent(WatcherEvent.EVENT_DELETE, pathStr));
				} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
					consumer.accept(new WatcherEvent(WatcherEvent.EVENT_MODIFY, pathStr));
				}
			}
			if (!watchKey.reset()) {
				keys.remove(watchKey);
				log.info("Unwatch: " + dir.toString());
			}
		}
	}

	@Override
	public void run() {
		try {
			processEvents();
		} catch (ClosedWatchServiceException e) {
			e.printStackTrace();
		} finally {
			try {
				watchService.close();
			} catch (IOException ignored) {
			}
		}
	}
}
