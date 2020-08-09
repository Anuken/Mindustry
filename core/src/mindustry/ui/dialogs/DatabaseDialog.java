package mindustry.ui.dialogs;

import arc.*;
import arc.input.*;
import arc.struct.*;
import arc.graphics.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.ui;

public class DatabaseDialog extends BaseDialog{

    public DatabaseDialog(){
        super("@database");

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

        Seq<Content>[] allContent = Vars.content.getContentMap();

        for(int j = 0; j < allContent.length; j++){
            ContentType type = ContentType.all[j];

            Seq<Content> array = allContent[j].select(c -> c instanceof UnlockableContent && !((UnlockableContent)c).isHidden());
            if(array.size == 0) continue;

            table.add("@content." + type.name() + ".name").growX().left().color(Pal.accent);
            table.row();
            table.image().growX().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent);
            table.row();
            table.table(list -> {
                list.left();

                int maxWidth = Core.graphics.isPortrait() ? 7 : 13;

                int count = 0;

                for(int i = 0; i < array.size; i++){
                    UnlockableContent unlock = (UnlockableContent)array.get(i);

                    Image image = unlocked(unlock) ? new Image(unlock.icon(Cicon.medium)) : new Image(Icon.lockOpen, Pal.gray);
                    list.add(image).size(8*4).pad(3);
                    ClickListener listener = new ClickListener();
                    image.addListener(listener);
                    if(!Vars.mobile && unlocked(unlock)){
                        image.addListener(new HandCursorListener());
                        image.update(() -> image.color.lerp(!listener.isOver() ? Color.lightGray : Color.white, 0.4f * Time.delta));
                    }

                    if(unlocked(unlock)){
                        image.clicked(() -> {
                            if(Core.input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(unlock.name) != 0){
                                Core.app.setClipboardText((char)Fonts.getUnicode(unlock.name) + "");
                                ui.showInfoFade("@copied");
                            }else{
                                Vars.ui.content.show(unlock);
                            }
                        });
                        image.addListener(new Tooltip(t -> t.background(Tex.button).add(unlock.localizedName)));
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
        return (!Vars.state.isCampaign() && !Vars.state.isMenu()) || content.unlocked();
    }
}
