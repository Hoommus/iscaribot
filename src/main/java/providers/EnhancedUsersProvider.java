package providers;

import model.DatabaseController;
import model.entities.EnhancedUser;
import model.MongoController;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wikia.WikiaUser;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

// TODO: добавить проверку нужна ли верификация юзеру исходя из настроек гильд, где он находится.
// В зависимости от этого решать доступаться ли к определенным полям.

public class EnhancedUsersProvider {
	/**
	 * Buffer fills from {@link EnhancedMembersProvider} {@literal .init()}
	 */
	private static final HashMap<Long, EnhancedUser>  ENHANCED_USERS_BUFFER = new HashMap<>();
	private static final Logger LOGGER = LogManager.getLogger(EnhancedUsersProvider.class);
	private static final EnhancedUsersProvider instance;
	private static DatabaseController databaseController = null;
	private static JDA api;
	static {
		instance = new EnhancedUsersProvider();
	}
	
	public static EnhancedUsersProvider getInstance() {
		return instance;
	}
	
	private EnhancedUsersProvider() {}
	
	public static void init() {
		databaseController = MongoController.getInstance();
		LOGGER.info("EnhancedUsersProvider initialized.");
	}
	
	public static void initWith(DatabaseController controller) {
		databaseController = controller;
	}
	
	public static void runLazyBuffering(JDA api) {
		LOGGER.info("Now starting lazyBuffering");
		CompletableFuture.runAsync(() ->
			api.getGuilds().forEach(guild -> {
				Stream<Member> stream = guild.getMembers().stream()
						.filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE);
				CompletableFuture.runAsync(() -> stream.forEach(
						member -> instance.enhance(member.getUser())));
			})
		);
		
	}
	
	public boolean checkIfWikiaUserBinded(WikiaUser wikiaUser) {
		return wikiaUser != null && databaseController.doesWikiaUserExist(wikiaUser.getUserid());
	}
	
	public EnhancedUser enhance(User user) {
		return enhance(user, true);
	}
	
	public EnhancedUser enhance(User user, boolean createIfAbsent) {
		long start = System.currentTimeMillis();
		Long userid = user.getIdLong();
		if(ENHANCED_USERS_BUFFER.containsKey(userid)) {
			//LOGGER.debug("'" + user.getName() + "' has already been buffered. Returning it.");
			//LOGGER.debug("User enhancement time taken: " + (System.currentTimeMillis() - start) + "ms");
			return ENHANCED_USERS_BUFFER.get(userid);
		} else if(databaseController.doesEnhancedUserExist(userid)) {
			EnhancedUser enhancedUser = databaseController.getEnhancedUserByDiscordId(userid);
			ENHANCED_USERS_BUFFER.put(userid, enhancedUser);
			LOGGER.debug("'" + user.getName() + "' taken from database and returned.");
			LOGGER.debug("User enhancement time taken: " + (System.currentTimeMillis() - start) + "ms");
			return enhancedUser;
		} else if (createIfAbsent) {
			LOGGER.debug("'" + user.getName() + "' created in database and returned.");
			LOGGER.debug("User enhancement time taken: " + (System.currentTimeMillis() - start) + "ms");
			return addNewUser(user);
		} else
			return null;
	}
	
	public void update(EnhancedUser updated) {
		EnhancedUser old = ENHANCED_USERS_BUFFER.get(updated.getIdLong());
		if(!Objects.equals(old, updated))
			return;
		
		CompletableFuture.runAsync(() -> databaseController.updateEnhancedUser(updated));
		ENHANCED_USERS_BUFFER.replace(old.getIdLong(), updated);
		LOGGER.debug("User " + updated.getName() + " updated.");
	}
	
	public EnhancedUser addNewUser(User user) {
		EnhancedUser enhancedUser = new EnhancedUser(user)
				.setWikiaUser(null)
				.setGuildsEverMetFromGuilds(user.getMutualGuilds())
				.setLastGoneOffline(null)
				.setLastGoneOnline(null);
		databaseController.writeNewEnhancedUser(enhancedUser);
		return enhancedUser;
	}
	
	public void addNewUser(EnhancedUser enhancedUser) {
		databaseController.writeNewEnhancedUser(enhancedUser);
		ENHANCED_USERS_BUFFER.put(enhancedUser.getIdLong(), enhancedUser);
	}
	
	public long countAllVerifiedUsers() {
		return databaseController.countVerifiedUsers();
	}
}
