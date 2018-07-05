package me.hexian000.filehistory.ui;

final class Version {
	static final String TITLE = "File History";
	static final String COPYRIGHT = "By: He Xian Copyright (c) 2018";
	private static final int MAJOR = 0;
	private static final int MINOR = 4;
	private static final int REVISION = 0;
	private static final String TAG = "alpha";

	static String format() {
		StringBuilder sb = new StringBuilder();
		sb.append(MAJOR).append('.').append(MINOR);
		if (REVISION != 0) {
			sb.append('.').append(REVISION);
		}
		if (TAG.length() > 0) {
			sb.append('-').append(TAG);
		}
		return sb.toString();
	}
}
