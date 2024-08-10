package io.github.awidesky.coTe.exception;

/**
 * Compile was failed, but not because the code is flawed, but the external process is failed.
 */
public class CompileFailedException extends Exception {

	private static final long serialVersionUID = 735253429330501639L;

	/**
	 * Constructs a new CompileFailedException with the specified compile error message.
	 * @param errorMessage compile error message
	 */
	public CompileFailedException(Throwable cause) {
		super(cause);
	}
}
