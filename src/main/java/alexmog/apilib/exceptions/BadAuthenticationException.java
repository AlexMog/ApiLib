package alexmog.apilib.exceptions;

import org.restlet.resource.Status;

@SuppressWarnings("serial")
@Status(400)
public class BadAuthenticationException extends BaseException {

	public BadAuthenticationException(String message) {
		super(400, message);
	}

}
