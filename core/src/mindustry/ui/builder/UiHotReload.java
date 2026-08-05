package mindustry.ui.builder;

import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

/** Utility class for showing a MSUI dialog box that automatically reloads changes for a certain file.
 * Desktop only. Usage: Just run UiHotReload.show() in the JS console. */
public class UiHotReload{

    /** Shows a file picker for the dialog box. */
    public static void show(){

        FileChooser.open("msui").submit(file -> {
            long[] lastUpdate = {file.lastModified()}, debounceTime = {0};
            boolean[] modified = {false};

            BaseDialog dialog = new BaseDialog("Mindustry DSL Preview");
            dialog.update(() -> {
                if(lastUpdate[0] != file.lastModified()){
                    lastUpdate[0] = file.lastModified();
                    modified[0] = true;
                    debounceTime[0] = Time.millis();
                }

                if(modified[0] && Time.timeSinceMillis(debounceTime[0]) > 100){
                    modified[0] = false;
                    reload(file, dialog);
                }
            });
            reload(file, dialog);
            dialog.addCloseButton();
            dialog.show();
        });
    }

    static void reload(Fi file, BaseDialog dialog){
        try{
            dialog.cont.clear();
            UiTreeBuilder.build(dialog.cont, UiBuilder.parse(file.readString()), result -> {
                Vars.ui.showInfo(result.toString());
            });
        }catch(Exception e){
            dialog.cont.clearChildren();
            dialog.cont.add("[scarlet]Error reading file: \n" + Strings.getStackTrace(e)).center().labelAlign(Align.center);
        }
    }
}
