package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.ctype.UnlockableContent;

public class ContentInfoDialog extends FloatingDialog{

    public ContentInfoDialog(){
        super("$info.title");

        addCloseButton();
    }

    public void show(UnlockableContent content){
        cont.clear();

        Table table = new Table();
        table.margin(10);

        content.displayInfo(table);

        ScrollPane pane = new ScrollPane(table);
        cont.add(pane);

        show();
    }
}
