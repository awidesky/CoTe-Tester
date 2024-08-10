package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
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
				SwingDialogs.error(prob.toString() + " - Compile Error!", "%e%", e1, true);
			} catch (CompileFailedException e2) {
				res = "Compile Process Failed!";
				logger.error(e2);
				SwingDialogs.error(prob.toString() + " - Compile Process Failed!", "%e%", e2, true);
			} catch (IOException e3) {
				res = "Compile Process Failed!";
				logger.error(e3);
				SwingDialogs.error("Failed to handle I/O!", "%e%", e3, true);
			}
			logger.info("[Result] " + prob.toString() + " : " + res);
			logger.newLine();
			logger.newLine();
			aftercallback.accept(res);
		});
	}
}
