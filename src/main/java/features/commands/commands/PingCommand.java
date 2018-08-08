package features.commands.commands;

import features.commands.events.CommandEvent;
import model.dbfields.BotMessages;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class PingCommand extends AbstractCommand {
	public PingCommand() {
		this.name = "ping";
		this.aliases = new String[]{"pong"};
		this.description = "Prints application ping";
	}
	
	public PingCommand(String name, String description, String[] aliases, boolean usesTransliteration, int argsCount) {
		super(name, description, aliases, usesTransliteration, argsCount);
	}
	
	@Override
	public MessageEmbed getHelpEmbed(CommandEvent event) {
		return new EmbedBuilder().setTitle("Help for command **" + name + "**").setDescription(description).build();
	}
	
	@Override
	public void execute(CommandEvent event) {
		String sound = Math.random() > 0.3333 ? "pong!" : "HONK :clown: ";
		Map<String, String> keys = new HashMap<>();
		keys.put("sound", sound);
		keys.put("usermention", event.getAuthor().getAsMention());
		keys.put("guildname", event.getEnhancedGuild().getName());
		keys.put("ping", String.valueOf(event.getJDA().getPing()));
		
		event.getChannel().sendMessage(event.getEnhancedGuild().getMessage(BotMessages.PING_TEXT, keys)).submit();
		
		new Thread(() -> {
			try {
				URL iscaribot = new URL("http://iscaribot.xyz");
				HttpURLConnection connection = (HttpURLConnection) iscaribot.openConnection();
				connection.connect();
				connection.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
}
