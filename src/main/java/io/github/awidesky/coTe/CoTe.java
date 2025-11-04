package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import io.github.awidesky.coTe.compiler.CompilerTester;
import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.coTe.exception.RunErrorException;
import io.github.awidesky.coTe.exception.TimeOutException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.processExecutor.ProcessExecutor.ProcessHandle;
import io.github.awidesky.processExecutor.ProcessIO;
import io.github.awidesky.projectPath.JarPath;


public class CoTe implements AutoCloseable {

	public static final int PROBLEMNUM = 100;
	private static final long processWaitSeconds = 10;
	private static File outputDir;
	public static Map<String, String> properties = new HashMap<>();
	static {
		try {
			Files.lines(Paths.get(JarPath.getProjectPath(CompilerTester.class), "properties.txt"))
			.filter(s -> s.contains("=")).map(s -> s.split("=")).forEach(arr -> properties.put(arr[0].strip(), arr[1].strip()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("\nProperties :");
		properties.entrySet().stream().map(e -> "\t" + e.getKey() + " : " + e.getValue()).forEach(System.out::println);
		System.out.println();
		
		outputDir = new File(properties.get("root"), properties.get("outputdir"));
		if(!outputDir.exists()) outputDir.mkdirs();
		else Arrays.stream(outputDir.listFiles()).parallel().forEach(File::delete);
	}

	private Logger logger;
	private int week;
	private int prob;
	private List<String> ioFiles;
	
	public CoTe(IntPair pair) {
		this(pair.week, pair.prob);
	}
	public CoTe(int week, int prob) {
		this.week = week;
		this.prob = prob;
		File ios = new File(properties.get("root"), properties.get("iodir"));
		ioFiles = IntStream.range(1, PROBLEMNUM)
					.mapToObj(i -> new File(ios + File.separator + format(properties.get("ioFiles"), i)))
					.takeWhile(File::exists)
					.map(File::getAbsolutePath)
					.map(s -> s.substring(0, s.lastIndexOf('.') + 1))
					.toList();
		
		if(ioFiles.isEmpty()) {
			throw new RuntimeException("Problem " + week + "_" + prob + " does not exists!");
		}

		logger = new ConsoleLogger();
		logger.setLogLevel(MainFrame.getDefaultLogLevel());
	}
	
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
	

	public Result test(File cpp) throws CompileFailedException, IOException {
		System.out.println("Problem : " + week + "_" + prob + "\twith " + cpp.getAbsolutePath());
		String out;
		boolean result = true;
		try {
			out = CompilerTester.getCompiler().compile(outputDir, cpp, logger).getAbsolutePath();
		} catch (CompileErrorException e) {
			return new Result(ResultType.COMPILE_ERROR, e);
		}
		for(String probFile : ioFiles) {
			List<String> outFile;
			
			String filename = probFile + properties.get("iext");
			StringFeeder sf = new StringFeeder(Paths.get(filename));
			filename = probFile + properties.get("oext");
			outFile = Files.readAllLines(Paths.get(filename), ProcessIO.getNativeChearset()); //TODO : set charset?
			try (Logger processOut = logger.withMorePrefix(String.format("[%4s | out] ", probFile.substring(probFile.lastIndexOf(File.separator) + 1)), false);
				 Logger processIn = logger.withMorePrefix("[" + probFile.substring(probFile.lastIndexOf(File.separator) + 1) + " | in ] ", false);
				 StringLogger output = new StringLogger(true);) {

				processOut.setLogLevel(logger.getLogLevel());
				processIn.setLogLevel(logger.getLogLevel());

				List<Integer> ioIndexList = new LinkedList<>();
				sf.setLogger(processIn);
				output.setPrintLogLevel(false);
				ProcessIO procIO = new ProcessIO(
						br -> {
							try (Logger clog = new ConsoleLogger()) {
								clog.setPrefix("[Process I/O] ");
								clog.setLogLevel(MainFrame.getDefaultLogLevel());
								clog.trace("ready readline");
								while(true) {
									String s = br.readLine();
									clog.trace("readline");
									if(s == null) return;

									ioIndexList.add(sf.getIndex());
									processOut.info(s);
									output.info(s);
								}
							} catch(IOException e) {
								throw new UncheckedIOException(e);
							}
						},
						br -> br.lines().forEach(processOut::error)
						);
				procIO.setStdin(sf);
			
				ProcessHandle handle = ProcessExecutor.run(List.of(out), null, procIO);
				if(!handle.getProcess().waitFor(processWaitSeconds , TimeUnit.SECONDS))
					return new Result(ResultType.TIME_OUT, new TimeOutException(processWaitSeconds , TimeUnit.SECONDS));
				
				int exitcode = handle.wait_all();
				processOut.info("Process done with exit code : " + exitcode);
				if(exitcode != 0) 
					return new Result(ResultType.RUN_ERROR , new RunErrorException(exitcode, sf.getElementOf(sf.getIndex()), sf.getIndex()));
				
				processOut.close();
				processIn.close();
				output.close();
				if(!diff(outFile.toArray(String[]::new), output.getString().split("\\R"), ioIndexList, sf,
						logger.withMorePrefix(String.format("[%sdiff] ", probFile.substring(probFile.lastIndexOf(File.separator) + 1)), false)))
					result = false;
				
				logger.newLine(); logger.newLine();
			} catch (IOException | ExecutionException | InterruptedException e) {
				return new Result(ResultType.RUN_ERROR, new RunErrorException(e, sf.getElementOf(sf.getIndex()), sf.getIndex()));
			}
			
		}
		return new Result(result ? ResultType.CORRECT : ResultType.WRONG_ANSWER, null);
	}

	private boolean diff(String[] original, String[] prog, List<Integer> ioIndexList, StringFeeder sf, Logger difLog) {
		if(original.length != prog.length) {
			difLog.info(original.length + "!=" + prog.length);
			difLog.info("Program output :");
			Arrays.stream(prog).forEach(difLog::info);
			return false;
		}
		
		boolean correct = true;
		for(int i = 0; i < original.length; i++) {
			if(!original[i].strip().equals(prog[i].strip())) {
				difLog.info();
				difLog.info("Wrong answer in Line " + (i + 1));
				difLog.info("Answer :");
				difLog.info(original[i]);
				difLog.info("Output :");
				difLog.info(prog[i]);
				
				String possibleIn = sf.getElementOf(ioIndexList.get(i));
				if(!possibleIn.isBlank()) {
					difLog.info("Input line " + (ioIndexList.get(i) + 1) + " is a possible input corresponds to the output :");
					difLog.info("\"" + possibleIn + "\"");
				}
				correct = false;
			}
		}
		
		if(correct) difLog.info(ResultType.CORRECT.str());
		else difLog.info(ResultType.WRONG_ANSWER.str());
		return correct;
	}
	
	private String format(String str) {
		return format(str, week, prob);
	}
	
	public static  String format(String str, int w, int p) {
		return str
				.replace("@w", String.valueOf(w))
				.replace("@W", String.format("%02d", w))
				.replace("@p", String.valueOf(p))
				.replace("@P", String.format("%02d", p));
	}
	
	private String format(String str, int d) {
		return format(str)
				.replace("@d", String.valueOf(d))
				.replace("@D", String.format("%02d", d));
	}
	
	@Override
	public void close() throws IOException {
		logger.close();
	}

}
