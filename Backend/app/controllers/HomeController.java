package controllers;

import java.sql.SQLException;

import com.google.inject.Inject;

import play.data.DynamicForm;
import play.data.FormFactory;
import play.db.Database;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.DataService;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller {

	@Inject
	Database db;

	@Inject
	DataService service;

	@Inject
	FormFactory formFactory;

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        return ok(views.html.index.render());
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result handleUpdates() throws SQLException {
		DynamicForm form = formFactory.form().bindFromRequest();

		service.insert(form);

		return ok("{\"ok\":\"Data received\"}");
    }

	@BodyParser.Of(BodyParser.Json.class)
	public Result getDistance() throws SQLException {

		DynamicForm form = formFactory.form().bindFromRequest();

		Double distance = service.calculateDistance(form);

		return ok("{\"result\":\"" + distance + "\"}");

	}

}
