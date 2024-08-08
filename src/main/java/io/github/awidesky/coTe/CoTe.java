package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SimpleLogger;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.level.Level;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.processExecutor.ProcessIO;


public class CoTe {

	private static File outputDir = new File(MainFrame.getRoot(), "out");

	private Logger logger;
	private Level logLevel;
	private int week;
	private int prob;
	private List<String> ioFiles;
	private OutputStream logTo = System.out;
	
	public CoTe(IntPair pair) {
		this(pair.week, pair.prob);
	}
	public CoTe(IntPair pair, Level logLeve) {
		this(pair.week, pair.prob, logLeve);
	}
	public CoTe(int week, int prob) {
		this(week, prob, Level.INFO);
	}
	public CoTe(int week, int prob, Level logLevel) {
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

		this.logLevel = logLevel;
		logger = new SimpleLogger(logTo);
		logger.setLogLevel(logLevel);
	}
	
	private String compile(File cpp) throws CompileErrorException {
		File out = new File(outputDir, new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + cpp.getName() + ".out");
		String[] command = { Compiler.getCompiler(), "--std=c++14", cpp.getAbsolutePath(), "-v", "-o", out.getAbsolutePath() };
		logger.debug("Compiling with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
		StringLogger comp_logger = new StringLogger(true);
		comp_logger.setPrintLogLevel(false);
		try {
			if(ProcessExecutor.runNow(comp_logger, new File("."), command) != 0) throw new CompileErrorException(comp_logger.getString());
		} catch (InterruptedException | ExecutionException | IOException e) {
			SwingDialogs.error("Error while compiling " + cpp, "%e%", e, true);
			throw null;
		}
		out.deleteOnExit();
		
		return out.getAbsolutePath();
	}

	public boolean test(File cpp) throws CompileErrorException {
		logger.info("Problem : " + week + "_" + prob + " with " + cpp.getAbsolutePath());
		String out = compile(cpp);
		return ioFiles.stream().map(probFile -> {
			List<String> inFile;
			List<String> outFile;
			
			String filename = probFile + ".in";
			try {
				inFile = Stream.concat(Files.lines(Paths.get(filename), ProcessIO.getNativeChearset()), Stream.of("\n")).toList(); //TODO : use ProcessIO.NATIVECHARSET
				filename = probFile + ".out";
				outFile = Files.readAllLines(Paths.get(filename), ProcessIO.getNativeChearset());
			} catch (IOException e) {
				SwingDialogs.error("Unable to read io File : " + filename, "%e%", e, true);
				return false;
			}
			Logger processOut = new SimpleLogger(logTo);
			processOut.setPrefix("[" + probFile.substring(probFile.lastIndexOf(File.separator) + 1) + " | out] ");
			processOut.setLogLevel(logLevel); //TODO : debug 여부는 config.ini에서 결
			Logger processIn = new SimpleLogger(logTo);
			processIn.setPrefix("[" + probFile.substring(probFile.lastIndexOf(File.separator) + 1) + " | in ] ");
			processIn.setLogLevel(logLevel);
			
			StringLogger output = new StringLogger(true);
			output.setPrintLogLevel(false);
			ProcessIO procIO = new ProcessIO(
					br -> br.lines().forEach(s -> { processOut.debug(s); output.info(s); }),
					br -> br.lines().forEach(processOut::error)
					);
			procIO.setStdin(inFile.stream().map(s -> { processIn.debug(s); return s; }));
			try {
				ProcessExecutor.run(List.of(out), null, procIO).wait_all();
				processOut.info("Process done!");
				processOut.close();
				processIn.close();
				output.close();
			} catch (IOException | ExecutionException | InterruptedException e) {
				SwingDialogs.error("Unable to run executable: " + out, "%e%", e, true);
			}
			return diff(outFile.toArray(String[]::new), output.getString().split("\\R"));
		}).allMatch(Boolean::booleanValue);
	}

	private boolean diff(String[] original, String[] prog) {
		if(original.length != prog.length) {
			//TODO
			logger.info(original.length + "!=" + prog.length);
			logger.info("Program output :");
			Arrays.stream(prog).forEach(logger::info);
			return false;
		}
		
		boolean correct = true;
		for(int i = 0; i < original.length; i++) {
			if(!original[i].strip().equals(prog[i].strip())) {
				logger.info("In Line " + (i + 1));
				logger.info("Answer :");
				logger.info(original[i]);
				logger.info("Output :");
				logger.info(prog[i]);
				logger.info();
				correct = false;
			}
		}
		
		if(correct) logger.info("Correct!");
		else logger.info("Wrong-answer!");
		return correct;
	}

}
