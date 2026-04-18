package mindustry.logic;

import arc.struct.Seq;
import mindustry.Vars;
import mindustry.mod.DataPatcher;

import java.util.Objects;

import static mindustry.logic.LStatements.patches;

public enum PatchSerEnum {
    create("create", (str, s) -> patches.put(str, new Seq<>())),
    addPatch("addPatch", (str, s) -> (patches.containsKey(str) ? patches.get(str) : new Seq<String>()).add(s)),
    apply("apply", (str, s) -> {
        // I should have used a better method...
        patches.get(str).remove(string -> Objects.equals(string.split(":")[0], "name"));
        // Why differentiate between these?
        patches.get(str).add("name: \"Processor#"+str+"\"");
        StringBuilder builder = new StringBuilder();
        for (String content : patches.get(str)) {
            builder.append(content).append("\n");
        }
        // This is not a good method, but if the output is added, the error will fill the log screen.
        try {
            // I should have used a better method...
            Vars.state.patcher.apply(new Seq<>(new String[]{builder.toString()}));
        } catch (Exception ignored) {}
    }),
    remove("remove", (str, s) -> {
        // Why differentiate between these?
        Vars.state.patcher.patches.removeAll(cp -> Objects.equals(cp.name, "Processor#" + str));
        patches.remove(str);
    }),
    clone("clone", (str, s) -> {
        DataPatcher.PatchSet p = Vars.state.patcher.patches.select(patch -> Objects.equals(patch.name, str)).get(0);
        patches.put(s, new Seq<>(new String[]{p.patch}));
    })
    ;

    public final String displayName;
    public final Op op;
    PatchSerEnum(String name, Op op) {
        displayName  = name;
        this.op = op;
    }
    interface Op {
        void get(String str, String arg);
    }
}