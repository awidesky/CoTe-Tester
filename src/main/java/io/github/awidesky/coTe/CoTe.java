package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.coTe.exception.RunErrorException;
import io.github.awidesky.coTe.exception.TimeOutException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.level.Level;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.processExecutor.ProcessExecutor.ProcessHandle;
import io.github.awidesky.processExecutor.ProcessIO;


public class CoTe implements AutoCloseable {

	private static final long processWaitSeconds = 1000000;
	private static File outputDir = new File(MainFrame.getRoot(), "out");
	static {
		if(!outputDir.exists()) outputDir.mkdirs();
		else Arrays.stream(outputDir.listFiles()).parallel().forEach(File::delete);
	}

	private Logger logger;
	private int week;
	private int prob;
	private List<String> ioFiles;
	private boolean slowIO = false;
	
	public CoTe(IntPair pair) {
		this(pair.week, pair.prob);
	}
	public CoTe(int week, int prob) {
		this.week = week;
		this.prob = prob;
		File ios = new File(MainFrame.getRoot(), "IO");
		ioFiles = Arrays.stream(ios.listFiles())
				.filter(s -> s.getName().matches(week + "_" + prob + ".\\d.in"))
				.map(File::getAbsolutePath)
				.filter(s -> s.endsWith("in"))
				.map(s -> s.replace(".in", ""))
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
	
	private String compile(File cpp) throws CompileErrorException, CompileFailedException {
		File out = new File(outputDir, new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + cpp.getName() + ".out");
		List<String> command = new ArrayList<>();
		Stream.of(Compiler.getCompiler(), "--std=c++14", cpp.getAbsolutePath(), "-o", out.getAbsolutePath()).forEach(command::add);
		if(MainFrame.getDefaultLogLevel().includes(Level.DEBUG)) command.add("-v");
		
		logger.debug("Compiling with : " + command.stream().collect(Collectors.joining(" ")));
		StringLogger comp_logger = new StringLogger(true);
		comp_logger.setPrintLogLevel(false);
		try {
			if(ProcessExecutor.runNow(comp_logger, new File("."), command.toArray(String[]::new)) != 0) throw new CompileErrorException(comp_logger.getString());
		} catch (InterruptedException | ExecutionException | IOException e) {
			SwingDialogs.error("Error while compiling " + cpp, "%e%", e, true);
			throw new CompileFailedException(e, comp_logger.getString());
		}
		out.deleteOnExit();
		
		return out.getAbsolutePath();
	}

	public Result test(File cpp) throws CompileFailedException, IOException {
		logger.info("Problem : " + week + "_" + prob + " with " + cpp.getAbsolutePath());
		String out;
		boolean result = true;
		try {
			out = compile(cpp);
		} catch (CompileErrorException e) {
			return new Result(ResultType.COMPILE_ERROR, e);
		}
		for(String probFile : ioFiles) {
			List<String> outFile;
			
			String filename = probFile + ".in";
			StringFeeder sf = new StringFeeder(Paths.get(filename), slowIO );
			filename = probFile + ".out";
			outFile = Files.readAllLines(Paths.get(filename), ProcessIO.getNativeChearset());
			try (Logger processOut = logger.withMorePrefix(String.format("[%6s | out] ", probFile.substring(probFile.lastIndexOf(File.separator) + 1)), false);
				 Logger processIn = logger.withMorePrefix("[" + probFile.substring(probFile.lastIndexOf(File.separator) + 1) + " | in ] ", false);
				 StringLogger output = new StringLogger(true);) {

				processOut.setLogLevel(logger.getLogLevel());
				processIn.setLogLevel(logger.getLogLevel());

				List<Integer> ioIndexList = new LinkedList<>();
				sf.setLogger(processIn);
				output.setPrintLogLevel(false);
				ProcessIO procIO = new ProcessIO(
						br -> {
							try {
								System.out.println("ready readline");
								while(true) {
									String s = br.readLine();
									System.out.println("readline");
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
				logger.newLine(); logger.newLine();
				if(!diff(outFile.toArray(String[]::new), output.getString().split("\\R"), ioIndexList, sf)) result = false;
			} catch (IOException | ExecutionException | InterruptedException e) {
				return new Result(ResultType.RUN_ERROR, new RunErrorException(e, sf.getElementOf(sf.getIndex()), sf.getIndex()));
			}
			
		}
		return new Result(result ? ResultType.CORRECT : ResultType.WRONG_ANSWER, null);
	}

	private boolean diff(String[] original, String[] prog, List<Integer> ioIndexList, StringFeeder sf) {
		if(original.length != prog.length) {
			logger.info(original.length + "!=" + prog.length);
			logger.info("Program output :");
			Arrays.stream(prog).forEach(logger::info);
			return false;
		}
		
		boolean correct = true;
		for(int i = 0; i < original.length; i++) {
			if(!original[i].strip().equals(prog[i].strip())) {
				logger.info("Wrong answer in Line " + (i + 1));
				logger.info("Answer :");
				logger.info(original[i]);
				logger.info("Output :");
				logger.info(prog[i]);
				logger.info("Input line " +(ioIndexList.get(i) + 1) + " is a possible input corresponds to the output :");
				logger.info("\"" + sf.getElementOf(ioIndexList.get(i)) + "\"");
				logger.info();
				correct = false;
			}
		}
		
		if(correct) logger.info("Correct!");
		else logger.info("Wrong-answer!");
		return correct;
	}
	@Override
	public void close() throws IOException {
		logger.close();
	}

}
