package io.anuke.mindustry.io.versions;

import io.anuke.mindustry.io.SaveFileVersion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Save15 extends SaveFileVersion {
    private Save14 save = new Save14();

    public Save15(){
        super(15);
    }

    @Override
    public void read(DataInputStream stream) throws IOException {
        
    }

    @Override
    public void write(DataOutputStream stream) throws IOException {

    }
}
