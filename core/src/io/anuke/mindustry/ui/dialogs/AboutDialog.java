package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.io.Contributors;
import io.anuke.mindustry.io.Contributors.Contributor;
import io.anuke.mindustry.ui.Links;
import io.anuke.mindustry.ui.Links.LinkEntry;
import io.anuke.arc.util.Time;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.layout.Cell;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.utils.UIUtils;
import io.anuke.arc.util.OS;
import io.anuke.arc.util.Strings;

import static io.anuke.mindustry.Vars.ios;
import static io.anuke.mindustry.Vars.ui;

public class AboutDialog extends FloatingDialog{
    private Array<Contributor> contributors = new Array<>();
    private static ObjectSet<String> bannedItems = ObjectSet.with("google-play", "itch.io", "dev-builds", "trello");

    public AboutDialog(){
        super("$about.button");

        Contributors.getContributors(out -> contributors = out, Throwable::printStackTrace);

        shown(this::setup);
        onResize(this::setup);
    }

    void setup(){
        cont.clear();
        buttons.clear();

        float h = UIUtils.portrait() ? 90f : 80f;
        float w = UIUtils.portrait() ? 330f : 600f;

        Table in = new Table();
        ScrollPane pane = new ScrollPane(in);

        for(LinkEntry link : Links.getLinks()){
            if((ios || OS.isMac) && bannedItems.contains(link.name)){ //because Apple doesn't like me mentioning things
                continue;
            }

            Table table = new Table("underline");
            table.margin(0);
            table.table(img -> {
                img.addImage("white").height(h - 5).width(40f).color(link.color);
                img.row();
                img.addImage("white").height(5).width(40f).color(link.color.cpy().mul(0.8f, 0.8f, 0.8f, 1f));
            }).expandY();

            table.table(i -> {
                i.background("button-edge-3");
                i.addImage("icon-" + link.name).size(14 * 3f);
            }).size(h - 5, h);

            table.table(inset -> {
                inset.add("[accent]" + Strings.capitalize(link.name.replace("-", " "))).growX().left();
                inset.row();
                inset.labelWrap(link.description).width(w - 100f).color(Color.LIGHT_GRAY).growX();
            }).padLeft(8);

            table.addImageButton("icon-link", 14 * 3, () -> {
                if(!Core.net.openURI(link.link)){
                    ui.showError("$linkfail");
                    Core.app.getClipboard().setContents(link.link);
                }
            }).size(h - 5, h);

            in.add(table).size(w, h).padTop(5).row();
        }

        shown(() -> Time.run(1f, () -> Core.scene.setScrollFocus(pane)));

        cont.add(pane).growX();

        addCloseButton();

        buttons.addButton("$credits", this::showCredits).size(200f, 64f);

        if(!ios && !OS.isMac){
            buttons.addButton("$changelog.title", ui.changelog::show).size(200f, 64f);
        }

        if(UIUtils.portrait()){
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
            dialog.cont.addImage("blank").color(Pal.accent).fillX().height(3f).pad(3f);
            dialog.cont.row();
            dialog.cont.add("$contributors");
            dialog.cont.row();
            dialog.cont.pane(new Table(){{
                int i = 0;
                left();
                for(Contributor c : contributors){
                    add("[lightgray]" + c.login).left().pad(3).padLeft(6).padRight(6);
                    if(++i % 3 == 0){
                        row();
                    }
                }
            }});
        }
        dialog.show();
    }
}
