package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.io.Changelogs;
import io.anuke.mindustry.io.Changelogs.VersionInfo;
import io.anuke.mindustry.game.Version;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.OS;

import static io.anuke.mindustry.Vars.ios;

public class ChangelogDialog extends FloatingDialog{
    private final float vw = 600;
    private Array<VersionInfo> versions;

    public ChangelogDialog(){
        super("$text.changelog.title");

        addCloseButton();

        content().add("$text.changelog.loading");

        if(!ios && !OS.isMac){
            Changelogs.getChangelog(result -> {
                versions = result;
                Gdx.app.postRunnable(this::setup);
            }, t -> {
                Log.err(t);
                Gdx.app.postRunnable(this::setup);
            });
        }
    }

    void setup(){
        Table table = new Table();
        ScrollPane pane = new ScrollPane(table);

        content().clear();
        content().add(pane).grow();

        if(versions == null){
            table.add("$text.changelog.error");
            if(Vars.android){
                table.row();
                table.add("$text.changelog.error.android").padTop(8);
            }

            if(ios){
                table.row();
                table.add("$text.changelog.error.ios").padTop(8);
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
                    in.add("$text.changelog.current");
                }else if(info == versions.first()){
                    in.row();
                    in.add("$text.changelog.latest");
                }
                in.row();
                in.labelWrap("[lightgray]" + desc).width(vw - 20).padTop(12);

                table.add(in).width(vw).pad(8).row();
            }

            int lastid = Settings.getInt("lastBuild");
            if(lastid != 0 && versions.peek().build > lastid){
                Settings.putInt("lastBuild", versions.peek().build);
                Settings.save();
                show();
            }
        }
    }
}
