package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Links.*;

import static io.anuke.mindustry.Vars.*;

public class AboutDialog extends FloatingDialog{
    private Array<String> contributors = new Array<>();
    private static ObjectSet<String> bannedItems = ObjectSet.with("google-play", "itch.io", "dev-builds");

    public AboutDialog(){
        super("$about.button");

        shown(() -> {
            contributors = Array.with(Core.files.internal("contributors").readString("UTF-8").split("\n"));
            Core.app.post(this::setup);
        });

        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        cont.clear();
        buttons.clear();

        float h = Core.graphics.isPortrait() ? 90f : 80f;
        float w = Core.graphics.isPortrait() ? 330f : 600f;

        Table in = new Table();
        ScrollPane pane = new ScrollPane(in);

        for(LinkEntry link : Links.getLinks()){
            if((ios || OS.isMac || steam) && bannedItems.contains(link.name)){
                continue;
            }

            Table table = new Table(Tex.underline);
            table.margin(0);
            table.table(img -> {
                img.addImage().height(h - 5).width(40f).color(link.color);
                img.row();
                img.addImage().height(5).width(40f).color(link.color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            table.table(i -> {
                i.background(Tex.buttonEdge3);
                i.addImage(Core.atlas.drawable("icon-" + link.name));
            }).size(h - 5, h);

            table.table(inset -> {
                inset.add("[accent]" + Strings.capitalize(link.name.replace("-", " "))).growX().left();
                inset.row();
                inset.labelWrap(link.description).width(w - 100f).color(Color.lightGray).growX();
            }).padLeft(8);

            table.addImageButton(Icon.link, () -> {
                if(link.name.equals("wiki")) Events.fire(Trigger.openWiki);

                if(!Core.net.openURI(link.link)){
                    ui.showErrorMessage("$linkfail");
                    Core.app.setClipboardText(link.link);
                }
            }).size(h - 5, h);

            in.add(table).size(w, h).padTop(5).row();
        }

        shown(() -> Time.run(1f, () -> Core.scene.setScrollFocus(pane)));

        cont.add(pane).growX();

        addCloseButton();

        buttons.addButton("$credits", this::showCredits).size(200f, 64f);

        if(Core.graphics.isPortrait()){
            for(Cell<?> cell : buttons.getCells()){
                cell.width(140f);
            }
        }

    }

    public void showCredits(){
        FloatingDialog dialog = new FloatingDialog("$credits");
        dialog.addCloseButton();
        dialog.cont.add("$credits.text");
        dialog.cont.row();
        if(!contributors.isEmpty()){
            dialog.cont.addImage().color(Pal.accent).fillX().height(3f).pad(3f);
            dialog.cont.row();
            dialog.cont.add("$contributors");
            dialog.cont.row();
            dialog.cont.pane(new Table(){{
                int i = 0;
                left();
                for(String c : contributors){
                    add("[lightgray]" + c).left().pad(3).padLeft(6).padRight(6);
                    if(++i % 3 == 0){
                        row();
                    }
                }
            }});
        }
        dialog.show();
    }
}
