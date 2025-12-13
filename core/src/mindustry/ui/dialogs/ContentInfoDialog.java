package mindustry.ui.dialogs;

import arc.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class ContentInfoDialog extends BaseDialog{

    public ContentInfoDialog(){
        super("@info.title");

        addCloseButton();

        keyDown(key -> {
            if(key == Binding.blockInfo.value.key){
                Core.app.post(this::hide);
            }
        });
    }

    public void show(UnlockableContent content){
        cont.clear();

        Table table = new Table();
        table.margin(10);

        //initialize stats if they haven't been yet
        content.checkStats();

        table.table(title1 -> {
            title1.image(content.uiIcon).size(iconXLarge).scaling(Scaling.fit);
            title1.add("[accent]" + content.localizedName + (settings.getBool("console") ? "\n[gray]" + content.name : "")).padLeft(5);
        });

        table.row();

        if(content.description != null){
            var any = content.stats.toMap().size > 0;

            if(any){
                table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
                table.row();
            }

            table.add("[lightgray]" + content.displayDescription()).wrap().fillX().padLeft(any ? 10 : 0).width(500f).padTop(any ? 0 : 10).left();
            table.row();

            if(!content.stats.useCategories && any){
                table.add("@category.general").fillX().color(Pal.accent);
                table.row();
            }
        }

        Stats stats = content.stats;

        for(StatCat cat : stats.toMap().keys()){
            OrderedMap<Stat, Seq<StatValue>> map = stats.toMap().get(cat);

            if(map.size == 0) continue;

            if(stats.useCategories){
                table.add("@category." + cat.name).color(Pal.accent).fillX();
                table.row();
            }

            for(Stat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    inset.add("[lightgray]" + stat.localized() + ":[] ").left().top();
                    Seq<StatValue> arr = map.get(stat);
                    for(StatValue value : arr){
                        value.display(inset);
                        inset.add().size(10f);
                    }
                }).fillX().padLeft(10);
                table.row();
            }
        }

        if(content.details != null){
            table.add("[gray]" + (content.unlocked() || !content.hideDetails ? content.details : Iconc.lock + " " + Core.bundle.get("unlock.incampaign"))).pad(6).padTop(20).width(400f).wrap().fillX();
            table.row();
        }

        if(settings.getBool("console")){
            table.button("@viewfields", Icon.link, Styles.grayt, () -> {
                Class<?> contentClass = content.getClass();
                if(contentClass.isAnonymousClass()) contentClass = contentClass.getSuperclass();

                Core.app.openURI("https://mindustrygame.github.io/wiki/Modding%20Classes/" + contentClass.getSimpleName());
            }).margin(8f).pad(4f).size(300f, 50f).row();
        }

        content.displayExtra(table);

        ScrollPane pane = new ScrollPane(table);
        table.marginRight(30f);
        //TODO: some things (e.g. reconstructor requirements) are too long and screw up the layout
        //pane.setScrollingDisabled(true, false);
        cont.add(pane);

        if(isShown()){
            show(scene, Actions.fadeIn(0f));
        }else{
            show();
        }
    }

}
