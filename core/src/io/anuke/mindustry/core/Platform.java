package io.anuke.mindustry.core;

import io.anuke.arc.*;
import io.anuke.arc.Input.*;
import io.anuke.arc.files.*;
import io.anuke.arc.function.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.util.serialization.*;
import io.anuke.mindustry.ui.dialogs.*;

import static io.anuke.mindustry.Vars.mobile;

public interface Platform{

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

    /** Request external read/write perms. Run callback when complete.*/
    default void requestExternalPerms(Runnable callback){
        callback.run();
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

    /** Forces the app into landscape mode. Currently Android only. */
    default void beginForceLandscape(){
    }

    /** Stops forcing the app into landscape orientation. Currently Android only. */
    default void endForceLandscape(){
    }
}