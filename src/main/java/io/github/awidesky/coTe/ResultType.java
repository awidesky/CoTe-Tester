package io.github.awidesky.coTe;

public enum ResultType {
	CORRECT("Correct!"),
	WRONG_ANSWER("Wrong Answer"),
	COMPILE_ERROR("Compile Error"),
	RUN_ERROR("Run Error"),
	TIME_OUT("Process Timeout");

	private final String str;
	ResultType(String str) {
		this.str = str;
	}
	public String str() {
		return str;
	}
}
