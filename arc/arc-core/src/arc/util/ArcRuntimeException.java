package arc.util;

public class ArcRuntimeException extends RuntimeException{
    private static final long serialVersionUID = 6735854402467673117L;

    public ArcRuntimeException(String message){
        super(message);
    }

    public ArcRuntimeException(Throwable t){
        super(t);
    }

    public ArcRuntimeException(String message, Throwable t){
        super(message, t);
    }
}
