package model.annotations;

import model.dbfields.UserFields;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserField {
	UserFields inDB();
}
