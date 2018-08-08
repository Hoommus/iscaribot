import com.mongodb.MongoSecurityException;
import config.BotConfig;
import features.UserSessionsTracker;
import features.commands.commands.*;
import features.commands.events.CommandsHandler;
import features.commands.events.CommandsListener;
import features.listeners.NaturalLanguageListener;
import features.verification.MemberJoinListener;
import features.verification.WikiaLinkListener;
import model.MongoController;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.PrivateChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import providers.EnhancedGuildsProvider;
import providers.EnhancedMembersProvider;
import providers.EnhancedUsersProvider;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import static config.BotConfig.*;

/**
 * Created by festus on 27.06.17
 */
public class ChickpeaBot {
	private static JDA api;
    public static void main(String[] args) {
        try {
        	Logger logger = LogManager.getLogger(ChickpeaBot.class);
	
			BotConfig.load("config.properties", Arrays.asList(args));
			
			final long begin = System.currentTimeMillis();
			final JDABuilder builder = new JDABuilder(BotConfig.getAccountType())
		            .setToken(BotConfig.getJDAtoken())
					.setAudioEnabled(false);
			
			// Async init before API build speeds up startup for a couple of seconds
			//MongoController.init("root", "admin", true);
			MongoController.initAsync(getDatabaseUser(), getDatabasePass());
			api = builder.buildBlocking();
			MongoController.setJda(api);

			EnhancedGuildsProvider.init();
			EnhancedUsersProvider.init();
			EnhancedMembersProvider.init(api);
			EnhancedUsersProvider.runLazyBuffering(api);
			
			CommandsHandler handler = new CommandsHandler(30).register(
					new AvatarCommand(),
					new ConfigCommand(),
					new EchoCommand(),
					new EvalCommand(),
					new ForceHachikoCommand(),
					new HelpCommand(),
					new HelpAdminCommand(),
					new InviteCommand(),
					new JudasKissCommand(),
					new ListCommandsCommand(),
					new PingCommand(),
					new UnverifyCommand(getAuthorID()),
					new UptimeCommand(),
					new UserInfoCommand(getWikiaOrigin(), getWikiaAccount()),
					new VersionCommand(),
					new VerifyCommand(),
					new WhoisCommand(MongoController.getInstance()),
					new WipeCommand());
			
			api.addEventListener(
					new CommandsListener (getPrefix(), handler),
					new WikiaLinkListener(getPrefix(), handler),
//					new UserSessionsTracker(),
//					new MentionsListener(),
					new NaturalLanguageListener()
//					new MemberJoinListener(api)
			);
			
			long startup = System.currentTimeMillis() - begin;
	
			MessageBuilder message = new MessageBuilder("(Пере)Запущен и готов. Запуск занял ")
					.append(String.valueOf(startup))
					.append(" миллисекунд.\nПрисутствие на ")
					.append(String.valueOf(api.getGuilds().size()))
					.append(" серверах.");
	
			PrivateChannel privateChannel = api.getUserById(getAuthorID()).openPrivateChannel().complete();
			privateChannel.getIterableHistory().complete()
					.stream()
					.limit(10)
					.filter(m -> m.getAuthor().isBot())
					.filter(m -> m.getContentDisplay().equalsIgnoreCase("Ухожу в оффлайн."))
					.findFirst()
					.ifPresent(m -> {
						Duration downtime = Duration.between(m.getCreationTime().toInstant(), Instant.now());
						Long seconds = downtime.toMillis()  / 1000;
						Long minutes = seconds / 60;
						Long hours   = minutes / 60;
						
						if (seconds > 0 || minutes > 0 || hours > 0)
							message.append("Даунтайм составил: ");
						if (hours > 0)
							message.append(hours).append(" часов");
						if (minutes > 0)
							message.append(", ").append(minutes).append("минут");
						if (seconds > 0)
							message.append(", ").append(seconds).append("секунд");
						if (seconds > 0 || minutes > 0 || hours > 0)
							message.append(".");
					});
			
			privateChannel.sendMessage(message.build()).queue();
			logger.debug("Startup time taken: " + startup + "ms");
			
			Thread games = new Thread(() -> {
				final Game help = Game.watching(getPrefix() + "help");
				final Game site = Game.listening("iscaribot.xyz");
				final Game ver  = Game.playing("v" + getVersion());
				
				int i = 0;
				//noinspection InfiniteLoopStatement
				while (true) {
					try {
						if (i == 0) {
							api.getPresence().setGame(ver);
							i++;
							Thread.sleep(15_000);
						} else if (i == 1) {
							api.getPresence().setGame(site);
							i++;
							Thread.sleep(10_000);
						} else {
							api.getPresence().setGame(help);
							i = 0;
							Thread.sleep(110_000);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			games.setName("Game-changer-thread");
			Thread alive = new Thread(ChickpeaBot::keepAlive);
			alive.setName("iscaribot.xyz-pinger-thread");
			alive.setDaemon(true);
			games.setDaemon(true);
			alive.start();
			games.start();
	
			if(isTest()) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					api.getUserById(getAuthorID()).openPrivateChannel().complete().sendMessage("Ухожу в оффлайн.").complete();
				}));
			}
			
			logger.debug("Account: " + api.getSelfUser().getName());
		} catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        } catch (MongoSecurityException | NullPointerException e) {
        	e.printStackTrace();
        	System.exit(1);
		}
    }
    
    @SuppressWarnings("InfiniteLoopStatement")
	private static void keepAlive() {
    	Logger logger = LogManager.getLogger("keeping alive");
		while (true) {
			try {
				logger.info("Keeping alive.");
				URL iscaribot = new URL("http://iscaribot.xyz");
				
				HttpURLConnection connection = (HttpURLConnection) iscaribot.openConnection();
				
				connection.connect();
				connection.getContent();
				connection.disconnect();
				
				Thread.sleep(29 * 1800_000);
			} catch (InterruptedException | IOException e) {
				e.printStackTrace();
			}
		}
	}
}


