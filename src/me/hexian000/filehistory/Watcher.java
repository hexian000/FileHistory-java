package me.hexian000.filehistory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class Watcher extends Thread {
	private final Map<String, Watch> watches;
	private boolean fullyWatched;
	private final Consumer<WatcherEvent> consumer;

	public Watcher(String path, Consumer<WatcherEvent> consumer) {
		this.consumer = consumer;
		watches = new ConcurrentHashMap<>();
		fullyWatched = true;
		register(path);
	}

	public int getWatchCount() {
		return watches.size();
	}

	public boolean isFullyWatched() {
		return fullyWatched;
	}

	private void register(String path) {
		Watch watch = new Watch();
		watch.path = path;
		try {
			watch.watchService = FileSystems.getDefault().newWatchService();
			Paths.get(path).register(watch.watchService,
					StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE,
					StandardWatchEventKinds.ENTRY_CREATE);
			watches.put(path, watch);
		} catch (IOException e) {
			e.printStackTrace();
			fullyWatched = false;
			return;
		}

		File[] files = new File(path).listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					register(file.getAbsolutePath());
				} else if (file.isFile()) {
					consumer.accept(new WatcherEvent(WatcherEvent.EVENT_CREATE, file.getAbsolutePath()));
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				watches.values().removeIf((watch) -> {
					WatchKey watchKey = watch.watchService.poll();
					if (watchKey != null) {
						List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
						for (WatchEvent<?> event : watchEvents) {
							String path = Paths.get(watch.path, event.context().toString()).toString();
							if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
								if (new File(path).isDirectory()) {
									register(path);
								}
								consumer.accept(new WatcherEvent(WatcherEvent.EVENT_CREATE, path));
							} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
								consumer.accept(new WatcherEvent(WatcherEvent.EVENT_DELETE, path));
							} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
								consumer.accept(new WatcherEvent(WatcherEvent.EVENT_MODIFY, path));
							}
						}
						if (!watchKey.reset()) {
							try {
								watch.watchService.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							return true;
						}
					}
					return false;
				});
				Thread.sleep(200);
			}
		} catch (InterruptedException ignored) {
		} catch (ClosedWatchServiceException e) {
			e.printStackTrace();
		} finally {
			for (Watch watch : watches.values()) {
				try {
					watch.watchService.close();
				} catch (IOException ignored) {
				}
			}
			watches.clear();
		}
	}

	private class Watch {
		WatchService watchService;
		String path;
	}
}
