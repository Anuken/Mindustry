package mindustry.io.versions;

import mindustry.io.*;

/** Removes short entity chunks, switching to 4 byte lengths for all chunks. */
public class Save10 extends SaveVersion{

    public Save10(){
        super(10);
    }
}
