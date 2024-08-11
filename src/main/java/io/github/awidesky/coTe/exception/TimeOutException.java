package io.github.awidesky.coTe.exception;

import java.util.concurrent.TimeUnit;

public class TimeOutException extends CoTeException {

	private static final long serialVersionUID = 3367935361815408789L;

	public TimeOutException(long time, TimeUnit unit) {
		super("Time limit", "Time limit " + time + unit.toString() + " was exeeded!");
	}

}
