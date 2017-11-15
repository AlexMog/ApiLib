package alexmog.apilib.exceptions;

import java.util.List;

import org.restlet.resource.Status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import alexmog.apilib.api.validation.FieldError;
import alexmog.apilib.api.validation.ValidationErrors;

@SuppressWarnings("serial")
@Status(422)
public class BadEntityException extends BaseException {
	private List<String> mGlobalMessages;
	private List<FieldError> mFieldErrorList;
	
	public BadEntityException(String message) {
		super(422, message);
	}

	public BadEntityException(String message, ValidationErrors validationErrors) {
		this(message);
		mGlobalMessages = validationErrors.getGlogbalMessages();
		mFieldErrorList = validationErrors.getFieldErrors();
	}
	
	@JsonInclude(Include.NON_EMPTY)
	public List<String> getGlobalMessages() {
		return mGlobalMessages;
	}
	
	@JsonInclude(Include.NON_EMPTY)
	public List<FieldError> getFieldErrors() {
		return mFieldErrorList;
	}
}
