package io.github.awidesky.coTe.exception;

public class CoTeException extends Exception {

	private static final long serialVersionUID = -7806008301370798315L;
	private String result;
	private String msg;
	
	public CoTeException(String result, String msg, Throwable cause) {
		super(msg, cause);
		this.result = result;
		this.msg = msg;
	}
	public CoTeException(String result, String msg) {
		super(msg);
		this.result = result;
		this.msg = msg;
	}

	public String getResult() {
		return result;
	}

	public String getMsg() {
		return msg;
	}
	
}
