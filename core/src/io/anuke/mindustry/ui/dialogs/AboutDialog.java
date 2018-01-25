package io.anuke.mindustry.ui.dialogs;

public class AboutDialog extends FloatingDialog {

    public AboutDialog(){
        super("$text.about.button");

        addCloseButton();
        content().add("$text.about");
    }
}
