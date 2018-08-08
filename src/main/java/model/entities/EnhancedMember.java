package model.entities;

import model.annotations.DiffIgnore;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public class EnhancedMember implements Member {
	/**
	 * Guild where member belongs
	 */
	@DiffIgnore
	private EnhancedGuild enhancedGuild;
	/**
	 * Bare enhanced user
	 */
	@DiffIgnore
	private EnhancedUser enhancedUser;
	@DiffIgnore
	private Member member;
	
	@DiffIgnore
	private Message lastMessage = null;
	private Instant lastMessageDate = Instant.MIN;
	private Instant lastCommandDate = Instant.MIN;
	
	private Instant mutedTill = Instant.MIN;
	
	private int spamCounter = 0;
	
	private Instant verification;
	
	private EnhancedMember() {}
	
	public EnhancedMember(Member member, EnhancedUser user, EnhancedGuild guild) {
		this.member = member;
		this.enhancedUser = user;
		this.enhancedGuild = guild;
	}
	
	public EnhancedUser getEnhancedUser() {
		return enhancedUser;
	}
	
	public EnhancedMember setEnhancedUser(EnhancedUser enhancedUser) {
		this.enhancedUser = enhancedUser;
		return this;
	}
	
	public Member getMember() {
		return member;
	}
	
	public void incrementSpamCounter() {
		spamCounter++;
	}
	
	public boolean hasWikiaAccount() {
		return enhancedUser.isVerified();
	}
	
	public Instant getVerification() {
		return verification;
	}
	
	public EnhancedMember setVerification(Instant verification) {
		this.verification = verification;
		return this;
	}
	
	@Override
	public User getUser() {
		return enhancedUser;
	}
	
	@Override
	public Guild getGuild() {
		return enhancedGuild;
	}
	
	@Override
	public JDA getJDA() {
		return member.getJDA();
	}
	
	@Override
	public OffsetDateTime getJoinDate() {
		return member.getJoinDate();
	}
	
	@Override
	public GuildVoiceState getVoiceState() {
		return member.getVoiceState();
	}
	
	@Override
	public Game getGame() {
		return member.getGame();
	}
	
	@Override
	public OnlineStatus getOnlineStatus() {
		return member.getOnlineStatus();
	}
	
	@Override
	public String getNickname() {
		return member.getNickname();
	}
	
	@Override
	public String getEffectiveName() {
		return member.getEffectiveName();
	}
	
	@Override
	public List<Role> getRoles() {
		return member.getRoles();
	}
	
	@Override
	public Color getColor() {
		return member.getColor();
	}
	
	@Override
	public List<Permission> getPermissions() {
		return member.getPermissions();
	}
	
	@Override
	public List<Permission> getPermissions(Channel channel) {
		return member.getPermissions(channel);
	}
	
	@Override
	public boolean hasPermission(Permission... permissions) {
		return member.hasPermission(permissions);
	}
	
	@Override
	public boolean hasPermission(Collection<Permission> permissions) {
		return member.hasPermission(permissions);
	}
	
	@Override
	public boolean hasPermission(Channel channel, Permission... permissions) {
		return member.hasPermission(channel, permissions);
	}
	
	@Override
	public boolean hasPermission(Channel channel, Collection<Permission> permissions) {
		return member.hasPermission(channel, permissions);
	}
	
	@Override
	public boolean canInteract(Member member) {
		return member.canInteract(member);
	}
	
	@Override
	public boolean canInteract(Role role) {
		return member.canInteract(role);
	}
	
	@Override
	public boolean canInteract(Emote emote) {
		return member.canInteract(emote);
	}
	
	@Override
	public boolean isOwner() {
		return member.isOwner();
	}
	
	@Override
	public String getAsMention() {
		return member.getAsMention();
	}
	
	@Nullable
	@Override
	public TextChannel getDefaultChannel() {
		return member.getDefaultChannel();
	}
	
	public Instant getLastMessageDate() {
		return lastMessageDate;
	}
	
	public EnhancedMember setLastMessageDate(Instant lastMessageDate) {
		this.lastMessageDate = lastMessageDate;
		return this;
	}
	
	public Instant getLastCommandDate() {
		return lastCommandDate;
	}
	
	public EnhancedMember setLastCommandDate(Instant lastCommandDate) {
		this.lastCommandDate = lastCommandDate;
		return this;
	}
	
	public int getSpamCounter() {
		return spamCounter;
	}
	
	public EnhancedMember setSpamCounter(int spamCounter) {
		this.spamCounter = spamCounter;
		return this;
	}
}
