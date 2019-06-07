package io.anuke.mindustry.core;

import io.anuke.arc.Core;
import io.anuke.arc.Input.TextInput;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.math.RandomXS128;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.util.serialization.Base64Coder;

import static io.anuke.mindustry.Vars.mobile;

public abstract class Platform{
    /** Each separate game platform should set this instance to their own implementation. */
    public static Platform instance = new Platform(){
    };

    /** Add a text input dialog that should show up after the field is tapped. */
    public void addDialog(TextField field){
        addDialog(field, 16);
    }

    /** See addDialog(). */
    public void addDialog(TextField field, int maxLength){
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
    public void updateRPC(){
    }

    /** Whether donating is supported. */
    public boolean canDonate(){
        return false;
    }

    /** Must be a base64 string 8 bytes in length. */
    public String getUUID(){
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
    public void shareFile(FileHandle file){
    }

    /**
     * Show a file chooser.
     * @param text File chooser title text
     * @param content Description of the type of files to be loaded
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param filetype File extension to filter
     */
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, Predicate<String> filetype){
    }

    /** Hide the app. Android only. */
    public void hide(){
    }

    /** Forces the app into landscape mode. Currently Android only. */
    public void beginForceLandscape(){
    }

    /** Stops forcing the app into landscape orientation. Currently Android only. */
    public void endForceLandscape(){
    }
}