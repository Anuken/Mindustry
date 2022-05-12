package mindustry.ui.dialogs;

import arc.util.*;

public class FullTextDialog extends BaseDialog{

    public FullTextDialog(){
        super("");
        shouldPause = true;
        addCloseButton();
    }

    public void show(String titleText, String text){
        title.setText(titleText);
        cont.clear();
        cont.add(text).grow().wrap().labelAlign(Align.center);

        super.show();
    }

}
