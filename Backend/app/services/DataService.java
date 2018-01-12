package services;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import akka.actor.ActorSystem;
import play.data.DynamicForm;
import play.db.Database;
import play.libs.concurrent.CustomExecutionContext;

@Singleton
public class DataService {

	private Database db;
	private DatabaseExecutionContext executionContext;

	@Inject
	public DataService(Database db, DatabaseExecutionContext context) {
		this.db = db;
		this.executionContext = context;
	}

	public CompletionStage<Boolean> insert(DynamicForm form) {
		
		return CompletableFuture.supplyAsync(() -> {
			return db.withConnection(connection -> {
				String sql = "Insert into location (username,time,latitude,longitude) values (?,?,?,?)";

				PreparedStatement s = db.getConnection().prepareStatement(sql);

				s.setString(1, form.get("username"));
				s.setTimestamp(2, new Timestamp(Long.parseLong(form.get("timestamp"))));
				s.setString(3, form.get("latitude"));
				s.setString(4, form.get("longitude"));

				try {

					return s.execute();
				} catch (Exception e) {
					throw new SQLException(e);
				}


			});
		}, executionContext);
	}

	public Double calculateDistance(DynamicForm form) throws SQLException {
		String query = "SELECT latitude,longitude from location where username=? and time >= ? order by time asc";

		PreparedStatement stm = db.getConnection().prepareStatement(query);

		stm.setString(1, form.get("username"));
		stm.setTimestamp(2, new Timestamp(Long.parseLong(form.get("timestamp"))));

		ResultSet result = stm.executeQuery();

		Double currentLat = result.getDouble(1);

		Double currentLon = result.getDouble(2);

		Double cumulativeDist = 0.0;

		while (result.next()) {
			cumulativeDist += p2pDistance(result.getDouble(1), result.getDouble(2), currentLat, currentLon);
			currentLat = result.getDouble(1);
			currentLon = result.getDouble(2);
		}

		return cumulativeDist;
	}

	// Below code is courtesy of GeoDataSource.com
	private static double p2pDistance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;

		return (dist);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}

	public static class DatabaseExecutionContext extends CustomExecutionContext {

		@javax.inject.Inject
		public DatabaseExecutionContext(ActorSystem actorSystem) {
			// uses a custom thread pool defined in application.conf
			super(actorSystem, "database.dispatcher");
		}
	}
}
