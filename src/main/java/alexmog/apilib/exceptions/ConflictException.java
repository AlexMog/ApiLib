package alexmog.apilib.exceptions;

import org.restlet.resource.Status;

@SuppressWarnings("serial")
@Status(409)
public class ConflictException extends BaseException {
	public ConflictException(String message) {
		super(409, message);
	}

}
