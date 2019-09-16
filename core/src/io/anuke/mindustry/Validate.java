package io.anuke.mindustry;

/**
 * Utility methods for parameter and expression validation.  API copied from Apache Lang Validate
 * https://commons.apache.org/proper/commons-lang/javadocs/api-3.9/org/apache/commons/lang3/Validate.html
 */
public class Validate{
    /**
     * Returns value or throws NPE if it is null
     * @param value the variable to validate
     * @param name  the variable name
     * @param <T>   the type of value
     * @return      value
     */
    public static <T> T notNull(T value, String name){
        if(value == null){
            throw new NullPointerException(name);
        }
        return value;
    }
}
