package mindustry.desktop;

import arc.backend.sdl.jni.*;
import arc.util.*;

import javax.swing.*;

//placed in a separate class due to the javawx.swing dependency, which is not present in the bundled JVM
public class ErrorDialog{

    public static void show(String text){
        Log.err(text);
        try{
            //will fail in the future on 32-bit platforms as no natives will be loaded
            SDL.SDL_ShowSimpleMessageBox(SDL.SDL_MESSAGEBOX_ERROR, "it's over", text);
        }catch(Throwable error){
            try{
                //usually won't work on packaged JVMs, but I won't be distributing those with 32 bit windows anyway
                JOptionPane.showMessageDialog(null, text);
            }catch(Throwable ignored){
            }
        }
        System.exit(1);
    }
}
