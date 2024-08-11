package io.github.awidesky.coTe.exception;

/**
 * Compile was failed, but not because the code is flawed, but the external process is failed.
 */
public class CompileFailedException extends CoTeException {

	private static final long serialVersionUID = 735253429330501639L;

	/**
	 * Constructs a new CompileFailedException with the specified compile error message.
	 * @param processOutput output of the compile process
	 * @param errorMessage compile error message
	 */
	public CompileFailedException(Throwable cause, String processOutput) {
		super("Compile Process Failed", processOutput + "\n" + cause.toString(), cause);
	}

}
