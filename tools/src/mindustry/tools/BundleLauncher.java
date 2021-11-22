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

        Fi.get(".").walk(child -> {
            if(child.name().equals("bundle.properties") || child.toString().contains("output")) return;

            Log.info("Parsing bundle: @", child);

            OrderedMap<String, String> other = new OrderedMap<>();

            //find the last known comment of each line
            ObjectMap<String, String> comments = new ObjectMap<>();
            StringBuilder curComment = new StringBuilder();

            for(String line : Seq.with(child.readString().split("\n", -1))){
                if(line.startsWith("#") || line.isEmpty()){
                    curComment.append(line).append("\n");
                }else if(line.contains("=")){
                    String lastKey = line.substring(0, line.indexOf("=")).trim();
                    if(curComment.length() != 0){
                        comments.put(lastKey, curComment.toString());
                        curComment.setLength(0);
                    }
                }
            }

            ObjectMap<String, String> extras = new OrderedMap<>();
            PropertiesUtils.load(other, child.reader());
            removals.clear();

            for(String key : other.orderedKeys()){
                if(!base.containsKey(key) && key.contains(".details")){
                    extras.put(key, other.get(key));
                }else if(!base.containsKey(key)){
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

            Func2<String, String, String> processor = (key, value) ->
                (comments.containsKey(key) ? comments.get(key) : "") + //append last known comment if present
                (key + " =" + (value.trim().isEmpty() ? "" : " ") + uniEscape(value)).replace("\n", "\\n") + "\n";
            Fi output = child.sibling("output/" + child.name());

            Log.info("&lc@ keys added.", added);
            Log.info("Writing bundle to @", output);
            StringBuilder result = new StringBuilder();

            //add everything ordered
            for(String key : base.orderedKeys().and(extras.keys().toSeq())){
                result.append(processor.get(key, other.get(key)));
                other.remove(key);
            }

            child.writeString(result.toString());
        });
    }

    static String uniEscape(String string){
        StringBuilder outBuffer = new StringBuilder();
        int len = string.length();
        for(int i = 0; i < len; i++){
            char ch = string.charAt(i);
            if((ch > 61) && (ch < 127)){
                outBuffer.append(ch == '\\' ? "\\\\" : ch);
                continue;
            }

            if(ch >= 0xE000){
                String hex = Integer.toHexString(ch);
                outBuffer.append("\\u");
                for(int j = 0; j < 4 - hex.length(); j++){
                    outBuffer.append('0');
                }
                outBuffer.append(hex);
            }else{
                outBuffer.append(ch);
            }
        }

        return outBuffer.toString();
    }

}
