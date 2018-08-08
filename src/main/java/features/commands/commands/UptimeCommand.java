package features.commands.commands;

import features.commands.events.CommandEvent;
import model.dbfields.BotMessages;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends AbstractCommand {
	public UptimeCommand() {
		this.name = "uptime";
		this.aliases = new String[]{"upt", "up"};
		this.description = "Prints application uptime.";
	}
	
	@Override
	public void execute(CommandEvent event) {
		RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
		long uptime = mxBean.getUptime();
		
		Duration working = Duration.between(Instant.parse("2017-06-28T14:35:00.00Z"), Instant.now());
		
		Map<String, String> messageKeys = new HashMap<>();
		messageKeys.put("hours", TimeUnit.MILLISECONDS.toHours(uptime) + "");
		messageKeys.put("minutes", TimeUnit.MILLISECONDS.toMinutes(uptime) % 60 + "");
		messageKeys.put("seconds", TimeUnit.MILLISECONDS.toSeconds(uptime) % 60 + "");
		messageKeys.put("working", working.toDays() + "");
		
		event.getChannel().sendMessage(event.getEnhancedGuild().getMessage(BotMessages.UPTIME, messageKeys)).queue();
	}
}
