package wikia;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

// TODO: Переписать запрос и верификацию для использования на отдельной нити или асинхронно.
// TODO: Убрать использование Gson.
public class WikiaAPICall {
	private static final String queryGeneric = "http://community.wikia.com/api.php?action=query&format=json&list=users&usprop=groups|editcount|registration&ususers=";
	private static final String queryBody = ".wikia.com/api.php?action=query&format=json&list=users&usprop=groups|editcount|registration&ususers=";
	private static final String queryOneGeneric = "http://community.wikia.com/api/v1/User/Details/?ids=";
	private static final String queryOneBody = ".wikia.com/api/v1/User/Details/?ids=";
	private static final Logger LOGGER = LogManager.getLogger(WikiaAPICall.class);
	
	private WikiaAPICall() {
	}
	
	public static WikiaUser getWikiaUserByName(String username)
			throws IOException, JsonIOException, JsonSyntaxException
	{
		return getWikiaUserByName("community", username);
	}
	
	@CheckForNull
	public static WikiaUser getWikiaUserByName(@Nonnull String wiki, @Nonnull String username)
			throws IOException, JsonIOException, JsonSyntaxException
	{
		URL url = new URL("http://" + wiki + queryBody + username.replaceAll(" ", "_"));
		long firstRequestStart = System.currentTimeMillis();
		
		HttpURLConnection userInfoRequest = (HttpURLConnection) url.openConnection();
		LOGGER.debug("Opened connection for '" + username + "'.");
		
		JsonElement root = new JsonParser().parse(new InputStreamReader((InputStream) userInfoRequest.getContent()));
		JsonObject rootObject = root.getAsJsonObject().getAsJsonObject("query");
		
		userInfoRequest.disconnect();
		
		long firstRequest = System.currentTimeMillis() - firstRequestStart;
		LOGGER.debug("Time taken: " + firstRequest + "millis.");
		
		// Get first user in array
		// There should always be one user in array
		JsonObject userInfo;
		try {
			userInfo = rootObject.getAsJsonArray("users").get(0).getAsJsonObject();
		} catch (IndexOutOfBoundsException | NullPointerException e) {
			/* checks first time if user exists */
			return null;
		}
		if(userInfo.get("missing") != null) {
			/* if missing key exists, then throw exception */
			LOGGER.info("Wikia user '" + username + "' missing.");
			return null;
		}
		
		WikiaUser wikiaUser;
		try {
			wikiaUser = new WikiaUser().setUserid(userInfo.getAsJsonPrimitive("userid").getAsLong())
					.setName(userInfo.getAsJsonPrimitive("name").getAsString())
					.setEditcount(userInfo.getAsJsonPrimitive("editcount").getAsInt())
					.setRegistration(Instant.parse(userInfo.getAsJsonPrimitive("registration").getAsString()))
					.setGroups(getStringArrayFromJsonArray(userInfo.getAsJsonArray("groups")));
		} catch (NullPointerException e) {
			/* Just one more check. If any of 'getAsJsonPrimitive' throws NullPointerException, then user doesn't exist. */
			LOGGER.warn("Mistake in received JSON. Wikia user '" + username + "' missing.");
			return null;
		}
		
		if(wikiaUser != null) {
			URL avatarAPI = new URL("http://" + wiki + queryOneBody + wikiaUser.getUserid());
			HttpURLConnection userAvatarRequest = (HttpURLConnection) avatarAPI.openConnection();
			JsonElement avatar = new JsonParser().parse(new InputStreamReader((InputStream) userAvatarRequest.getContent()));
			
			userAvatarRequest.disconnect();
			
			JsonObject avatarRoot = avatar.getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject();
			wikiaUser.setAvatarUrl(avatarRoot.get("avatar").getAsString().replaceAll("/scale-to-width-down/100", ""));
		}
		
		
		LOGGER.info("User successfully received and processed.");
		return wikiaUser;
	}
	
	public static WikiaUser getWikiaUserById(String wiki, String id) throws IOException {
		LOGGER.traceEntry();
		URL url = new URL("http://" + wiki + queryOneBody + id);
		HttpURLConnection request = (HttpURLConnection) url.openConnection();
		
		JsonParser jsonParser = new JsonParser();
		JsonElement root = jsonParser.parse(new InputStreamReader((InputStream) request.getContent()));
		
		LOGGER.traceExit();
		return null;
	}
	
	private static String[] getStringArrayFromJsonArray(JsonArray jsonArray) {
		String[] array = new String[jsonArray.size()];
		
		for (int i = 0; i < jsonArray.size(); i++) {
			array[i] = jsonArray.get(i).getAsString();
		}
		
		return array;
	}
}
