package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.TextField;

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
            TextField to = dialog.content().addField(field.getText(), t-> {}).pad(15).width(250f).get();
            to.setMaxLength(maxLength);
            to.keyDown(Keys.ENTER, () -> dialog.content().find("okb").fireClick());
            dialog.content().addButton("$text.ok", () -> {
                field.clearText();
                field.appendText(to.getText());
                field.change();
                dialog.hide();
                Gdx.input.setOnscreenKeyboardVisible(false);
            }).width(90f).name("okb");

            dialog.show();
            Timers.runTask(1f, () -> {
                to.setCursorPosition(to.getText().length());
                Core.scene.setKeyboardFocus(to);
                Gdx.input.setOnscreenKeyboardVisible(true);
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