package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;

public class DatabaseDialog extends FloatingDialog{

    public DatabaseDialog(){
        super("$database");

        shouldPause = true;
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    void rebuild(){
        cont.clear();

        Table table = new Table();
        table.margin(20);
        ScrollPane pane = new ScrollPane(table);

        Array<Content>[] allContent = Vars.content.getContentMap();

        for(int j = 0; j < allContent.length; j++){
            ContentType type = ContentType.values()[j];

            Array<Content> array = allContent[j].select(c -> c instanceof UnlockableContent && !((UnlockableContent)c).isHidden());
            if(array.size == 0) continue;

            table.add("$content." + type.name() + ".name").growX().left().color(Pal.accent);
            table.row();
            table.addImage().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
            table.row();
            table.table(list -> {
                list.left();

                int maxWidth = Core.graphics.isPortrait() ? 7 : 13;

                int count = 0;

                for(int i = 0; i < array.size; i++){
                    UnlockableContent unlock = (UnlockableContent)array.get(i);

                    Image image = unlocked(unlock) ? new Image(unlock.icon(Cicon.medium)) : new Image(Icon.lockedSmall, Pal.gray);
                    list.add(image).size(8*4).pad(3);
                    ClickListener listener = new ClickListener();
                    image.addListener(listener);
                    if(!Vars.mobile && unlocked(unlock)){
                        image.addListener(new HandCursorListener());
                        image.update(() -> image.getColor().lerp(!listener.isOver() ? Color.lightGray : Color.white, 0.4f * Time.delta()));
                    }

                    if(unlocked(unlock)){
                        image.clicked(() -> Vars.ui.content.show(unlock));
                        image.addListener(new Tooltip(t -> t.background(Tex.button).add(unlock.localizedName())));
                    }

                    if((++count) % maxWidth == 0){
                        list.row();
                    }
                }
            }).growX().left().padBottom(10);
            table.row();
        }

        cont.add(pane);
    }

    boolean unlocked(UnlockableContent content){
        return (!Vars.world.isZone() && !Vars.state.is(State.menu)) || content.unlocked();
    }
}
