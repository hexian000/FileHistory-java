package me.hexian000.filehistory;

class WatcherEvent {
	static final int EVENT_CREATE = 1;
	static final int EVENT_DELETE = 2;
	static final int EVENT_MODIFY = 3;
	private final String path;
	long timestamp;
	private int event;

	WatcherEvent(int event, String path) {
		this.event = event;
		this.path = path;
		timestamp = System.currentTimeMillis();
	}

	void update(WatcherEvent event) {
		if (!path.equals(event.path)) {
			throw new IllegalArgumentException("update with different path");
		}
		this.event = event.event;
		this.timestamp = event.timestamp;
	}

	int getEvent() {
		return event;
	}

	String getPath() {
		return path;
	}
}
