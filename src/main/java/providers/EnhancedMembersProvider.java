package providers;

import model.DatabaseController;
import model.entities.EnhancedGuild;
import model.entities.EnhancedMember;
import model.MongoController;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Member;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class EnhancedMembersProvider {
	private static final HashMap<Long, EnhancedMember> ENHANCED_MEMBER_BUFFER = new HashMap<>();
	private static final Logger LOGGER = LogManager.getLogger(EnhancedMembersProvider.class);
	private static final EnhancedMembersProvider instance;
	private static DatabaseController databaseController;
	private static JDA api;
	static {
		instance = new EnhancedMembersProvider();
	}
	
	public static EnhancedMembersProvider getInstance() {
		return instance;
	}
	
	private EnhancedMembersProvider() {}
	
	public static void init(JDA jda) {
		api = jda;
		databaseController = MongoController.getInstance();
		LOGGER.info("EnhancedMembersProvider initialized.");
		LOGGER.info("Starting lazy buffering.");
		CompletableFuture.runAsync(() -> buffer(OnlineStatus.OFFLINE))
					 .thenRunAsync(() -> buffer(OnlineStatus.UNKNOWN));
	}
	
	public static EnhancedMember enhance(Member member) {
		Long userid = member.getUser().getIdLong();
		EnhancedUser enhancedUser = EnhancedUsersProvider.getInstance().enhance(member.getUser());
		EnhancedGuild enhancedGuild = EnhancedGuildsProvider.getInstance().enhance(member.getGuild());
		
		if(ENHANCED_MEMBER_BUFFER.containsKey(userid)) {
			return ENHANCED_MEMBER_BUFFER.get(userid);
		} else if (databaseController.doesMemberEntryExist(userid)) {
			EnhancedMember enhancedMember = databaseController.getEntryAsEnhancedMember(userid);
			ENHANCED_MEMBER_BUFFER.put(userid, enhancedMember);
			return enhancedMember;
		} else {
			EnhancedMember enhancedMember = new EnhancedMember(member, enhancedUser, enhancedGuild);
			databaseController.writeNewMemberEntry(enhancedMember);
			return enhancedMember;
		}
	}
	
	private static void buffer(OnlineStatus ifNot) {
		api.getGuilds().forEach(guild ->
			guild.getMembers().parallelStream()
					.filter(member -> member.getOnlineStatus() != ifNot)
					.forEach(EnhancedMembersProvider::enhance));
	}
}
