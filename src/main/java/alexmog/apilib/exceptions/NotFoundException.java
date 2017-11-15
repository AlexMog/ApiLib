package alexmog.apilib.exceptions;

import org.restlet.resource.Status;

@SuppressWarnings("serial")
@Status(404)
public class NotFoundException extends BaseException {
	public NotFoundException(String message) {
		super(404, message);
	}

}
