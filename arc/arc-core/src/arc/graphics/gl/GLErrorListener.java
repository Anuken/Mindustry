package arc.graphics.gl;

import arc.util.*;

/**
 * Listener for GL errors detected by {@link GLProfiler}.
 * @author Jan Polák
 * @see GLProfiler
 */
public interface GLErrorListener{

    /** Listener that will log using Log.error GL error name and GL function. */
    GLErrorListener loggingListener = error -> {
        String place = getCallName();

        if(place != null){
            Log.err(new RuntimeException(Strings.format("[GLProfiler] Error @ from @", error, place)));
        }else{
            Log.err(new RuntimeException(Strings.format("[GLProfiler] Error @", error)));
        }
    };

    /** Listener that will throw a ArcRuntimeException with error name. */
    GLErrorListener throwingListener = error -> {
        String place = getCallName();

        if(place != null){
            throw new RuntimeException(Strings.format("[GLProfiler] Error @ from @", error, place));
        }else{
            throw new RuntimeException(Strings.format("[GLProfiler] Error @", error));
        }
    };

    /** Put your error logging code here. */
    void onError(String error);

    static @Nullable String getCallName(){
        String place = null;
        try{
            final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for(int i = 0; i < stack.length; i++){
                if("check".equals(stack[i].getMethodName())){
                    if(i + 1 < stack.length){
                        final StackTraceElement glMethod = stack[i + 1];
                        place = glMethod.getMethodName();
                    }
                    break;
                }
            }
        }catch(Exception ignored){
        }
        return place;
    }
}
