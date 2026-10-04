package mindustry.io;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.Files;

//TODO: this class is a collection of random utilities unrelated to mindustry, maybe move it elsehwere?
public class FileIO{

    /** Reads a \n-separated UTF-8 file into a list of non-empty lines. */
    public static Seq<String> readLines(Fi file){
        byte[] bytes = file.readBytes();
        Seq<String> lines = new Seq<>();
        int start = 0;

        for(int i = 0; i <= bytes.length; i++){
            if(i == bytes.length || bytes[i] == '\n'){
                int end = i;
                if(end > start && bytes[end - 1] == '\r') end--;
                if(end > start) lines.add(new String(bytes, start, end - start, StandardCharsets.UTF_8));
                start = i + 1;
            }
        }

        return lines;
    }

    public static void writeLines(Seq<String> lines, Fi file) throws IOException{
        try(OutputStream out = file.write(false, 8192)){
            for(String line : lines){
                out.write(line.getBytes(StandardCharsets.UTF_8));
                out.write('\n');
            }
        }
    }

    /** Writes to a temporary sibling file, then swaps it over the destination. */
    public static void atomicWrite(Fi dest, ConsT<Fi, IOException> writer) throws IOException{
        Fi tmp = dest.sibling(dest.name() + ".tmp");
        writer.get(tmp);
        swap(tmp, dest);
    }

    /** Moves tmp over dest, atomically where the platform supports it. */
    public static void swap(Fi tmp, Fi dest){
        if(!OS.isMobile || (OS.isAndroid && Core.app.getVersion() >= 26)){
            try{
                Files.move(tmp.file().toPath(), dest.file().toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            }catch(IOException e){
                Log.err("Atomic move failed, falling back to non-atomic move.", e);
            }
        }

        tmp.moveTo(dest);
    }
}
