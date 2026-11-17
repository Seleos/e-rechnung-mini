package einvoice.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Maps the displayed units to the codes required by the standard (UN/ECE Rec. 20). */
public final class Units {
    private static final Map<String, String> CODES = new LinkedHashMap<>();

    static {
        CODES.put("h", "HUR");
        CODES.put("Stk", "H87");
        CODES.put("Tag", "DAY");
        CODES.put("Monat", "MON");
        CODES.put("pauschal", "LS");
        CODES.put("km", "KMT");
        CODES.put("min", "MIN");
    }

    private Units() {
    }

    /**
     * Returns the units offered in the user interface.
     *
     * @return the display names, for example "h" or "Stk"
     */
    public static List<String> all() {
        return List.copyOf(CODES.keySet());
    }

    /**
     * Translates a displayed unit into its code.
     *
     * @param label display name of the unit, may be null
     * @return the unit code, or C62 ("one") for unknown units
     */
    public static String code(String label) {
        return CODES.getOrDefault(label == null ? "" : label.trim(), "C62");
    }
}
