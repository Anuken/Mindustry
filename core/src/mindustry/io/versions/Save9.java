package mindustry.io.versions;

import mindustry.io.*;

/** Adds support for the new 7-byte custom tile data. This can read Save8 data, but Save8 doesn't know how to handle this version's output, thus the version change. */
public class Save9 extends SaveVersion{

    public Save9(){
        super(9);
    }
}
