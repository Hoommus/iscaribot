package providers;

import model.DatabaseController;
import model.entities.EnhancedGuild;
import model.MongoController;
import model.dbfields.BotFeatures;
import net.dv8tion.jda.core.entities.Guild;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class holds an enhanced guilds buffer that has been read from Mongo Database.
 * TODO: implement here Pub/Sub.
 * TODO: remove Singleton and make this using dependency injection.
 */
@SuppressWarnings("unused")
public class EnhancedGuildsProvider {
	private static final HashMap<Long, EnhancedGuild> ENHANCED_GUILDS_BUFFER = new HashMap<>();
	private static final EnhancedGuildsProvider instance;
	private static DatabaseController mongoController = null;
	
	static {
		instance = new EnhancedGuildsProvider();
	}
	
	public static EnhancedGuildsProvider getInstance() {
		return instance;
	}
	
	private EnhancedGuildsProvider() { }
	
	public static void init() {
		if(mongoController == null) mongoController = MongoController.getInstance();
		synchronized (ENHANCED_GUILDS_BUFFER) {
			mongoController.getAllEnhancedGuilds().forEach((id, guild) -> {
				if( ! ENHANCED_GUILDS_BUFFER.containsKey(id))
					ENHANCED_GUILDS_BUFFER.put(id, guild);
			});
		}
	}
	
	public static void initWith(DatabaseController databaseController) {
		if(mongoController == null) mongoController = databaseController;
		init();
	}
	
	public static void initAsync(final MongoController mongoController) {
		CompletableFuture.runAsync(() -> {
			synchronized (ENHANCED_GUILDS_BUFFER) {
				ENHANCED_GUILDS_BUFFER.putAll(mongoController.getAllEnhancedGuilds());
			}
		});
	}
	
	/**
	 * @param guild guild that needs enhancement
	 * @return EnhancedGuild from buffer or new initialized enhanced
	 */
	public EnhancedGuild enhance(@Nonnull Guild guild) {
		if (ENHANCED_GUILDS_BUFFER.containsKey(guild.getIdLong())) {
			return ENHANCED_GUILDS_BUFFER.get(guild.getIdLong());
		} else if (mongoController.doesEnhancedGuildExist(guild.getIdLong())) {
			EnhancedGuild enhancedGuild = mongoController.getEnhancedGuild(guild);
			ENHANCED_GUILDS_BUFFER.put(guild.getIdLong(), enhancedGuild);
			update(enhancedGuild);
			return enhancedGuild;
		} else {
			EnhancedGuild enhancedGuild = new EnhancedGuild(guild);
			ENHANCED_GUILDS_BUFFER.put(guild.getIdLong(), enhancedGuild);
			mongoController.writeNewEnhancedGuild(enhancedGuild);
			return enhancedGuild;
		}
	}
	
	public ArrayList<EnhancedGuild> enhanceEach(@Nonnull List<Guild> guilds) {
		ArrayList<EnhancedGuild> enhancedGuilds = new ArrayList<>();
		for (Guild g : guilds) {
			if(ENHANCED_GUILDS_BUFFER.get(g.getIdLong()) == null) {
				EnhancedGuild enhancedGuild = new EnhancedGuild(g).setBotFeaturesEnabled(BotFeatures.Pinger);
				mongoController.writeNewEnhancedGuild(enhancedGuild);
				enhancedGuilds.add(enhancedGuild);
			} else {
				enhancedGuilds.add(ENHANCED_GUILDS_BUFFER.get(g.getIdLong()));
			}
		}
		return enhancedGuilds;
	}
	
	public void update(EnhancedGuild enhancedGuild) {
		ENHANCED_GUILDS_BUFFER.replace(enhancedGuild.getGuildId(), enhancedGuild);
		mongoController.updateEnhancedGuild(enhancedGuild);
	}
	
	public ArrayList<EnhancedGuild> getAllEnhancedGuilds() {
		return new ArrayList<>(ENHANCED_GUILDS_BUFFER.values());
	}
	
	public EnhancedGuild getEnhancedGuild(@Nonnull Guild guild) {
		return ENHANCED_GUILDS_BUFFER.get(guild.getIdLong());
	}
	
	public EnhancedGuild getEnhancedGuildById(@Nonnull Long id) {
		return ENHANCED_GUILDS_BUFFER.get(id);
	}
}
