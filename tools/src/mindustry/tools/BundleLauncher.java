package mindustry.tools;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

public class BundleLauncher{

    public static void main(String[] args){
        OrderedMap<String, String> base = new OrderedMap<>();
        PropertiesUtils.load(base, Fi.get("bundle.properties").reader());
        Seq<String> removals = new Seq<>();
        String str = Fi.get("bundle.properties").readString();
        ObjectSet<String> newlines = Seq.with(str.split("\n")).select(l -> l.contains(" = ") && str.indexOf(l) + l.length() < str.length() - 2 && str.charAt(str.indexOf(l) + l.length() + 1) == '\n').map(l -> l.split(" = ")[0]).asSet();
        Fi.get(".").walk(child -> {
            if(child.name().equals("bundle.properties") || child.toString().contains("output")) return;

            Log.info("Parsing bundle: @", child);

            OrderedMap<String, String> other = new OrderedMap<>();
            PropertiesUtils.load(other, child.reader(2048, "UTF-8"));
            removals.clear();

            for(String key : other.orderedKeys()){
                if(!base.containsKey(key)){
                    removals.add(key);
                    Log.info("&lr- Removing unused key '@'...", key);
                }
            }
            Log.info("&lr@ keys removed.", removals.size);
            for(String s : removals){
                other.remove(s);
            }

            int added = 0;

            for(String key : base.orderedKeys()){
                if(!other.containsKey(key) || other.get(key).trim().isEmpty()){
                    other.put(key, base.get(key));
                    added++;
                    Log.info("&lc- Adding missing key '@'...", key);
                }
            }

            Func2<String, String, String> processor = (key, value) -> (key + " = " + value).replace("\\", "\\\\").replace("\n", "\\n") + "\n" + (newlines.contains(key) ? "\n" : "");
            Fi output = child.sibling("output/" + child.name());

            Log.info("&lc@ keys added.", added);
            Log.info("Writing bundle to @", output);
            StringBuilder result = new StringBuilder();

            //add everything ordered
            for(String key : base.orderedKeys()){
                result.append(processor.get(key, other.get(key)));
                other.remove(key);
            }

            child.writeString(result.toString());
        });
    }

}
