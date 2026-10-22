package arc.util;

import arc.func.*;

public interface Eachable<T>{
    void each(Cons<? super T> cons);
}
