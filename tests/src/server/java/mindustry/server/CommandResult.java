package mindustry.server;

import arc.util.Log.*;
import mindustry.server.ServerHarness.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** What a single server console command logged. */
public final class CommandResult{
    public final String command;
    public final List<LogLine> lines;

    CommandResult(String command, List<LogLine> lines){
        this.command = command;
        this.lines = lines;
    }

    /** @return all logged lines, newline separated. */
    public String output(){
        StringJoiner joiner = new StringJoiner("\n");
        lines.forEach(l -> joiner.add(l.text()));
        return joiner.toString();
    }

    public List<String> errors(){
        return lines.stream().filter(l -> l.level() == LogLevel.err).map(LogLine::text).toList();
    }

    public boolean has(String text){
        return lines.stream().anyMatch(l -> l.text().contains(text));
    }

    public CommandResult assertHas(String text){
        assertTrue(has(text), "'" + command + "' did not log '" + text + "'. Output:\n" + output());
        return this;
    }

    /** Asserts that the command logged an error containing the text, and stops that error from failing the test. */
    public CommandResult expectError(String text){
        String found = errors().stream().filter(e -> e.contains(text)).findFirst().orElse(null);
        assertNotNull(found, "'" + command + "' did not log an error containing '" + text + "'. Output:\n" + output());
        ServerHarness.claimError(found);
        return this;
    }
}
