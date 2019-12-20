package io.anuke.mindustry.net;

import io.anuke.arc.collection.*;
import io.anuke.arc.util.Log;

import java.util.regex.*;

public class Metadata{
    // stores categorized key value string pairs
    public ObjectMap<String, StringMap> categories = new ObjectMap<>();

    // pack to send it via mods
    public Array<String> pack(){
        Array<String> meta = new Array<>();
        categories.each((category, map) -> {
            map.each((key, value) -> {
                meta.add(category + "-" + key + ":" + value);
            });
        });
        return meta;
    }

    // probably needs better validation to avoid server crashes or something
    public Metadata unpack(Array<String> meta){
        Pattern p = Pattern.compile("^(.*)-(.*):(.*)$");

        meta.each(string -> {
            Matcher m = p.matcher(string);
            if(m.find()){
                if(categories.get(m.group(1)) == null) categories.put(m.group(1), new StringMap());
                categories.get(m.group(1)).put(m.group(2), m.group(3));
//                Log.info("match:");
//                Log.info(m.group(1));
//                Log.info(m.group(2));
//                Log.info(m.group(3));
            }
        });

        return this;
    }
}
