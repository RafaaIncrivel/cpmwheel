package dev.cpmwheel;

import java.util.ArrayList;
import java.util.List;

import com.tom.cpm.shared.parts.anim.menu.AbstractDropdownButtonData;
import com.tom.cpm.shared.parts.anim.menu.BoolParameterToggleButtonData;
import com.tom.cpm.shared.parts.anim.menu.CustomPoseGestureButtonData;

import net.minecraft.text.Text;

/** Uma fatia da roda. */
public final class WheelItem {
    public enum Kind { TOGGLE, GESTURE, POSE, DROPDOWN, OPTION, RESET, BACK }

    public final Kind kind;
    public final String label;
    /** Nome usado para procurar o icone (arquivo PNG com esse nome). */
    public final String iconKey;

    private BoolParameterToggleButtonData toggle;
    private AbstractDropdownButtonData dropdown;
    private CustomPoseGestureButtonData gesture;
    private String optionValue = "";

    private WheelItem(Kind kind, String label, String iconKey) {
        this.kind = kind;
        this.label = label;
        this.iconKey = iconKey;
    }

    private static String nameOf(String s) {
        return s == null ? "?" : s;
    }

    private static String tr(String key) {
        return Text.translatable(key).getString();
    }

    public static WheelItem ofToggle(BoolParameterToggleButtonData d) {
        WheelItem w = new WheelItem(Kind.TOGGLE, nameOf(d.getName()), nameOf(d.getName()));
        w.toggle = d;
        return w;
    }

    public static WheelItem ofDropdown(AbstractDropdownButtonData d) {
        WheelItem w = new WheelItem(Kind.DROPDOWN, nameOf(d.getName()), nameOf(d.getName()));
        w.dropdown = d;
        return w;
    }

    public static WheelItem ofGesture(CustomPoseGestureButtonData d) {
        WheelItem w = new WheelItem(d.isPose() ? Kind.POSE : Kind.GESTURE, nameOf(d.getName()), nameOf(d.getName()));
        w.gesture = d;
        return w;
    }

    public static WheelItem reset() {
        return new WheelItem(Kind.RESET, tr("cpm_wheel.reset"), "_reset");
    }

    public static WheelItem back() {
        return new WheelItem(Kind.BACK, tr("cpm_wheel.back"), "_back");
    }

    /** Opcoes de um dropdown, como sub-roda. */
    public List<WheelItem> children() {
        List<WheelItem> out = new ArrayList<>();
        if (dropdown == null) {
            return out;
        }
        for (String opt : dropdown.getActiveOptions()) {
            String value = opt == null ? "" : opt;
            String shown = value.isEmpty() ? tr("cpm_wheel.none") : value;
            WheelItem w = new WheelItem(Kind.OPTION, shown, label + "_" + value);
            w.dropdown = dropdown;
            w.optionValue = value;
            out.add(w);
        }
        return out;
    }

    /** Executa a acao do item (toggle, gesto, escolher opcao). */
    public void run() {
        switch (kind) {
            case TOGGLE -> toggle.toggle();
            case GESTURE, POSE -> gesture.activate();
            case OPTION -> dropdown.set(optionValue);
            default -> {
            }
        }
    }

    public boolean isActive() {
        try {
            return switch (kind) {
                case TOGGLE -> toggle.getValue();
                case OPTION -> optionValue.equals(dropdown.get());
                case DROPDOWN -> {
                    String v = dropdown.get();
                    yield v != null && !v.isEmpty();
                }
                default -> false;
            };
        } catch (Throwable t) {
            return false;
        }
    }

    public String subtitle() {
        try {
            return switch (kind) {
                case TOGGLE -> tr(toggle.getValue() ? "cpm_wheel.on" : "cpm_wheel.off");
                case DROPDOWN -> {
                    String v = dropdown.get();
                    yield (v == null || v.isEmpty()) ? tr("cpm_wheel.none") : v;
                }
                default -> "";
            };
        } catch (Throwable t) {
            return "";
        }
    }
}
