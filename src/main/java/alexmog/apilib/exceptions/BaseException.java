package alexmog.apilib.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "cause", "localizedMessage", "stackTrace", "suppressed" })
public class BaseException extends RuntimeException {
	private int status;
	
	public BaseException(int status, String message) {
		super(message);
		this.status = status;
	}
	
	public BaseException(int status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}
	
	public int getStatus() {
		return status;
	}
}
