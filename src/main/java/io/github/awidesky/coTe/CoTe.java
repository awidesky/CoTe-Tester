package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.level.Level;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.processExecutor.ProcessIO;


public class CoTe {

	private static String compiler = "g++";
	private static List<String> compilerCandidates = List.of("g++", "clang++", "cl++", "cl.exe");
	private static LoggerThread lt = new LoggerThread();
	private static Logger staticLogger = lt.getLogger("[static] ");
	private static File root = new File("." + File.pathSeparator + "probs");
	private static File outputDir = new File(root, "out");
	
	static {
		findCompiler();
	}

	private Logger logger = lt.getLogger(null, Level.INFO);
	private int week;
	private int prob;
	private File problem;
	private List<String> ioFiles;
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public CoTe(int week, int prob) {
		this.week = week;
		this.prob = prob;
		this.problem = new File(root, week + "-" + prob + ".pdf");
		if(!problem.exists()) {
			throw new RuntimeException("Problem " + week + "-" + prob + " does not exists!");
			//SwingDialogs.error("Problem does not exists!", "Problem " + week + "-" + prob + " does not exists!", null, true);
		}
		File ios = new File(root, "IO");
		ioFiles = Arrays.stream(ios.listFiles()).map(File::getName)
				.filter(s -> s.endsWith("in"))
				.filter(s -> s.matches("sample\\-W" + week + "_P" + prob + ".\\d.in"))
				.map(s -> s.replace("in", ""))
				.toList();
	}
	
	private static void findCompiler() {
		List<String> workingCompilers = compilerCandidates.stream().filter(c -> {
			String[] command = { c, "--version" };
			staticLogger.debug("Testing Compiler with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
			try {
				return ProcessExecutor.runNow(lt.getLogger("[Compiler test] "), new File("."), command ) == 0;
			} catch (InterruptedException | ExecutionException | IOException e) {
				SwingDialogs.error("Error while checking " + c, "%e%", e, true);
				return false;
			}
		}).toList();
		System.out.println("Found compilers : " + workingCompilers.stream().collect(Collectors.joining(", ")));
		System.out.println(workingCompilers.get(0) + " will used.");
		compiler = workingCompilers.get(0);
	}
	
	private static String compile(File cpp) throws CompileErrorException {
		String out = outputDir.getAbsolutePath() + File.separator + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + cpp.getName() + ".out";
		String[] command = { compiler, "--std=c++14", cpp.getAbsolutePath(), "-o", out };
		staticLogger.debug("Compiling with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
		StringLogger comp_logger = new StringLogger(true);
		comp_logger.setPrintLogLevel(false);
		try {
			if(ProcessExecutor.runNow(comp_logger, new File("."), command) != 0) throw new CompileErrorException(comp_logger.getString());
		} catch (InterruptedException | ExecutionException | IOException e) {
			SwingDialogs.error("Error while compiling " + cpp, "%e%", e, true);
			throw null;
		}
		
		return out;
	}

	public void test(File cpp) throws CompileErrorException {
		logger.info(week + " - " + prob + "  " + cpp.getAbsolutePath());
		String out = compile(cpp);
		
		ioFiles.forEach(io -> {
			List<String> inFile;
			List<String> outFile;
			
			String filename = io + ".in";
			try {
				inFile = Files.readAllLines(Paths.get(filename), Charset.forName(System.getProperty("native.encoding"))); //TODO : use ProcessIO.NATIVECHARSET
				filename = io + ".out";
				outFile = Files.readAllLines(Paths.get(filename), Charset.forName(System.getProperty("native.encoding")));
			} catch (IOException e) {
				SwingDialogs.error("Unable to read io File : " + filename, "%e%", e, true);
				return;
			}
			Logger l = lt.getLogger("[" + io + "] ");
			StringLogger output = new StringLogger(true);
			output.setPrintLogLevel(false);
			ProcessIO procIO = new ProcessIO(br -> br.lines().forEach(output::info), br -> br.lines().forEach(l::error));
			procIO.setStdin(inFile.stream());
			try {
				ProcessExecutor.run(List.of(out), null, procIO);
				l.close();
				output.close();
			} catch (IOException e) {
				SwingDialogs.error("Unable to run executable: " + out, "%e%", e, true);
			}
			diff(outFile.toArray(String[]::new), output.getString().split("\\R"));
		});
	}

	private void diff(String[] original, String[] prog) {
		
	}
	
	
}
