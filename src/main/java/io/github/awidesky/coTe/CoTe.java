package io.github.awidesky.coTe;

import java.io.Closeable;
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
import java.util.stream.Stream;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.level.Level;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.processExecutor.ProcessIO;


public class CoTe implements Closeable {

	private static String compiler = null;
	private static List<String> compilerCandidates;
	private LoggerThread lt = new LoggerThread();
	private static File root = new File("probs");
	private static File outputDir = new File(root, "out");
	

	private Logger logger = lt.getLogger(null, Level.DEBUG);
	private int week;
	private int prob;
	private List<String> ioFiles;
	
	static {
		try {
			compilerCandidates = Stream.concat(
					Files.lines(Paths.get("compilers.txt")), 
					Stream.of("g++", "clang++", "cl.exe", "cl++")
					).toList();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public CoTe(int week, int prob) {
		this.week = week;
		this.prob = prob;
		File ios = new File(root + File.separator + "IO");
		//TODO : debug : Arrays.stream(ios.listFiles()).map(File::getName).forEach(System.out::println);
		ioFiles = Arrays.stream(ios.listFiles())
				.filter(s -> s.getName().matches(week + "_" + prob + ".\\d.in"))
				.map(File::getAbsolutePath)
				.filter(s -> s.endsWith("in"))
				.map(s -> s.replace(".in", ""))
				.toList();
		if(ioFiles.isEmpty()) {
			throw new RuntimeException("Problem " + week + "_" + prob + " does not exists!");
			//SwingDialogs.error("Problem does not exists!", "Problem " + week + "-" + prob + " does not exists!", null, true);
		}
		
		lt.setLogDestination(System.out);
		lt.start();
		findCompiler();
	}
	
	private void findCompiler() {
		if(compiler != null) return;
		List<String> workingCompilers = compilerCandidates.stream().filter(c -> {
			String[] command = { c, "--version" };
			logger.info();
			logger.debug("Testing Compiler with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
			try {
				return ProcessExecutor.runNow(lt.getLogger("[Compiler test : " + c + "] "), new File("."), command ) == 0;
			} catch (InterruptedException | ExecutionException | IOException e) {
				//SwingDialogs.error("Error while checking " + c, "%e%", e, true);
				if(e.getLocalizedMessage().endsWith("No such file or directory"))
					System.err.println(e.getLocalizedMessage());
				else 
					e.printStackTrace();
				System.err.flush();
				return false;
			}
		}).toList();
		System.out.println("Found compilers : " + workingCompilers.stream().collect(Collectors.joining(", ")));
		System.out.println(workingCompilers.get(0) + " will used.");
		compiler = workingCompilers.get(0);
	}
	
	private String compile(File cpp) throws CompileErrorException {
		String out = outputDir.getAbsolutePath() + File.separator + new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + cpp.getName() + ".out";
		String[] command = { compiler, "--std=c++14", cpp.getAbsolutePath(), "-v", "-o", out };
		logger.debug("Compiling with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
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
		logger.info("Problem : " + week + "_" + prob + " with " + cpp.getAbsolutePath());
		String out = compile(cpp);
		ioFiles.forEach(probFile -> {
			List<String> inFile;
			List<String> outFile;
			
			String filename = probFile + ".in";
			try {
				inFile = Stream.concat(Files.lines(Paths.get(filename), Charset.forName(System.getProperty("native.encoding"))), Stream.of("\n")).toList(); //TODO : use ProcessIO.NATIVECHARSET
				filename = probFile + ".out";
				outFile = Files.readAllLines(Paths.get(filename), Charset.forName(System.getProperty("native.encoding")));
			} catch (IOException e) {
				SwingDialogs.error("Unable to read io File : " + filename, "%e%", e, true);
				return;
			}
			Logger processOut = lt.getLogger("[" + week + "_" + prob + " | out] ", Level.DEBUG); //TODO : debug 여부는 config.ini에서 결
			Logger processIn = lt.getLogger("[" + week + "_" + prob + " | in ] ", Level.DEBUG); //TODO : debug 여부는 config.ini에서 결
			StringLogger output = new StringLogger(true);
			output.setPrintLogLevel(false);
			ProcessIO procIO = new ProcessIO(
					br -> br.lines().forEach(s -> { processOut.debug(s); output.info(s); }),
					br -> br.lines().forEach(processOut::error)
					);
			procIO.setStdin(inFile.stream().map(s -> { processIn.debug(s); return s; }));
			try {
				ProcessExecutor.run(List.of(
						out
						// "/Users/eugenehong/Test/echo.out"//TODO : delete!
						), null, procIO).wait_all();
				processOut.close();
				output.close();
			} catch (IOException | ExecutionException | InterruptedException e) {
				SwingDialogs.error("Unable to run executable: " + out, "%e%", e, true);
			}
			processOut.info("Process done!");
			diff(outFile.toArray(String[]::new), output.getString().split("\\R"));
		});
	}

	private void diff(String[] original, String[] prog) {
		if(original.length != prog.length) {
			//TODO
			System.out.println(original.length + "!=" + prog.length);
			System.out.println("Program output :");
			Arrays.stream(prog).forEach(System.out::println);
			return;
		}
		
		boolean correct = true;
		for(int i = 0; i < original.length; i++) {
			if(!original[i].strip().equals(prog[i].strip())) {
				System.out.println("In Line " + (i + 1));
				System.out.println("Answer :");
				System.out.println(original[i]);
				System.out.println("Output :");
				System.out.println(prog[i]);
				System.out.println();
				correct = false;
			}
		}
		
		if(correct) System.out.println("Correct!");
		else System.out.println("Wrong-answer!");
	}

	@Override
	public void close() {
		lt.shutdown(5000);
	}
	
}
