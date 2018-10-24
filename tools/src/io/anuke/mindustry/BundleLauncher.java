package io.anuke.mindustry;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.PropertiesUtils;
import io.anuke.ucore.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BundleLauncher {

    public static void main(String[] args) throws Exception{
        File file = new File("bundle.properties");
        OrderedMap<String, String> base = new OrderedMap<>();
        PropertiesUtils.load(base, new InputStreamReader(new FileInputStream(file)));
        Array<String> removals = new Array<>();

        Files.walk(Paths.get("")).forEach(child -> {
            try {
                if (child.getFileName().toString().equals("bundle.properties") || Files.isDirectory(child) || child.toString().contains("output")) return;

                Log.info("Parsing bundle: {0}", child);

                OrderedMap<String, String> other = new OrderedMap<>();
                PropertiesUtils.load(other, Files.newBufferedReader(child, Charset.forName("UTF-8")));
                removals.clear();

                for(String key : other.orderedKeys()){
                    if(!base.containsKey(key) && !key.contains(".description")){
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
                    if(!other.containsKey(key)){
                        other.put(key, base.get(key));
                        added ++;
                        Log.info("&lc- Adding missing key '{0}'...", key);
                    }
                }

                Path output = child.resolveSibling("output/" + child.getFileName());

                Log.info("&lc{0} keys added.", added);
                Log.info("Writing bundle to {0}", output);
                StringBuilder result = new StringBuilder();
                for(ObjectMap.Entry<String, String> e : other.entries()){
                    result.append((e.key + " = " + e.value).replace("\\", "\\\\").replace("\n", "\\n"));
                    result.append("\n");
                }
                Files.write(child, result.toString().getBytes("UTF-8"));

            }catch (IOException e){
                throw new RuntimeException(e);
            }
        });
    }

}
