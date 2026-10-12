package arc.util;

import java.text.MessageFormat;
import java.util.Locale;

/**
 * {@code TextFormatter} is used by {@link I18NBundle} to perform argument replacement.
 * @author davebaol
 */
public class TextFormatter{

    private MessageFormat messageFormat;
    private StringBuilder buffer;

    public TextFormatter(Locale locale, boolean useMessageFormat){
        buffer = new StringBuilder();
        if(useMessageFormat) messageFormat = new MessageFormat("", locale);
    }

    /**
     * Formats the given {@code pattern} replacing its placeholders with the actual arguments specified by {@code args}.
     * <p>
     * If this {@code TextFormatter} has been instantiated with {@link #TextFormatter(Locale, boolean) TextFormatter(locale, true)}
     * {@link MessageFormat} is used to process the pattern, meaning that the actual arguments are properly localized with the
     * locale of this {@code TextFormatter}.
     * <p>
     * On the contrary, if this {@code TextFormatter} has been instantiated with {@link #TextFormatter(Locale, boolean)
     * TextFormatter(locale, false)} pattern's placeholders are expected to be in the simplified form {0}, {1}, {2} and so on and
     * they will be replaced with the corresponding object from {@code args} converted to a string with {@code toString()}, so
     * without taking into account the locale.
     * <p>
     * In both cases, there's only one simple escaping rule, i.e. a left curly bracket must be doubled if you want it to be part of
     * your string.
     * <p>
     * It's worth noting that the rules for using single quotes within {@link MessageFormat} patterns have shown to be somewhat
     * confusing. In particular, it isn't always obvious to localizers whether single quotes need to be doubled or not. For this
     * very reason we decided to offer the simpler escaping rule above without limiting the expressive power of message format
     * patterns. So, if you're used to MessageFormat's syntax, remember that with {@code TextFormatter} single quotes never need to
     * be escaped!
     * @param pattern the pattern
     * @param args the arguments
     * @return the formatted pattern
     * @throws IllegalArgumentException if the pattern is invalid
     */
    public String format(String pattern, Object... args){
        if(messageFormat != null){
            messageFormat.applyPattern(replaceEscapeChars(pattern));
            return messageFormat.format(args);
        }
        return simpleFormat(pattern, args);
    }

    // This code is needed because a simple replacement like
    // pattern.replace("'", "''").replace("{{", "'{'");
    // can't properly manage some special cases.
    // For example, the expected output for {{{{ is {{ but you get {'{ instead.
    // Also this code is optimized since a new string is returned only if something has been replaced.
    private String replaceEscapeChars(String pattern){
        buffer.setLength(0);
        boolean changed = false;
        int len = pattern.length();
        for(int i = 0; i < len; i++){
            char ch = pattern.charAt(i);
            if(ch == '\''){
                changed = true;
                buffer.append("''");
            }else if(ch == '{'){
                int j = i + 1;
                while(j < len && pattern.charAt(j) == '{')
                    j++;
                int escaped = (j - i) / 2;
                if(escaped > 0){
                    changed = true;
                    buffer.append('\'');
                    do{
                        buffer.append('{');
                    }while((--escaped) > 0);
                    buffer.append('\'');
                }
                if((j - i) % 2 != 0) buffer.append('{');
                i = j - 1;
            }else{
                buffer.append(ch);
            }
        }
        return changed ? buffer.toString() : pattern;
    }

    /**
     * Formats the given {@code pattern} replacing any placeholder of the form {0}, {1}, {2} and so on with the corresponding
     * object from {@code args} converted to a string with {@code toString()}, so without taking into account the locale.
     * <p>
     * This method only implements a small subset of the grammar supported by {@link java.text.MessageFormat}. Especially,
     * placeholder are only made up of an index; neither the type nor the style are supported.
     * <p>
     * If nothing has been replaced this implementation returns the pattern itself.
     * @param pattern the pattern
     * @param args the arguments
     * @return the formatted pattern
     * @throws IllegalArgumentException if the pattern is invalid
     */
    private String simpleFormat(String pattern, Object... args){
        buffer.setLength(0);
        boolean changed = false;
        int placeholder = -1;
        int patternLength = pattern.length();
        for(int i = 0; i < patternLength; ++i){
            char ch = pattern.charAt(i);
            if(placeholder < 0){ // processing constant part
                if(ch == '{'){
                    changed = true;
                    if(i + 1 < patternLength && pattern.charAt(i + 1) == '{'){
                        buffer.append(ch); // handle escaped '{'
                        ++i;
                    }else{
                        placeholder = 0; // switch to placeholder part
                    }
                }else{
                    buffer.append(ch);
                }
            }else{ // processing placeholder part
                if(ch == '}'){
                    if(placeholder >= args.length)
                        throw new IllegalArgumentException("Argument index out of bounds: " + placeholder);
                    if(pattern.charAt(i - 1) == '{')
                        throw new IllegalArgumentException("Missing argument index after a left curly brace");
                    if(args[placeholder] == null)
                        buffer.append("null"); // append null argument
                    else
                        buffer.append(args[placeholder].toString()); // append actual argument
                    placeholder = -1; // switch to constant part
                }else{
                    if(ch < '0' || ch > '9')
                        throw new IllegalArgumentException("Unexpected '" + ch + "' while parsing argument index");
                    placeholder = placeholder * 10 + (ch - '0');
                }
            }
        }
        if(placeholder >= 0) throw new IllegalArgumentException("Unmatched braces in the pattern.");

        return changed ? buffer.toString() : pattern;
    }
}
