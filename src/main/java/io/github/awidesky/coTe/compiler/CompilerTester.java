package io.github.awidesky.coTe.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.processExecutor.ProcessExecutor;
import io.github.awidesky.projectPath.JarPath;

public class CompilerTester {

	private static Compiler compiler = null;
	private static ConsoleLogger logger = new ConsoleLogger();
	private static List<Compiler> compilerCandidates;
	static {
		logger.setPrefix("[Compiler test] ");
		compilerCandidates = new LinkedList<Compiler>();
		try {
			Files.lines(Paths.get(JarPath.getProjectPath(CompilerTester.class), "compilers.txt")).map(PosixCompiler::new).forEach(compilerCandidates::add);
			compilerCandidates.addAll(findMSVC());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Stream.of("g++", "clang++", "cl.exe").map(PosixCompiler::new).forEach(compilerCandidates::add);
	}
	
	public static Compiler getCompiler() {
		if (compiler == null) {
            synchronized (CompilerTester.class) {
                if (compiler == null) {
                	compiler = findCompiler();
                }
            }
        }
        return compiler;
	}
	
	private static List<MSVCCompiler> findMSVC() throws IOException {
		//"C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
		File vsroot = new File("C:\\Program Files\\Microsoft Visual Studio");
		if(!vsroot.exists()) {
			logger.info(vsroot.getAbsolutePath() + " does not exist!");
			return List.of();
		}
		
		return Arrays.stream(vsroot.listFiles(File::isDirectory))
				.sorted((f1, f2) -> f2.getName().compareTo(f1.getName()))
				.map(f -> new File(f, "\\Community\\VC\\Auxiliary\\Build\\vcvars64.bat"))
				.filter(File::exists)
				.peek(f -> logger.info("vcvars64.bat found : " + f.getAbsolutePath()))
				.map(File::getAbsolutePath).map(MSVCCompiler::new)
				.filter(MSVCCompiler::isValid)
				.peek(m -> logger.info("Valid MSVC cl.exe : " + m.getCompilerExecutable()))
				.toList();
	}

	private static Compiler findCompiler() {
		if(compiler != null) return compiler;
		List<Compiler> workingCompilers = compilerCandidates.stream().filter(c -> {
			String[] command = c.testCommand();
			logger.info();
			logger.debug("Testing Compiler with : " + Arrays.stream(command).collect(Collectors.joining(" ")));
			try(Logger pl = new ConsoleLogger()) {
				pl.setPrefix("[Compiler test : " + c.getCompilerExecutable() + "] ");
				return ProcessExecutor.runNow(pl, new File("."), command) == 0;
			} catch (InterruptedException | ExecutionException | IOException e) {
				logger.error(e.getLocalizedMessage());
				logger.debug(e);
				return false;
			}
		}).toList();
		logger.info("Found compilers : " + workingCompilers.stream().map(Compiler::getCompilerExecutable).collect(Collectors.joining(", ")));
		logger.info(workingCompilers.get(0) + " will used.");
		return workingCompilers.get(0);
	}
}
