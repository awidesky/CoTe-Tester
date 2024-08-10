package io.github.awidesky.coTe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Worker {

	private static final ExecutorService threadpool = Executors.newSingleThreadExecutor();
	
	public static Future<?> submit(Runnable r) {
		return threadpool.submit(r);
	}
}
