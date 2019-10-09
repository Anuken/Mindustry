package io.anuke.mindustry.game;

import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.Time;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.world.Tile;

/**
 * Maintains a fixed-window log of player actions, primarily to aid griefer detection.
 */
public class PlayerLog{
    public enum Action{
        constructed,
        deconstructed,
        linked,
        unlinked,
        configured;

        public static final String LIST = list();

        public static String list(){
            final StringBuilder sb = new StringBuilder();
            for(Action value : values()){
                sb.append(", ").append(value);
            }
            return sb.toString().substring(2);
        }
    }
    /** The size of the log; for 16 players averaging 200 APM this will save about 5 min of actions */
    public static final int BUFFER_SIZE = 16*1024;
    /** Events per "page" when returning results */
    public static final int PAGE_SIZE = 16;

    private final ObjectSet<String> players;
    private final PlayerEvent[] ringBuffer;
    private final int pageSize;
    private int last;

    public PlayerLog(int size, int pageSize){
        this.players = new ObjectSet<>();
        this.ringBuffer = new PlayerEvent[size];
        this.last = 0;
        this.pageSize = pageSize;
    }

    /** Records a new event and updates the player map */
    public void record(Player player, Action action, Tile tile1, Tile tile2){
        int now = (int)(Time.time() / 60f);
        final String name = player == null ? "<unknown>" : player.name;
        players.add(name);
        int next = last++;
        if(last == ringBuffer.length) last = 0;
        ringBuffer[next] = tile2 == null
            ? new PlayerEvent(now, name, action, tile1.block().localizedName(), tile1.x, tile1.y)
            : new PlayerEvent(now, name, action, tile1.block().localizedName(), tile1.x, tile1.y,
                tile2.block().localizedName(), tile2.x, tile2.y);
    }

    /** Returns matching log records from newest to oldest as a String */
    public String search(String[] args, Player player){
        try{
            final LogFilter filter = new LogFilter(pageSize, args, players, player);
            // Build result
            final StringBuilder sb = new StringBuilder();
            // There is a race condition where another thread could call record() and inject a new record in place of the
            // oldest.  This is a benign race and is not worth avoiding.
            int now = (int) (Time.time() / 60f);
            for(int i = last - 1; i != last; --i){
                if(i < 0){
                    i = ringBuffer.length - 1;
                }
                final PlayerEvent event = ringBuffer[i];
                // If the buffer wraps, there will be events up to the end
                if(event == null) break;
                // Only add matching events
                if(filter.matches(event)){
                    event.append(sb, now);
                }
            }
            final String result = sb.toString();
            return result.isEmpty() ? "No results." : filter.summary() + result;
        }catch(IllegalArgumentException e){
            return e.getMessage();
        }
    }
}

class PlayerEvent{
    final int timestamp;
    final String player;
    final PlayerLog.Action action;
    final String tile1;
    final short x1, y1;
    final String tile2;
    final short x2, y2;

    PlayerEvent(int timestamp, String player, PlayerLog.Action action, String tile1, short x1, short y1){
        this(timestamp, player, action, tile1, x1, y1, null, Short.MIN_VALUE, Short.MIN_VALUE);
    }

    PlayerEvent(int timestamp, String player, PlayerLog.Action action, String tile1, short x1, short y1, String tile2, short x2, short y2){
        this.timestamp = timestamp;
        this.player = player;
        this.action = action;
        this.tile1 = tile1;
        this.x1 = x1;
        this.y1 = y1;
        this.tile2 = tile2;
        this.x2 = x2;
        this.y2 = y2;
    }

    StringBuilder append(StringBuilder sb, int now){
        int seconds = now - timestamp;
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        int hours = minutes / 60;
        minutes -= hours * 60;
        sb.append(String.format("%02d:%02d:%02d: (%s) %s %s@%d,%d",
                hours, minutes, seconds, player, action, tile1, x1, y1));
        if (tile2 != null){
            sb.append(String.format(" %s@%d,%d", tile2, x2, y2));
        }
        return sb.append('\n');
    }
}

class LogFilter{
    private final int pageSize;
    private final Rectangle scope;
    private PlayerLog.Action action;
    private String name;
    private int pageBegin;
    private int pageEnd;
    private int count;

    LogFilter(int pageSize, String[] args, ObjectSet<String> players, Player player){
        // Default to page 0, whole map scope, all players
        this.pageSize = pageSize;
        this.scope = new Rectangle(0, 0, Float.MAX_VALUE, Float.MAX_VALUE);
        this.action = null;
        this.name = null;
        this.pageBegin = 0;
        this.pageEnd = pageSize;
        this.count = 0;
        // Parse args[]
        int a = 0;
        // Search near me
        if(a < args.length && args[a].equals(".")){
            ++a;
            scope.set(player.x / 8f - 20, player.y / 8f - 20, 40, 40);
        }
        // Action
        if(a < args.length && args[a].startsWith("-")){
            final String prefix = args[a++].substring(1);
            for(PlayerLog.Action candidate : PlayerLog.Action.values()){
                if(candidate.name().startsWith(prefix)){
                    action = candidate;
                    break;
                }
            }
            if(action == null){
                throw new IllegalArgumentException("Action must be one of: " + PlayerLog.Action.LIST + '.');
            }
        }
        while(a < args.length){
            try{
                // Page number
                pageBegin = (Integer.parseInt(args[a]) - 1) * pageSize;
                pageEnd = pageBegin + pageSize;
                if(pageBegin < 0){
                    throw new IllegalArgumentException("Page must be > 0.");
                }
                ++a;
                break;
            }catch(NumberFormatException e){
                // Player name
                if(name != null) break;
                name = findPlayer(players, args[a]);
                if(name == null){
                    throw new IllegalArgumentException("Player " + args[a] + " not found.");
                }else if(name.contains(",")){
                    throw new IllegalArgumentException("Did you mean: " + name + "?");
                }
                ++a;
            }
        }
        if(a < args.length){
            throw new IllegalArgumentException("Too many arguments.");
        }
    }

    public boolean matches(PlayerEvent event){
        if((scope.contains(event.x1, event.y1) || scope.contains(event.x2, event.y2))
            && (name == null || name.equals(event.player))
            && (action == null || action == event.action)){
            ++count;
            return pageBegin < count && count <= pageEnd;
        }
        return false;
    }

    public String summary(){
        return "Showing page " + (pageBegin / pageSize + 1) + " of " + (count + pageSize - 1) / pageSize + '\n';
    }

    private String findPlayer(ObjectSet<String> players, String player){
        // Exact match is best
        if(players.contains(player)){
            return player;
        }
        // Look for case-sensitive match
        String result = null;
        for(String name : players){
            if(name.contains(player)){
                if(result == null){
                    result = name;
                }else{
                    result += ", " + name;
                }
            }
        }
        if(result != null) return result;
        // Look for case-insensitive match
        player = player.toLowerCase();
        for(String name : players){
            if(name.toLowerCase().contains(player)){
                if(result == null){
                    result = name;
                }else{
                    result += ", " + name;
                }
            }
        }
        return result;
    }
}
