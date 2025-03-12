package io.github.awidesky.coTe.compiler;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.coTe.MainFrame;
import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.guiUtil.level.Level;
import io.github.awidesky.processExecutor.ProcessExecutor;

public class PosixCompiler implements Compiler {
	
	private final String compiler;
	
	public PosixCompiler(String compiler) {
		this.compiler = compiler;
	}

	@Override
	public File compile(File outputDir, File cpp, Logger logger) throws CompileErrorException, CompileFailedException {
		File out = new File(outputDir, new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss_").format(new Date()) + cpp.getName() + ".out");
		List<String> command = new ArrayList<>();
		Stream.of(compiler, "--std=c++14", cpp.getAbsolutePath(), "-o", out.getAbsolutePath()).forEach(command::add);
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
		comp_logger.getString().lines().forEach(logger::debug);
		out.deleteOnExit();
		
		return out;
	}

	@Override
	public String[] testCommand() {
		return new String[] { compiler, "--version" };
	}

	@Override
	public String getCompilerExecutable() {
		return compiler;
	}

}
