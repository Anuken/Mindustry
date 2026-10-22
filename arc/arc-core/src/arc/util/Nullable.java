package arc.util;

import java.lang.annotation.*;

/** Indicates that a method return or field can be null. */
@Documented
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable{

}
