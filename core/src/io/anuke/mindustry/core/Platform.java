package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.Input.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.maps.*;
import io.anuke.mindustry.net.*;
import io.anuke.mindustry.net.Net.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.mobile;

public interface Platform{

    /** Steam: Update lobby visibility.*/
    default void updateLobby(){}

    /** Steam: Show multiplayer friend invite dialog.*/
    default void inviteFriends(){}

    /** Steam: Share a map on the workshop.*/
    default void publishMap(Map map){}

    /** Steam: Return external workshop maps to be loaded.*/
    default Array<FileHandle> getExternalMaps(){
        return Array.with();
    }

    /** Steam: Return external workshop mods to be loaded.*/
    default Array<FileHandle> getExternalMods(){
        return Array.with();
    }

    /** Steam: View a map listing on the workshop.*/
    default void viewMapListing(Map map){}

    /** Steam: View a listing on the workshop.*/
    default void viewListing(String mapid){}

    /** Steam: View map workshop info, removing the map ID tag if its listing is deleted.
     * Also presents the option to update the map. */
    default void viewMapListingInfo(Map map){}

    /** Steam: Open workshop for maps.*/
    default void openWorkshop(){}

    /** Get the networking implementation.*/
    default NetProvider getNet(){
        return new ArcNetImpl();
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

    /** Whether donating is supported. */
    default boolean canDonate(){
        return false;
    }

    /** Must be a base64 string 8 bytes in length. */
    default String getUUID(){
        String uuid = Core.settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new RandomXS128().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Core.settings.put("uuid", uuid);
            Core.settings.save();
            return uuid;
        }
        return uuid;
    }

    /** Only used for iOS or android: open the share menu for a map or save. */
    default void shareFile(FileHandle file){
    }

    /**
     * Show a file chooser.
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param extension File extension to filter
     */
    default void showFileChooser(boolean open, String extension, Consumer<FileHandle> cons){
        new FileChooser(open ? "$open" : "$save", file -> file.extension().toLowerCase().equals(extension), open, file -> {
            if(!open){
                cons.accept(file.parent().child(file.nameWithoutExtension() + "." + extension));
            }else{
                cons.accept(file);
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
}