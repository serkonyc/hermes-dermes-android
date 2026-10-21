package ai.serge.dermes;

import java.util.ArrayList;
import java.util.List;

/** Plain data the dashboard draws; produced live (buildState) or from Demo. */
final class DashState {
    static final class Model {
        final String id, state; final double gb;
        Model(String id, String state, double gb) { this.id = id; this.state = state; this.gb = gb; }
    }
    static final class Card {
        final String label, value; final int accent;
        Card(String label, String value, int accent) { this.label = label; this.value = value; this.accent = accent; }
    }

    List<Model> models = new ArrayList<>();
    List<Card> cards = new ArrayList<>();
    int running, waiting;
    double tps;                      // tokens/sec for the big sparkline number
    int requestDelta;                // completed requests since previous refresh
    boolean online;

    static List<Model> modelsFrom(List<DashboardActivity.StarModel> ms) {
        List<Model> out = new ArrayList<>();
        for (DashboardActivity.StarModel m : ms) out.add(new Model(m.id, m.state, m.bytesResident / 1e9));
        return out;
    }
}
