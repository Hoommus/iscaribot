package features.commands.annotations;

import net.dv8tion.jda.core.Permission;

import javax.annotation.CheckReturnValue;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubCommand {
	
	/**
	 * @return String array of subcommand signature
	 *
	 * e.g. {@code ["status", "brief"]} used for  {@code "{@literal <command>} status brief"} input string
	 */
	String[] subCommand();
	
	/**
	 * @return String array of aliases a subcommand can use
	 */
	@CheckReturnValue
	String[] aliases() default {};
	
	/**
	 * @return permissions array that member must have to call this subcommand
	 */
	@CheckReturnValue
	Permission[] permissions() default {Permission.MESSAGE_WRITE};
	
	String description() default "no help";
	
	SubCommandType type() default SubCommandType.NONE;
	
	boolean isEnabled() default true;
	
	int args() default 0;
}
