package arc.net;

import arc.func.*;

public class ArcNet{
    public static Cons<Throwable> errorHandler = e -> {};

    public static void handleError(Throwable e){
        errorHandler.get(e);
    }
}
