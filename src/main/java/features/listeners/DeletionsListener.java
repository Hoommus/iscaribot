package features.listeners;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;

public class DeletionsListener extends ListenerAdapter {
	@Override
	public void onGuildMessageDelete(GuildMessageDeleteEvent event) {

//		JDA jda = ChickpeaBot.api;
//
		
		Message message = event.getChannel().getMessageById(event.getMessageId()).complete();
		Guild guild = event.getGuild();
		Member member = guild.getMember(message.getAuthor());
		Channel channel = event.getChannel();
		EmbedBuilder embedBuilder = new EmbedBuilder().setColor(Color.RED).setTitle("Сообщение удалено в канале " + channel.getName());
		
		if(!member.hasPermission(Permission.BAN_MEMBERS) || !member.hasPermission(Permission.ADMINISTRATOR)) {
		
		}
		
		//
//		event.getMessageId();
	}
}
