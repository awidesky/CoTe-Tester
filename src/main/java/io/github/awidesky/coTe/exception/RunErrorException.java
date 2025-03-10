package io.github.awidesky.coTe.exception;

public class RunErrorException extends CoTeException {

	private static final long serialVersionUID = -6694125290473746317L;

	private String lastInput;
	private int lastInputIndex;

	public RunErrorException(int exitcode, String lastInput, int lastInputIndex) {
		this("Exit code : " + exitcode, lastInput, lastInputIndex);
	}
	
	public RunErrorException(Throwable cause, String lastInput, int lastInputIndex) {
		this(cause.toString(), lastInput, lastInputIndex);
	}
	
	private RunErrorException(String msg, String lastInput, int lastInputIndex) {
		super("Run_Error", msg + "\nPossible last input(line %d) : \n\"%s\"".formatted(lastInputIndex + 1, lastInput));
		this.lastInput = lastInput;
		this.lastInputIndex = lastInputIndex;
	}

	public String getLastInput() {
		return lastInput;
	}
	
	public int getLastInputIndex() {
		return lastInputIndex;
	}
	
}
