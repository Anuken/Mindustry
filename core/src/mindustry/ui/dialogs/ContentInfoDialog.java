package mindustry.ui.dialogs;

import arc.*;
import arc.graphics.*;
import arc.scene.actions.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.game.conditions.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
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

        //compute stats every time
        var stats = content.computeStats();
        var statMap = stats.toMap();

        table.table(title1 -> {
            title1.image(content.uiIcon).size(iconXLarge).scaling(Scaling.fit);
            title1.add("[accent]" + content.localizedName + (settings.getBool("console") ? "\n[gray]" + content.name : "")).padLeft(5);
        });

        table.row();

        if(state.isGame() && state.data.isPatched(content)){
            table.table(t -> {
                t.image(Icon.info).color(Pal.lightishGray);
                t.add("@database.patched").color(Pal.lightishGray).padLeft(4f);
            }).pad(4f).left();

            table.row();
        }

        if(content.description != null){
            var any = statMap.size > 0;

            if(any){
                table.add("@category.purpose").color(Pal.accent).fillX().padTop(10);
                table.row();
            }

            table.add("[lightgray]" + UI.formatIcons(content.displayDescription())).wrap().fillX().padLeft(any ? 10 : 0).width(500f).padTop(any ? 0 : 10).left();
            table.row();

            if(!stats.useCategories && any){
                table.add("@category.general").fillX().color(Pal.accent);
                table.row();
            }
        }

        for(StatCat cat : statMap.keys()){
            var map = statMap.get(cat);

            if(map.size == 0) continue;

            if(stats.useCategories){
                table.add(cat.localized()).color(Pal.accent).fillX();
                table.row();
            }

            for(Stat stat : map.keys()){
                table.table(inset -> {
                    inset.left();
                    stats.statInfo(inset.add("[lightgray]" + stat.localized() + ":[] ").left().top(), stat);
                    Seq<StatValue> arr = map.get(stat);
                    for(StatValue value : arr){
                        value.display(inset);
                        inset.add().size(10f);
                    }
                }).fillX().padLeft(10);
                table.row();
            }
        }

        TechNode node = content.techNode;
        if(node != null){
            SectorPreset found = node.content instanceof SectorPreset preset ? preset : node.objectives
                .map(cond -> cond instanceof SectorComplete c ? c.preset : cond instanceof OnSector s ? s.preset : null)
                .find(p -> p != null);
            if(found == null){
                TechNode curr = node.parent;
                while(curr != null){
                    if(curr.content instanceof SectorPreset preset){
                        found = preset;
                        break;
                    }
                    curr = curr.parent;
                }
            }
            SectorPreset sector = found;
            boolean sectorEntry = sector != null && sector != content, nodeEntry = node.parent != null && node.parent.content != content;

            if(sectorEntry || nodeEntry){
                table.left().add("@techtree").color(Pal.accent).fillX().padTop(10).row();
                table.table(tech -> {
                    tech.left();
                    if(sectorEntry){
                        techEntry(tech, "techtree.sector", sector);
                        tech.row();
                    }
                    if(nodeEntry){
                        techEntry(tech, "techtree.previous", node.parent.content);
                    }
                }).fillX().padLeft(10);
                table.row();
            }
        }

        if(content.details != null){
            table.add("[gray]" + (content.unlocked() || !content.hideDetails ? UI.formatIcons(content.details) : Iconc.lock + " " + Core.bundle.get("unlock.incampaign"))).pad(6).padTop(20).width(400f).wrap().fillX();
            table.row();
        }

        //TODO: move this into a final end-game credit sequence. this is temporary and thus not localized
        if(content.credit != null){
            table.row();
            table.add(Core.bundle.format("content.createdby", content.credit)).color(Color.gray).padTop(40f).row();
        }

        if(settings.getBool("console")){
            table.button("@viewfields", Icon.link, Styles.grayt, () -> {
                Class<?> contentClass = content.getClass();
                if(contentClass.isAnonymousClass()) contentClass = contentClass.getSuperclass();

                Core.app.openURI("https://mindustrygame.github.io/wiki/Modding%20Classes/" + contentClass.getSimpleName());
            }).margin(8f).pad(4f).padTop(16f).size(300f, 50f).row();
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

    private void techEntry(Table table, String label, UnlockableContent content){
        table.table(t -> {
            t.left();
            t.add("[lightgray]" + Core.bundle.get(label) + ":").padRight(4f);
            t.image(content.uiIcon).size(iconMed).scaling(Scaling.fit).padRight(4f).with(i -> StatValues.withTooltip(i, content, true));
            t.add("[white]" + content.localizedName).left().padRight(12f);
        }).left().padLeft(10f);
    }
}
