package alexmog.apilib.exceptions;

import org.restlet.resource.Status;

@SuppressWarnings("serial")
@Status(403)
public class PermissionsException extends BaseException {

	public PermissionsException(String message) {
		super(403, message);
	}

}
