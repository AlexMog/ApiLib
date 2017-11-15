package alexmog.apilib.api;

import java.sql.SQLException;
import java.util.logging.Level;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.service.StatusService;

import alexmog.apilib.ApiServer;
import alexmog.apilib.exceptions.InternalServerError;

public class ApiStatusService extends StatusService {
	@Override
	public Status toStatus(Throwable throwable, Request request, Response response) {
		if (throwable instanceof SQLException) {
			ApiServer.LOGGER.log(Level.SEVERE, "Database error spotted", throwable);
			throwable = new InternalServerError("Database error");
		}
		return super.toStatus(throwable, request, response);
	}
}
