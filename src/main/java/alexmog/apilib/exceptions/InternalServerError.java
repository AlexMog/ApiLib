package alexmog.apilib.exceptions;

import org.restlet.resource.Status;

@SuppressWarnings("serial")
@Status(500)
public class InternalServerError extends BaseException {

	public InternalServerError(String message) {
		super(500, message);
	}
	
}
