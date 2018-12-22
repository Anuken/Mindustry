package io.anuke.mindustry.core;

import io.anuke.arc.Core;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.ui.Dialog;
import io.anuke.arc.scene.ui.TextField;
import io.anuke.arc.util.Time;
import io.anuke.arc.util.serialization.Base64Coder;

import java.util.Random;

import static io.anuke.mindustry.Vars.mobile;

public abstract class Platform {
    /**Each separate game platform should set this instance to their own implementation.*/
    public static Platform instance = new Platform() {};

    /**Add a text input dialog that should show up after the field is tapped.*/
    public void addDialog(TextField field){
        addDialog(field, 16);
    }
    /**See addDialog().*/
    public void addDialog(TextField field, int maxLength){
        if(!mobile) return; //this is mobile only, desktop doesn't need dialogs

        field.tapped(() -> {
            Dialog dialog = new Dialog("", "dialog");
            dialog.setFillParent(true);
            dialog.content().top();
            dialog.content().defaults().height(65f);

            TextField[] use = {null};

            dialog.content().addImageButton("icon-copy", "clear", 16*3, () -> use[0].copy())
                    .visible(() -> !use[0].getSelection().isEmpty()).width(65f);

            dialog.content().addImageButton("icon-paste", "clear", 16*3, () ->
                    use[0].paste(Core.app.getClipboard().getContents(), false))
                    .visible(() -> Core.app.getClipboard() != null && Core.app.getClipboard().getContents() != null && !Core.app.getClipboard().getContents().isEmpty()).width(65f);

            TextField to = dialog.content().addField(field.getText(), t-> {}).pad(15).width(250f).get();
            to.setMaxLength(maxLength);
            to.keyDown(KeyCode.ENTER, () -> dialog.content().find("okb").fireClick());

            use[0] = to;

            dialog.content().addButton("$text.ok", () -> {
                field.clearText();
                field.appendText(to.getText());
                field.change();
                dialog.hide();
                Core.input.setOnscreenKeyboardVisible(false);
            }).width(90f).name("okb");

            dialog.show();
            Time.runTask(1f, () -> {
                to.setCursorPosition(to.getText().length());
                Core.scene.setKeyboardFocus(to);
                Core.input.setOnscreenKeyboardVisible(true);
            });
        });
    }
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
    /**Must be a base64 string 8 bytes in length.*/
    public String getUUID(){
        String uuid = Core.settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new Random().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Core.settings.put("uuid", uuid);
            Core.settings.save();
            return uuid;
        }
        return uuid;
    }
    /**Only used for iOS or android: open the share menu for a map or save.*/
    public void shareFile(FileHandle file){}

    /**Show a file chooser. Desktop only.
     *
     * @param text File chooser title text
     * @param content Description of the type of files to be loaded
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param filetype File extension to filter
     */
    public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, String filetype){}

    /**Forces the app into landscape mode. Currently Android only.*/
    public void beginForceLandscape(){}

    /**Stops forcing the app into landscape orientation. Currently Android only.*/
    public void endForceLandscape(){}
}