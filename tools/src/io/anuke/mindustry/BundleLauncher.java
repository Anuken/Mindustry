package io.anuke.mindustry;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.OrderedMap;
import io.anuke.arc.func.Func2;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Strings;
import io.anuke.arc.util.io.PropertiesUtils;

import java.io.*;
import java.nio.file.*;

public class BundleLauncher{

    public static void main(String[] args) throws Exception{
        File file = new File("bundle.properties");
        OrderedMap<String, String> base = new OrderedMap<>();
        PropertiesUtils.load(base, new InputStreamReader(new FileInputStream(file)));
        Array<String> removals = new Array<>();

        Files.walk(Paths.get("")).forEach(child -> {
            try{
                if(child.getFileName().toString().equals("bundle.properties") || Files.isDirectory(child) || child.toString().contains("output"))
                    return;

                Log.info("Parsing bundle: {0}", child);

                OrderedMap<String, String> other = new OrderedMap<>();
                PropertiesUtils.load(other, Files.newBufferedReader(child, Strings.utf8));
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

                Path output = child.resolveSibling("output/" + child.getFileName());

                Log.info("&lc{0} keys added.", added);
                Log.info("Writing bundle to {0}", output);
                StringBuilder result = new StringBuilder();

                //add everything ordered
                for(String key : base.orderedKeys()){
                    result.append(processor.get(key, other.get(key)));
                    other.remove(key);
                }

                Files.write(child, result.toString().getBytes(Strings.utf8));

            }catch(IOException e){
                throw new RuntimeException(e);
            }
        });
    }

}
