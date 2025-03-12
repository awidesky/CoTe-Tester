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

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.guiUtil.ConsoleLogger;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.StringLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.processExecutor.ProcessExecutor;

public class MSVCCompiler implements Compiler {

	private final String cl;
	private final String vcvars64;
	private final String compileCommand;
	
	public MSVCCompiler(String vcvars64) {
		this.vcvars64 = vcvars64;
		cl = findCL();
		this.compileCommand = "\"%s\" > nul 2>&1 && \"%s\" ".formatted(vcvars64, cl);
	}
	
	private String findCL() {
		StringLogger clLogger = new StringLogger();
		ConsoleLogger log = new ConsoleLogger();
		
		try {
			if(ProcessExecutor.runNow(clLogger, new File("."), new String[] {"cmd", "/c", "\"%s\" && where cl.exe".formatted(vcvars64)}) != 0);
		} catch (InterruptedException | ExecutionException | IOException e) {
			log.error(e.getLocalizedMessage());
			log.debug(e);
			return null;
		} finally { log.close(); }
		
		List<String> l = clLogger.getString().lines().toList();
		l.forEach(log::info);
		
		return l.get(l.size() - 1);
	}
	
	public boolean isValid() {
		return cl != null && vcvars64 != null;
	}

	@Override
	public File compile(File outputDir, File cpp, Logger logger) throws CompileErrorException, CompileFailedException {
		outputDir = new File(outputDir, new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date()) + "_" + cpp.getName());
		outputDir.mkdirs();
		File out = new File(outputDir, cpp.getName() + ".exe");
		List<String> command = new ArrayList<>();
		Stream.of("cmd", "/c", compileCommand + 
				//"/ZI /nologo /W3 /WX- /diagnostics:column /sdl /Od /D _DEBUG /D _CONSOLE /D _UNICODE /D UNICODE /MDd /EHsc /GS /source-charset:utf-8 /std:c++14 " + 
				"/ZI /JMC /nologo /W3 /WX- /diagnostics:column /sdl /Od /D _DEBUG /D _CONSOLE /D _UNICODE /D UNICODE /Gm- /source-charset:utf-8 /EHsc /RTC1 /MDd /GS /fp:precise /Zc:wchar_t /Zc:forScope /Zc:inline /permissive- /external:W3 /Gd /TP /FC /errorReport:prompt " +
				 "/Fe\"" + out.getAbsolutePath() + "\" " + cpp.getAbsolutePath())
			.forEach(command::add);
		//TODO : if(MainFrame.getDefaultLogLevel().includes(Level.DEBUG)) command.add("/VERBOSE");
		
		logger.debug("Compiling with : " + command.stream().collect(Collectors.joining(" ")));
		StringLogger comp_logger = new StringLogger(true);
		comp_logger.setPrintLogLevel(false);
		try {
			if(ProcessExecutor.runNow(comp_logger, outputDir, command.toArray(String[]::new)) != 0) throw new CompileErrorException(comp_logger.getString());
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
		return new String[] {"cmd", "/c", "\"\"%s\" && \"%s\"\"".formatted(vcvars64, cl)} ;
	}

	@Override
	public String getCompilerExecutable() {
		return cl;
	}

}
