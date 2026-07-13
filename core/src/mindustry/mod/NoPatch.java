package mindustry.mod;

import java.lang.annotation.*;

/** Indicates that a field cannot be edited by the content patcher. */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPatch{
}
