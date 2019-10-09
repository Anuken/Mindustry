package io.anuke.mindustry.game;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.ContentLoader;
import io.anuke.mindustry.entities.type.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit Tests for PlayerLog.
 */
public class PlayerLogTest{
    private static final int TIMESTAMP = 1234;
    private static final String PLAYER = "Bob";
    private static final String TILE1 = "Steam Gen";
    private static final String TILE2 = "Power Node";
    private static final short IN_XY = 20;
    private static final short OUT_XY = 21;

    private StringBuilder sb;
    private int now;
    private ConcurrentMap<String, Integer> players;
    private Player player;
    private PlayerEvent event;

    @BeforeEach
    public void setUp(){
        sb = new StringBuilder();
        now = 3333;
        players = new ConcurrentHashMap<>();
        players.put(PLAYER, 555);
        players.put("Sally", 1234);
        players.put("Billy", 5678);
        Vars.content = new ContentLoader();
        Vars.content.createContent();
        player = new Player() { };
        event = createEvent(IN_XY);
    }

    private LogFilter createFilter(String... args){
        return new LogFilter(2, args, players, player);
    }

    private PlayerEvent createEvent(short xy){
        return new PlayerEvent(TIMESTAMP, PLAYER, PlayerLog.Action.linked, TILE1, xy, xy);
    }

    private PlayerEvent createEvent(short xy1, short xy2){
        return new PlayerEvent(TIMESTAMP, PLAYER, PlayerLog.Action.linked, TILE1, xy1, xy1, TILE2, xy2, xy2);
    }

    @Test
    public void append_Single_Succeeds(){
        assertEquals("00:34:59: (Bob) linked Steam Gen@20,20\n", event.append(sb, now).toString());
    }

    @Test
    public void append_Double_Succeeds(){
        final PlayerEvent event = createEvent(IN_XY, IN_XY);
        assertEquals("00:34:59: (Bob) linked Steam Gen@20,20 Power Node@20,20\n", event.append(sb, now).toString());
    }

    @Test
    public void LogFilter_NoArgs_Succeeds(){
        assertDoesNotThrow(() -> createFilter());
    }

    @Test
    public void LogFilter_InvalidAction_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("-action"));
        assertEquals("Action must be one of: constructed, deconstructed, linked, unlinked, configured.", e.getMessage());
    }

    @Test
    public void LogFilter_InvalidPage_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("bob", "-3"));
        assertEquals("Page must be > 0.", e.getMessage());
    }

    @Test
    public void LogFilter_InvalidPlayer_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ann"));
        assertEquals("Player ann not found.", e.getMessage());
    }

    @Test
    public void LogFilter_MultiplePlayer_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ly"));
        assertEquals("Did you mean: Billy, Sally?", e.getMessage());
    }

    @Test
    public void LogFilter_ExcessAlphaArg_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("ill", "huh?"));
        assertEquals("Too many arguments.", e.getMessage());
    }

    @Test
    public void LogFilter_ExcessNumberArg_Throws(){
        final Exception e = assertThrows(IllegalArgumentException.class, () -> createFilter("4", "2"));
        assertEquals("Too many arguments.", e.getMessage());
    }

    @Test
    public void matches_Default_True(){
        assertTrue(createFilter().matches(event));
    }

    @Test
    public void matches_OutOfScope_False(){
        assertFalse(createFilter(".").matches(createEvent(OUT_XY)));
    }

    @Test
    public void matches_SecondInScope_True(){
        assertTrue(createFilter(".").matches(createEvent(OUT_XY, IN_XY)));
    }

    @Test
    public void matches_BothOutOfScope_False(){
        assertFalse(createFilter(".").matches(createEvent(OUT_XY, OUT_XY)));
    }

    @Test
    public void matches_Action_False(){
        assertFalse(createFilter("-deconstructed").matches(event));
    }

    @Test
    public void matches_Player_True(){
        assertTrue(createFilter(PLAYER).matches(event));
    }

    @Test
    public void matches_Player_False(){
        final LogFilter filter = createFilter("sally");
        assertFalse(filter.matches(event));
    }

    @Test
    public void matches_Page1_False(){
        final LogFilter filter = createFilter();
        assertTrue(filter.matches(event));
        assertTrue(filter.matches(event));
        assertFalse(filter.matches(event));
    }

    @Test
    public void matches_Page2_True(){
        final LogFilter filter = createFilter("2");
        assertFalse(filter.matches(event));
        assertFalse(filter.matches(event));
        assertTrue(filter.matches(event));
    }

    @Test
    public void summary_Succeeds(){
        final LogFilter filter = createFilter();
        filter.matches(event);
        filter.matches(event);
        filter.matches(event);
        assertEquals("Showing page 1 of 2\n", filter.summary());
    }
}
