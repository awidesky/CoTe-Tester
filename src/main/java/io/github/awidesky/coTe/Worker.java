package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.github.awidesky.coTe.exception.CoTeException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class Worker {

	private static final ExecutorService threadpool = Executors.newSingleThreadExecutor();
	private static Logger logger = new ConsoleLogger();
	
	public static Future<?> submit(IntPair prob, File cpp, Consumer<String> aftercallback) {
		return threadpool.submit(() -> {
			String res = null;
			try (CoTe c = new CoTe(prob)) {
				res = c.test(cpp) ? "Correct" : "Wrong Answer";
				SwingDialogs.information(prob.toString(), res, true);
			} catch (CoTeException e) {
				res = e.getMsg();
				e.getMsg().lines().forEach(logger::error);
				SwingDialogs.error(prob.toString() + " - " + res, "%e%", Objects.requireNonNullElse(e.getCause(), e), true);
			} catch (IOException e4) {
				logger.error(e4);
				SwingDialogs.error("Failed close logger!", "%e%", e4, true);
			}
			logger.info("[Result] " + prob.toString() + " : " + res);
			logger.newLine();
			logger.newLine();
			aftercallback.accept(res);
		});
	}
}
