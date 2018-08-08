package www;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

import static spark.Spark.*;

public class SparkBootloader {
	private static final Logger LOGGER = LogManager.getLogger(SparkBootloader.class);
	
	public static void main(String[] args) {
		ignite();
	}
	
	public static void ignite() {
		try {
			staticFileLocation("/www");
			port(getHerokuAssignedPort());
			Spark.init();
			redirect.get("/robots.txt", "/robots.txt");
			redirect.get("*", "/html/stub.html");
			after((request, response) -> LOGGER.info("Requested by " + request.ip()));
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
	
	private static int getHerokuAssignedPort() {
		ProcessBuilder processBuilder = new ProcessBuilder();
		if (processBuilder.environment().get("PORT") != null) {
			return Integer.parseInt(processBuilder.environment().get("PORT"));
		}
		return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
	}
}
