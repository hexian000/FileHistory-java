package me.hexian000.filehistory;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventFilter implements Consumer<WatcherEvent> {
	private static final long TIMEOUT = 30000; // 30 seconds
	private final Timer timeout;
	private final Map<String, WatcherEvent> fileMap;
	private boolean closed = false;

	public EventFilter(final Consumer<WatcherEvent> consumer) {
		fileMap = new ConcurrentHashMap<>();
		timeout = new Timer();
		timeout.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				synchronized (timeout) {
					fileMap.entrySet().removeIf((item) -> {
						final WatcherEvent event = item.getValue();
						if (System.currentTimeMillis() - event.timestamp > TIMEOUT) {
							consumer.accept(event);
							return true;
						}
						return false;
					});
				}
			}
		}, 5000, 5000);
	}

	@Override
	public void accept(WatcherEvent watcherEvent) {
		if (closed) {
			throw new IllegalStateException("EventFilter is closed");
		}
		WatcherEvent item = fileMap.get(watcherEvent.getPath());
		if (item == null) {
			fileMap.put(watcherEvent.getPath(), watcherEvent);
		} else {
			item.update(watcherEvent);
		}
	}

	public void close() {
		if (!closed) {
			closed = true;
			timeout.cancel();
			synchronized (timeout) { // wait until timer task finished
			}
			fileMap.clear();
		}
	}
}
