package io.github.awidesky.coTe.exception;

public class CompileErrorException extends Exception {

	private static final long serialVersionUID = -7246270723772710393L;

	/**
	 * Constructs a new CompileErrorException with the specified compile error message.
	 * @param errorMessage compile error message
	 */
	public CompileErrorException(String errorMessage) {
		super(errorMessage);
	}
}
