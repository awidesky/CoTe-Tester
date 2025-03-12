package io.github.awidesky.coTe.compiler;

import java.io.File;

import io.github.awidesky.coTe.exception.CompileErrorException;
import io.github.awidesky.coTe.exception.CompileFailedException;
import io.github.awidesky.guiUtil.Logger;

public interface Compiler {
	public File compile(File outputDir, File cpp, Logger logger) throws CompileErrorException, CompileFailedException;
	public String[] testCommand();
	public String getCompilerExecutable();
}
