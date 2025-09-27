package squares;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Main {
    static final Pattern GAME_RE = Pattern.compile(
        "^GAME\\s+(\\d+)\\s*,\\s*(user|comp)\\s+([WBwb])\\s*,\\s*(user|comp)\\s+([WBwb])\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    static final Pattern MOVE_RE = Pattern.compile(
        "^MOVE\\s+(-?\\d+)\\s*,\\s*(-?\\d+)\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    static class Player { final String type; final char color; Player(String t,char c){type=t;color=c;} }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        System.out.println("Type HELP for commands");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        GameState state = null;
        Player p1 = null, p2 = null;

        String line;
        while ((line = br.readLine()) != null) {
            String cmd = line.trim();

            if (cmd.equalsIgnoreCase("EXIT")) return;

            if (cmd.equalsIgnoreCase("HELP")) {
                System.out.println("Commands:\n  GAME N, (user|comp) C1, (user|comp) C2\n  MOVE X, Y\n  HELP\n  EXIT");
                continue;
            }

            // GAME
            Matcher gm = GAME_RE.matcher(cmd);
            if (gm.matches()) {
                int n = Integer.parseInt(gm.group(1));
                if (n <= 2) { System.out.println("Incorrect command"); continue; }

                p1 = new Player(gm.group(2).toLowerCase(), Character.toUpperCase(gm.group(3).charAt(0)));
                p2 = new Player(gm.group(4).toLowerCase(), Character.toUpperCase(gm.group(5).charAt(0)));

                state = GameState.empty(n, 'W'); 
                System.out.println("New game started");
                continue;
            }

            // MOVE
            Matcher mv = MOVE_RE.matcher(cmd);
            if (mv.matches()) {
                if (state == null) { System.out.println("Incorrect command"); continue; }
                int x, y;
                try {
                    x = Integer.parseInt(mv.group(1));
                    y = Integer.parseInt(mv.group(2));
                } catch (Exception e) {
                    System.out.println("Incorrect command");
                    continue;
                }
                try {
                    state = Rules.applyMove(state, x, y, state.turn);
                    // если игра завершилась — объявить
                    if (state.finished) {
                        if (state.winner != null) System.out.println("Game finished. " + state.winner + " wins!");
                        else System.out.println("Game finished. Draw");
                    }
                } catch (IllegalArgumentException ex) {
                    System.out.println("Incorrect command");
                }
                continue;
            }

            System.out.println("Incorrect command");
        }
    }
}
