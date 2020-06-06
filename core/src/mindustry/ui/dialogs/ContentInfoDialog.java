package mindustry.ui.dialogs;

import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import mindustry.ctype.UnlockableContent;

public class ContentInfoDialog extends BaseDialog{

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
