package io.anuke.mindustry.core;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.TextField;

import java.util.Date;
import java.util.Locale;
import java.util.Random;

public abstract class Platform {
    /**Each separate game platform should set this instance to their own implementation.*/
    public static Platform instance = new Platform() {};

    /**Format the date using the default date formatter.*/
    public String format(Date date){return "invalid";}
    /**Format a number by adding in commas or periods where needed.*/
    public String format(int number){return "invalid";}
    /**Show a native error dialog.*/
    public void showError(String text){}
    /**Add a text input dialog that should show up after the field is tapped.*/
    public void addDialog(TextField field){
        addDialog(field, 16);
    }
    /**See addDialog().*/
    public void addDialog(TextField field, int maxLength){}
    /**Update discord RPC.*/
    public void updateRPC(){}
    /**Called when the game is exited.*/
    public void onGameExit(){}
    /**Open donation dialog. Currently android only.*/
    public void openDonations(){}
    /**Whether donating is supported.*/
    public boolean canDonate(){
        return false;
    }
    /**Whether the user has Discord installed. Defaults to true if unknown.*/
    public boolean hasDiscord(){return true;}
    /**Return the localized name for the locale. This is basically a workaround for GWT not supporting getName().*/
    public String getLocaleName(Locale locale){
        return locale.toString();
    }
    /**Whether debug mode is enabled.*/
    public boolean isDebug(){return false;}
    /**Must be a base64 string 8 bytes in length.*/
    public String getUUID(){
        String uuid = Settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new Random().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Settings.putString("uuid", uuid);
            Settings.save();
            return uuid;
        }
        return uuid;
    }
    /**Only used for iOS or android: open the share menu for a map or save.*/
    public void shareFile(FileHandle file){}
    /**Download a file. Only used on GWT backend.*/
    public void downloadFile(String name, byte[] bytes){}

    /**Show a file chooser. Desktop only.
     *
     * @param text File chooser title text
     * @param content Description of the type of files to be loaded
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param filetype File extension to filter
     */
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, String filetype){}
    /**Use the default thread provider from the kryonet module for this.*/
    public ThreadProvider getThreadProvider(){
        return new ThreadProvider() {
            @Override public boolean isOnThread() {return true;}
            @Override public void sleep(long ms) {}
            @Override public void start(Runnable run) {}
            @Override public void stop() {}
            @Override public void notify(Object object) {}
            @Override public void wait(Object object) {}
        };
    }

    //TODO iOS implementation
    /**Forces the app into landscape mode. Currently Android only.*/
    public void beginForceLandscape(){}

    //TODO iOS implementation
    /**Stops forcing the app into landscape orientation. Currently Android only.*/
    public void endForceLandscape(){}
}