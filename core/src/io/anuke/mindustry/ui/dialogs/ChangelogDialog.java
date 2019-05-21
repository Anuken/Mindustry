package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.OS;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.Version;
import io.anuke.mindustry.io.Changelogs;
import io.anuke.mindustry.io.Changelogs.VersionInfo;

import static io.anuke.mindustry.Vars.ios;

public class ChangelogDialog extends FloatingDialog{
    private final float vw = 600;
    private Array<VersionInfo> versions;

    public ChangelogDialog(){
        super("$changelog.title");

        addCloseButton();

        cont.add("$changelog.loading");

        shown(() -> {
            if(!ios && !OS.isMac){
                Changelogs.getChangelog(result -> {
                    versions = result;
                    Core.app.post(this::setup);
                }, t -> {
                    Log.err(t);
                    Core.app.post(this::setup);
                });
            }
        });
    }

    void setup(){
        Table table = new Table();
        ScrollPane pane = new ScrollPane(table);

        cont.clear();
        cont.add(pane).grow();

        if(versions == null){
            table.add("$changelog.error");
            if(Vars.android){
                table.row();
                table.add("$changelog.error.android").padTop(8);
            }

            if(ios){
                table.row();
                table.add("$changelog.error.ios").padTop(8);
            }
        }else{
            for(VersionInfo info : versions){
                String desc = info.description;

                desc = desc.replace("Android", "Mobile");

                Table in = new Table("underline");
                in.top().left().margin(10);

                in.add("[accent]" + info.name + "[LIGHT_GRAY]  | " + info.date);
                if(info.build == Version.build){
                    in.row();
                    in.add("$changelog.current");
                }else if(info == versions.first()){
                    in.row();
                    in.add("$changelog.latest");
                }
                in.row();
                in.labelWrap("[lightgray]" + desc).width(vw - 20).padTop(12);

                table.add(in).width(vw).pad(8).row();
            }

            int lastid = Core.settings.getInt("lastBuild");
            if(lastid != 0 && versions.peek().build > lastid){
                Core.settings.put("lastBuild", versions.peek().build);
                Core.settings.save();
                show();
            }
        }
    }
}
