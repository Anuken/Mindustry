package mindustry.io.versions;

import mindustry.io.*;

/** Adds support for the marker binary data region. The code is unchanged here, because it was easier to add a >= 8 check in the SaveVersion class itself. */
public class Save8 extends SaveVersion{

    public Save8(){
        super(8);
    }
}
