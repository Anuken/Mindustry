package arc.util.serialization;

/** An unchecked exception to indicate that an input does not qualify as valid JSON. */
public class JsonParseException extends RuntimeException{
    public final int offset;
    public final int line;
    public final int column;

    JsonParseException(String message, int offset, int line, int column){
        super(message + " at " + line + ":" + column);
        this.offset = offset;
        this.line = line;
        this.column = column;
    }
}
