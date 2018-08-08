package model.annotations;

import model.dbfields.GuildFields;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuildField {
	GuildFields inDB();
	FieldType type() default FieldType.NAME;
}