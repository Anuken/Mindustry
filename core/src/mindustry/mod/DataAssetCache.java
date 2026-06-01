package mindustry.mod;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;

public class DataAssetCache{
    private static final char[] base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private ObjectMap<String, Fi> hashToFile = new ObjectMap<>();

    public void load(){
        //TODO: is this necessary? you could just resolve the file on the filesystem instead...
        for(Fi file : Vars.assetCacheDirectory.list()){
            hashToFile.put(file.name(), file);
        }
    }

    /** @return the hash */
    public byte[] add(byte[] bytes){
        byte[] hash = Streams.sha256(bytes);
        String name = encodeHash(hash);
        Fi file = Vars.assetCacheDirectory.child(name);
        file.writeBytes(bytes); //TODO: redundant disk writes if the data is already there
        hashToFile.put(name, file);
        return hash;
    }

    public boolean has(String hash){
        return get(hash) != null;
    }

    public @Nullable Fi get(String shaHash){
        if(shaHash == null) return null;
        return hashToFile.get(shaHash);
    }

    public @Nullable Fi get(byte[] shaHash){
        return get(encodeHash(shaHash));
    }

    //base32 without padding
    public static String encodeHash(byte[] data) {
        char[] out = new char[52];
        int di = 0, oi = 0;
        long bits;

        for(int i = 0; i < 6; i++){
            bits = ((long)(data[di++] & 0xFF) << 32) |
                ((long)(data[di++] & 0xFF) << 24) |
                ((long)(data[di++] & 0xFF) << 16) |
                ((long)(data[di++] & 0xFF) << 8)  |
                ((long)(data[di++] & 0xFF));

            out[oi++] = base32Alphabet[(int)(bits >>> 35) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 30) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 25) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 20) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 15) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 10) & 0x1F];
            out[oi++] = base32Alphabet[(int)(bits >>> 5) & 0x1F];
            out[oi++] = base32Alphabet[(int)bits & 0x1F];
        }

        int b0 = data[di++] & 0xFF;
        int b1 = data[di]   & 0xFF;

        out[oi++] = base32Alphabet[(b0 >>> 3) & 0x1F];
        out[oi++] = base32Alphabet[((b0 << 2) & 0x1C) | ((b1 >>> 6) & 0x03)];
        out[oi++] = base32Alphabet[(b1 >>> 1) & 0x1F];
        out[oi++] = base32Alphabet[(b1 << 4) & 0x10];

        return new String(out);
    }
}
