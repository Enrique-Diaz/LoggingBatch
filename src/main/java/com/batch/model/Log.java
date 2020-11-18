package com.batch.model;

import java.time.Instant;

public class Log {

	private String line;
	private Instant time;
	

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}
}