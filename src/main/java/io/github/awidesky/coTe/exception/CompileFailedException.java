package io.github.awidesky.coTe.exception;

/**
 * Compile was failed, but not because the code is flawed, but the external process is failed.
 */
public class CompileFailedException extends Exception {

	private static final long serialVersionUID = 735253429330501639L;
	private final String processOutput;

	/**
	 * Constructs a new CompileFailedException with the specified compile error message.
	 * @param processOutput output of the compile process
	 * @param errorMessage compile error message
	 */
	public CompileFailedException(Throwable cause, String processOutput) {
		super(processOutput, cause);
		this.processOutput = processOutput;
	}

	/**
	 * return output of the failed compile process.
	 * @return output of the failed compile process
	 */
	public String getProcessOutput() {
		return processOutput;
	}
}
