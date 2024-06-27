package io.github.awidesky.coTe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


public class CoTe {

	private static String compiler = "g++";
	private static List<String> compilerCandidates = List.of("g++", "clang++", "cl++", "cl.exe");
	private static ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	private static void findCompiler() {
		List<String> workingCompilers = compilerCandidates.stream().filter(c -> {
			ProcessBuilder pb = new ProcessBuilder(c, "--version");
			//TODO : log(pb.list())
			try {
				Process p = pb.start();
				redirectOutput(p).wait();
				return p.waitFor() == 0;
			} catch (IOException | InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}).toList();
		System.out.println("Found compilers : " + workingCompilers.stream().collect(Collectors.joining(", ")));
		System.out.println(workingCompilers.get(0) + " will used.");
		compiler = workingCompilers.get(0);
	}
	
	private static Future<Void> redirectOutput(Process p) throws InterruptedException, ExecutionException {
		Future<?> f1 = threadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				//stdout.accept(br);
			} catch (IOException e) {
				//SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
				//		+ "\n%e%", e, false);
			}
		});
		Future<?> f2 = threadPool.submit(() -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
				//stderr.accept(br);
			} catch (IOException e) {
				//SwingDialogs.error("Unable to close process input stream!", "Process : " + command.stream().collect(Collectors.joining(" "))
				//		+ "\n%e%", e, false);
			}
		});
		return new Future<Void>() {
			@Override public boolean isDone() { return f1.isDone() && f2.isDone();	}
			@Override public boolean isCancelled() { return f1.isCancelled() && f2.isCancelled();	}
			@Override
			public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { return get(); }
			
			@Override
			public Void get() throws InterruptedException, ExecutionException {
				f1.get(); f2.get();
				return null;
			}
			
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) { f1.cancel(mayInterruptIfRunning); f2.cancel(mayInterruptIfRunning); return false; }
		};
	}

	private static void compile(File cpp) {
		String out = "." + File.separator + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + cpp.getName() + ".out";
		try {
			ProcessBuilder comp = new ProcessBuilder(compiler, "--std=c++14", cpp.getAbsolutePath(), "-o", out);
			//TODO : log(pb.list())
			Process comp_p = comp.start();
			redirectOutput(comp_p).wait();
			if(comp_p.waitFor() == 0);
			
			ProcessBuilder run = new ProcessBuilder(out);
			//TODO : log(pb.list())
			Process run_p = run.start();
		} catch (IOException | InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void test(File[] in, File cpp) {
		
	}
}
