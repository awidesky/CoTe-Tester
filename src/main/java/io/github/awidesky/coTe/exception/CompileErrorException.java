package io.github.awidesky.coTe.exception;

/**
 * Compile was failed because the code has compile error.
 */
public class CompileErrorException extends Exception {

	private static final long serialVersionUID = -7246270723772710393L;
	
	private final String compile_msg;

	/**
	 * Constructs a new CompileErrorException with the specified compile error message.
	 * @param errorMessage compile error message
	 */
	public CompileErrorException(String errorMessage) {
		super(errorMessage);
		compile_msg = errorMessage;
	}

	/**
	 * Get compile error message
	 * @return compile error message
	 */
	public String getCompile_msg() {
		return compile_msg;
	}
}
