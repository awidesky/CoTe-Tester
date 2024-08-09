package io.github.awidesky.coTe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.processExecutor.ProcessExecutor;

public class Compiler {

	private static String compiler = null;
	private static ConsoleLogger logger = new ConsoleLogger();
	private static List<String> compilerCandidates;
	static {
		logger.setPrefix("[Compiler test] ");
		try {
			compilerCandidates = Stream.concat(
					Files.lines(Paths.get("compilers.txt")), 
					Stream.of("g++", "clang++", "cl.exe", "cl++")
					).toList();
		} catch (IOException e) {
			e.printStackTrace();
			compilerCandidates = List.of("g++", "clang++", "cl.exe", "cl++");
		}
	}
	
	public static String getCompiler() {
		if (compiler == null) {
            synchronized (Compiler.class) {
                if (compiler == null) {
                	compiler = findCompiler();
                }
            }
        }
        return compiler;
	}
	
	private static String findCompiler() {
		if(compiler != null) return compiler;
		List<String> workingCompilers = compilerCandidates.stream().filter(c -> {
			String[] command = { c, "--version" };
			logger.info();
			logger.debug("Testing Compiler with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
			try(Logger pl = new ConsoleLogger()) {
				pl.setPrefix("[Compiler test : " + c + "] ");
				return ProcessExecutor.runNow(pl, new File("."), command ) == 0;
			} catch (InterruptedException | ExecutionException | IOException e) {
				if(e.getLocalizedMessage().endsWith("No such file or directory"))
					logger.error(e.getLocalizedMessage());
				else 
					e.printStackTrace();
				return false;
			}
		}).toList();
		logger.info("Found compilers : " + workingCompilers.stream().collect(Collectors.joining(", ")));
		logger.info(workingCompilers.get(0) + " will used.");
		return workingCompilers.get(0);
	}
}
