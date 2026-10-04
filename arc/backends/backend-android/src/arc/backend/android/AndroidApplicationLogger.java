package arc.backend.android;

import android.util.*;
import arc.*;
import arc.util.Log.*;

public class AndroidApplicationLogger implements LogHandler{

    @Override
    public void log(LogLevel level, String text){
        if(level == LogLevel.info){
            Log.i(Core.settings == null || Core.settings.getAppName() == null ? "Arc" : Core.settings.getAppName(), text);
        }else if(level == LogLevel.warn){
            Log.w(Core.settings == null || Core.settings.getAppName() == null ? "Arc" : Core.settings.getAppName(), text);
        }else if(level == LogLevel.err){
            Log.e(Core.settings == null || Core.settings.getAppName() == null ? "Arc" : Core.settings.getAppName(), text);
        }else if(level == LogLevel.debug){
            Log.d(Core.settings == null || Core.settings.getAppName() == null ? "Arc" : Core.settings.getAppName(), text);
        }
    }
}
