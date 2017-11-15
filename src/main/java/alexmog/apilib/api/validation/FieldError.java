package alexmog.apilib.api.validation;

public class FieldError {
	private String mField, mMessage;
	
	public FieldError() {}
	
	public FieldError(String field, String message) {
		mField = field;
		mMessage = message;
	}
	
	public String getField() {
		return mField;
	}
	
	public String getMessage() {
		return mMessage;
	}
}
