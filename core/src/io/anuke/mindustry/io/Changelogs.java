package io.anuke.mindustry.io;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.function.Consumer;

import static io.anuke.mindustry.Vars.releasesURL;

public class Changelogs{

    public static void getChangelog(Consumer<Array<VersionInfo>> success, Consumer<Throwable> fail){
        Net.http(releasesURL, "GET", result -> {
            JsonReader reader = new JsonReader();
            JsonValue value = reader.parse(result).child;
            Array<VersionInfo> out = new Array<>();

            while(value != null){
                String name = value.getString("name");
                String description = value.getString("body").replace("\r", "");
                int id = value.getInt("id");
                int build = Integer.parseInt(value.getString("tag_name").substring(1));
                out.add(new VersionInfo(name, description, id, build, value.getString("published_at")));
                value = value.next;
            }

            success.accept(out);
        }, fail);
    }

    public static class VersionInfo{
        public final String name, description, date;
        public final int id, build;

        public VersionInfo(String name, String description, int id, int build, String date){
            this.name = name;
            this.description = description;
            this.id = id;
            this.build = build;
            this.date = date;
        }

        @Override
        public String toString(){
            return "VersionInfo{" +
                    "name='" + name + '\'' +
                    ", description='" + description + '\'' +
                    ", id=" + id +
                    ", build=" + build +
                    '}';
        }
    }
}
