package features.commands.events;

import model.entities.EnhancedGuild;
import model.entities.EnhancedUser;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import providers.EnhancedGuildsProvider;
import providers.EnhancedUsersProvider;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;

@SuppressWarnings("unused")
public class CommandEvent {
	
	private final JDA api;
	private final long responseNumber;
	
	private final EnhancedGuild enhancedGuild;
	private final EnhancedUser author;
	private final Member member;

	private final Message message;
	private final String command;
	private final LinkedList<String> args;
	
	private final Map<String, String> messageKeys;
	
	private final EnhancedUsersProvider enhancedUsersProvider;
	private final EnhancedGuildsProvider enhancedGuildsProvider;
	
	private CommandsHandler handler;
	
	public CommandEvent(@Nonnull JDA api, long responseNumber, @Nonnull EnhancedGuild enhancedGuild,
						@Nonnull EnhancedUser author, @Nonnull Member member, @Nonnull Message message,
						@Nonnull String command, @Nonnull Map<String, String> messageKeys,
						@Nonnull EnhancedUsersProvider enhancedUsersProvider,
						@Nonnull EnhancedGuildsProvider enhancedGuildsProvider,
						@Nonnull LinkedList<String> args) {
		this.api = api;
		this.responseNumber = responseNumber;
		this.enhancedGuild = enhancedGuild;
		this.author = author;
		this.member = member;
		this.message = message;
		this.command = command;
		this.enhancedUsersProvider = enhancedUsersProvider;
		this.enhancedGuildsProvider = enhancedGuildsProvider;
		this.messageKeys = messageKeys;
		this.args = args;
	}
	
	public CommandsHandler getHandler() {
		return handler;
	}
	
	public CommandEvent reduceArgsBy(int n) throws IllegalArgumentException {
		if(this.args.size() < n)
			throw new IllegalArgumentException("Number to remove cannot be greater than args size.");
		for(int i = 0; i < n; i++)
			this.args.removeFirst();
		return this;
	}
	
	public CommandEvent reduceArgs() {
		this.args.removeFirst();
		return this;
	}
	
	CommandEvent setHandler(CommandsHandler handler) {
		this.handler = handler;
		return this;
	}
	
	public Instant getEventTime() {
		return message.getCreationTime().toInstant();
	}
	
	public void sendMessage(Message message) {
		this.getChannel().sendMessage(message).queue();
	}
	
	public void sendMessage(String message) {
		this.sendMessage(new MessageBuilder(message).build());
	}
	
	public long getResponseNumber() {
		return responseNumber;
	}
	
	public EnhancedUser getAuthor() {
		return author;
	}
	
	public EnhancedUser getEnhancedUser() {
		return author;
	}
	
	public Message getMessage() {
		return message;
	}
	
	public EnhancedGuild getEnhancedGuild() {
		return this.enhancedGuild;
	}
	
	public String getCommandString() {
		return this.command;
	}
	
	public LinkedList<String> getArgs() {
		return this.args;
	}
	
	public MessageChannel getChannel() {
		return message.getChannel();
	}
	
	public JDA getJDA() {
		return api;
	}
	
	public Member getMember() {
		return member;
	}
	
	public Map<String, String> getMessageKeys() {
		return messageKeys;
	}
	
	public EnhancedGuildsProvider getEnhancedGuildsProvider() {
		return enhancedGuildsProvider;
	}
	
	public EnhancedUsersProvider getEnhancedUsersProvider() {
		return enhancedUsersProvider;
	}
	
	public static class Builder {
	
	}
}
