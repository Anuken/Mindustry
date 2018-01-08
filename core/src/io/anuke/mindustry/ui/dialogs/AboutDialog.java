package io.anuke.mindustry.ui.dialogs;

import static io.anuke.mindustry.Vars.aboutText;

public class AboutDialog extends FloatingDialog {

    public AboutDialog(){
        super("$text.about.button");

        addCloseButton();

        for(String text : aboutText){
            content().add(text).left();
            content().row();
        }
    }
}
