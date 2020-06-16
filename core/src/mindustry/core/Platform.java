package mindustry.core;

import arc.*;
import arc.Input.*;
import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import rhino.*;

import static mindustry.Vars.*;

public interface Platform{

    /** Steam: Update lobby visibility.*/
    default void updateLobby(){}

    /** Steam: Show multiplayer friend invite dialog.*/
    default void inviteFriends(){}

    /** Steam: Share a map on the workshop.*/
    default void publish(Publishable pub){}

    /** Steam: View a listing on the workshop.*/
    default void viewListing(Publishable pub){}

    /** Steam: View a listing on the workshop by an ID.*/
    default void viewListingID(String mapid){}

    /** Steam: Return external workshop maps to be loaded.*/
    default Seq<Fi> getWorkshopContent(Class<? extends Publishable> type){
        return new Seq<>(0);
    }

    /** Steam: Open workshop for maps.*/
    default void openWorkshop(){}

    /** Get the networking implementation.*/
    default NetProvider getNet(){
        return new ArcNetProvider();
    }

    /** Gets the scripting implementation. */
    default Scripts createScripts(){
        return new Scripts();
    }

    default Context getScriptContext(){
        Context c = Context.enter();
        c.setOptimizationLevel(9);
        return c;
    }

    /** Add a text input dialog that should show up after the field is tapped. */
    default void addDialog(TextField field){
        addDialog(field, 16);
    }

    /** See addDialog(). */
    default void addDialog(TextField field, int maxLength){
        if(!mobile) return; //this is mobile only, desktop doesn't need dialogs

        field.tapped(() -> {
            TextInput input = new TextInput();
            input.text = field.getText();
            input.maxLength = maxLength;
            input.accepted = text -> {
                field.clearText();
                field.appendText(text);
                field.change();
                Core.input.setOnscreenKeyboardVisible(false);
            };
            Core.input.getTextInput(input);
        });
    }

    /** Update discord RPC. */
    default void updateRPC(){
    }

    /** Must be a base64 string 8 bytes in length. */
    default String getUUID(){
        String uuid = Core.settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new Rand().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Core.settings.put("uuid", uuid);
            return uuid;
        }
        return uuid;
    }

    /** Only used for iOS or android: open the share menu for a map or save. */
    default void shareFile(Fi file){
    }

    default void export(String name, String extension, FileWriter writer){
        if(!ios){
            platform.showFileChooser(false, extension, file -> {
                ui.loadAnd(() -> {
                    try{
                        writer.write(file);
                    }catch(Throwable e){
                        ui.showException(e);
                        Log.err(e);
                    }
                });
            });
        }else{
            ui.loadAnd(() -> {
                try{
                    Fi result = Core.files.local(name+ "." + extension);
                    writer.write(result);
                    platform.shareFile(result);
                }catch(Throwable e){
                    ui.showException(e);
                    Log.err(e);
                }
            });
        }
    }

    /**
     * Show a file chooser.
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param extension File extension to filter
     */
    default void showFileChooser(boolean open, String extension, Cons<Fi> cons){
        new FileChooser(open ? "$open" : "$save", file -> file.extension().toLowerCase().equals(extension), open, file -> {
            if(!open){
                cons.get(file.parent().child(file.nameWithoutExtension() + "." + extension));
            }else{
                cons.get(file);
            }
        }).show();
    }

    /** Hide the app. Android only. */
    default void hide(){
    }

    /** Forces the app into landscape mode.*/
    default void beginForceLandscape(){
    }

    /** Stops forcing the app into landscape orientation.*/
    default void endForceLandscape(){
    }

    interface FileWriter{
        void write(Fi file) throws Throwable;
    }
}
