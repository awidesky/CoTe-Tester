package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.coTe.exception.TimeOutException;
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
				res = c.test(cpp) ? "Correct!" : "Wrong Answer!";
				SwingDialogs.information(prob.toString(), res, true);
			} catch (CompileErrorException e1) {
				res = "Compile Error!";
				e1.getCompile_msg().lines().forEach(logger::error);
				SwingDialogs.error(prob.toString() + " - " + res, "%e%", e1, true);
			} catch (CompileFailedException e2) {
				res = "Compile Process Failed!";
				e2.getProcessOutput().lines().forEach(logger::error);
				SwingDialogs.error(prob.toString() + " - " + res, "%e%", e2.getCause(), true);
			} catch (TimeOutException e3) {
				res = "Time limit!";
				SwingDialogs.error(prob.toString() + " - " + res, "%e%", e3, true);
			} catch (IOException e4) {
				res = "Compile Process Failed!";
				logger.error(e4);
				SwingDialogs.error("Failed to handle I/O!", "%e%", e4, true);
			}
			logger.info("[Result] " + prob.toString() + " : " + res);
			logger.newLine();
			logger.newLine();
			aftercallback.accept(res);
		});
	}
}
