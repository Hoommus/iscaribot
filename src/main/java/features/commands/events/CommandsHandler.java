package features.commands.events;

import features.commands.annotations.SubCommand;
import features.commands.commands.AbstractCommand;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class CommandsHandler {
	private final List<AbstractCommand> commands = new ArrayList<>();
	private final long cooldown;
	
	public CommandsHandler(long baseCooldown) {
		this.cooldown = baseCooldown;
	}
	
	public CommandsHandler register(AbstractCommand... abstractCommand) {
		commands.addAll(Arrays.stream(abstractCommand).filter(c -> c.getName() != null).collect(Collectors.toList()));
		return this;
	}
	
	public void unregister(AbstractCommand abstractCommand) {
		commands.remove(abstractCommand);
	}
	
	public void handle(CommandEvent event) {
		for (AbstractCommand abstractCommand : commands) {
			if (abstractCommand.isCalledBy(event.getCommandString())) {
				if (event.getMember().hasPermission(abstractCommand.getMemberPermissions())) {
					try {
						mainLoop:
						for (Method method : abstractCommand.getClass().getMethods()) {
							final SubCommand annotation = method.getDeclaredAnnotation(SubCommand.class);
							if (annotation != null && annotation.isEnabled()
									&& event.getArgs().size() >= annotation.subCommand().length
									&& !Collections.disjoint(event.getMember().getPermissions(), Arrays.asList(annotation.permissions()))) {
								final String[] commandArray = annotation.subCommand();
								int i = 0;
								// At the moment, commands with wildcards disabled.
								int wildcard = -42;
								for (; i < commandArray.length; i++) {
									final String subCommand = commandArray[i];
									final String arg = event.getArgs().toArray(new String[0])[i];
									
									if(subCommand.equals("${*}") && wildcard == -42) {
										wildcard = i;
										continue;
									}
									
									if(!subCommand.equalsIgnoreCase(arg))
										continue mainLoop;
								}
								
								// Don't miss this one. Actually, I should rewrite it. 19.12.17
								if(wildcard != -42)
									i = wildcard;
								method.invoke(abstractCommand, event.reduceArgsBy(i).setHandler(this));
								return;
							}
						}
						abstractCommand.execute(event.setHandler(this));
					} catch (PermissionException e) {
						CommandsListener.LOGGER.warn("Not enough permissions in " +
								event.getEnhancedGuild().getName() + " permission=" + e.getPermission());
					} catch (Throwable throwable) {
						CommandsListener.LOGGER.error("one abstractCommand had an uncaught exception", throwable);
					}
				}
			}
		}
	}
	
	public List<AbstractCommand> getCommands() {
		return new ArrayList<>(commands);
	}
}
