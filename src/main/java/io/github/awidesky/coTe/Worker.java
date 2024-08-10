package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.guiUtil.SwingDialogs;

public class Worker {

	private static final ExecutorService threadpool = Executors.newSingleThreadExecutor();
	
	
	public static Future<?> submit(IntPair prob, File cpp, Consumer<Boolean> aftercallback) {
		return threadpool.submit(() -> {
			boolean res = false;
			try (CoTe c = new CoTe(prob)) {
				res = c.test(cpp);
				SwingDialogs.information(prob.toString(), res ? "Correct!" : "Wrong Answer - check the log!", true);
			} catch (CompileErrorException e1) {
				SwingDialogs.error(prob.toString() + " - Compile Error!", "%e%", e1, true);
				//e1.printStackTrace(); //do not invoke because output of compile process is already printed to console.
			} catch (CompileFailedException e2) {
				SwingDialogs.error(prob.toString() + " - Compile Process Failed!", "%e%", e2, true);
				e2.printStackTrace();
			} catch (IOException e3) {
				SwingDialogs.error("Failed to handle I/O!", "%e%", e3, true);
				e3.printStackTrace();
			}
			aftercallback.accept(res);
		});
	}
}
