package features.listeners;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class MentionsListener extends ListenerAdapter {
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if(event.getMessage().getMentionedUsers().contains(event.getJDA().getSelfUser())
				&& event.getMessage().getContentRaw().trim().length() < 26) // maximum 22 symbols for mention and 4 for commands
		{
			event.getChannel().sendMessage(event.getAuthor().getAsMention() + " :angry:").queue();
		}
	}
}
