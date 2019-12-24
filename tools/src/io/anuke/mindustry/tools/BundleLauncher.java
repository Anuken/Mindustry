package io.anuke.mindustry.tools;

import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.func.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.io.*;

import java.io.*;

public class BundleLauncher{

    public static void main(String[] args) throws Exception{
        File file = new File("bundle.properties");
        OrderedMap<String, String> base = new OrderedMap<>();
        PropertiesUtils.load(base, new InputStreamReader(new FileInputStream(file)));
        Array<String> removals = new Array<>();
        Fi.get("").walk(child -> {
            if(child.name().equals("bundle.properties") || child.isDirectory() || child.toString().contains("output"))
                return;

            Log.info("Parsing bundle: {0}", child);

            OrderedMap<String, String> other = new OrderedMap<>();
            PropertiesUtils.load(other, child.reader(2048, "UTF-8"));
            removals.clear();

            for(String key : other.orderedKeys()){
                if(!base.containsKey(key)){
                    removals.add(key);
                    Log.info("&lr- Removing unused key '{0}'...", key);
                }
            }
            Log.info("&lr{0} keys removed.", removals.size);
            for(String s : removals){
                other.remove(s);
            }

            int added = 0;

            for(String key : base.orderedKeys()){
                if(!other.containsKey(key) || other.get(key).trim().isEmpty()){
                    other.put(key, base.get(key));
                    added++;
                    Log.info("&lc- Adding missing key '{0}'...", key);
                }
            }

            Func2<String, String, String> processor = (key, value) -> (key + " = " + value).replace("\\", "\\\\").replace("\n", "\\n") + "\n";
            Fi output = child.sibling("output/" + child.name());

            Log.info("&lc{0} keys added.", added);
            Log.info("Writing bundle to {0}", output);
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
