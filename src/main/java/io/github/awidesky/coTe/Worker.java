package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class Worker {

	private static final ExecutorService threadpool = Executors.newSingleThreadExecutor();
	private static Logger logger = new ConsoleLogger(true);
	
	public static Future<?> submit(IntPair prob, File cpp, Consumer<Result> aftercallback) {
		logger.info("Submitted in thread pool..");
		return threadpool.submit(() -> {
			logger.debug("Running in thread pool");
			Result res = null;
			try (CoTe c = new CoTe(prob)) {
				logger.debug("Start test");
				res = c.test(cpp);
				logger.debug("CoTe.test() returned!");
				SwingDialogs.information(prob.toString(), res.result().str(), true);
			} catch (CompileFailedException e) {
				logger.error("Compile Failed!");
				logger.error(e);
				SwingDialogs.error(prob.toString() + " - " + res, "%e%", Objects.requireNonNullElse(e.getCause(), e), true);
			} catch (IOException e4) {
				logger.error(e4);
				SwingDialogs.error("Unable to read .in/.out File", "%e%", e4, true);
			} catch (Exception rest) {
				logger.error(rest);
				SwingDialogs.error("Unknown Exception", "%e%", rest, true);
			}
			
			if(res != null) {
				logger.info(res.result().str());
				if(res.coteException() != null) {
					logger.info(res.coteException().getMessage());
					logger.debug(res.coteException());
				}
				logger.info("[Result] " + prob.toString() + " : " + res);
				logger.newLine();
				logger.newLine();
			}
			aftercallback.accept(res);
		});
	}
}
