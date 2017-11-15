package alexmog.apilib.api.validation;

import java.util.ArrayList;
import java.util.List;

import alexmog.apilib.exceptions.BadEntityException;

public class ValidationErrors {
	private List<FieldError> mFieldErrors = new ArrayList<>();
	private List<String> mGlobalMessages = new ArrayList<>();
	
	public void addFieldError(FieldError fieldError) {
		mFieldErrors.add(fieldError);
	}
	
	public void addFieldError(String field, String message) {
		mFieldErrors.add(new FieldError(field, message));
	}
	
	public void verifyFieldEmptyness(String field, String fieldValue, int minSize) {
		verifyFieldEmptyness(field, fieldValue, minSize, 0);
	}
	
	public void verifyFieldEmptyness(String field, String fieldValue, int minSize, int maxSize) {
		if (fieldValue == null || fieldValue.length() == 0) addFieldError(field, "IS_EMPTY");
		else if (fieldValue.length() < minSize) addFieldError(field, "BAD_SIZE:MIN(" + minSize + ")");
		else if (maxSize > 0 && fieldValue.length() > maxSize) addFieldError(field, "BAD_SIZE:MAX(" + maxSize + ")");
	}
	
	public void addGlobalMessage(String globalMessage) {
		mGlobalMessages.add(globalMessage);
	}
	
	public void checkErrors(String message) throws BadEntityException {
		if (!mGlobalMessages.isEmpty() || !mFieldErrors.isEmpty()) throw new BadEntityException(message, this);
	}
	
	public List<FieldError> getFieldErrors() {
		return mFieldErrors;
	}
	
	public List<String> getGlogbalMessages() {
		return mGlobalMessages;
	}
}
