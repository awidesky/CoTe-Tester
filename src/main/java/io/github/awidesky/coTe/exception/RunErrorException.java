package io.github.awidesky.coTe.exception;

public class RunErrorException extends CoTeException {

	private static final long serialVersionUID = 1086909894042655980L;

	public RunErrorException(int exitcode) {
		super("Run_Error", "Exit code : " + exitcode);
	}
	
	public RunErrorException(Throwable cause) {
		super("Run_Error", cause.toString(), cause);
	}

}
