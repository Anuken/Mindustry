package mindustry.annotations.remote;

import java.util.ArrayList;

/** Represents a class witha list method entries to include in it. */
public class ClassEntry{
    /** All methods in this generated class. */
    public final ArrayList<MethodEntry> methods = new ArrayList<>();
    /** Simple class name. */
    public final String name;

    public ClassEntry(String name){
        this.name = name;
    }
}
