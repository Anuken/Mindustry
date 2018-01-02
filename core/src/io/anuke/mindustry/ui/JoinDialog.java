package io.anuke.mindustry.ui;

public class JoinDialog extends FloatingDialog {

    public JoinDialog(){
        super("$text.joingame");
    }

    void setup(){
        content().clear();
    }
}
