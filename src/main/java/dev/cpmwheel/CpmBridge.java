package dev.cpmwheel;

import java.util.ArrayList;
import java.util.List;

import com.tom.cpm.shared.MinecraftClientAccess;
import com.tom.cpm.shared.animation.AnimationEngine;
import com.tom.cpm.shared.config.Player;
import com.tom.cpm.shared.definition.ModelDefinition;
import com.tom.cpm.shared.parts.anim.menu.AbstractDropdownButtonData;
import com.tom.cpm.shared.parts.anim.menu.AbstractGestureButtonData;
import com.tom.cpm.shared.parts.anim.menu.BoolParameterToggleButtonData;
import com.tom.cpm.shared.parts.anim.menu.CustomPoseGestureButtonData;

/**
 * Unico lugar que fala com as classes INTERNAS do CPM (testado com 0.6.27a).
 * Se o CPM mudar em uma versao futura, e aqui que o ajuste acontece.
 */
public final class CpmBridge {
    private CpmBridge() {
    }

    private static ModelDefinition currentDefinition() {
        Player<?> p = MinecraftClientAccess.get().getCurrentClientPlayer();
        if (p == null) {
            return null;
        }
        return p.getModelDefinition();
    }

    /** Mesmo filtro que o gesture menu do CPM usa. Sliders (value layers) ficam de fora. */
    private static boolean visible(AbstractGestureButtonData d) {
        return d != null && !d.isProperty() && d.canShow();
    }

    public static List<WheelItem> readItems() {
        List<WheelItem> out = new ArrayList<>();
        ModelDefinition def = currentDefinition();
        if (def == null) {
            return out;
        }
        for (AbstractGestureButtonData d : def.getAnimations().getNamedActions()) {
            if (!visible(d)) {
                continue;
            }
            if (d instanceof BoolParameterToggleButtonData t) {
                out.add(WheelItem.ofToggle(t));
            } else if (d instanceof AbstractDropdownButtonData dd) {
                out.add(WheelItem.ofDropdown(dd));
            } else if (d instanceof CustomPoseGestureButtonData g) {
                out.add(WheelItem.ofGesture(g));
            }
        }
        return out;
    }

    /** Desliga todos os toggles, volta dropdowns para "nenhum" e limpa gesto e pose. */
    public static void resetAll() {
        ModelDefinition def = currentDefinition();
        if (def == null) {
            return;
        }
        AnimationEngine engine = MinecraftClientAccess.get().getPlayerRenderManager().getAnimationEngine();
        engine.clearGesture(def);
        engine.clearCustomPose(def);
        for (AbstractGestureButtonData d : def.getAnimations().getNamedActions()) {
            if (!visible(d)) {
                continue;
            }
            if (d instanceof BoolParameterToggleButtonData t) {
                if (t.getValue()) {
                    t.toggle();
                }
            } else if (d instanceof AbstractDropdownButtonData dd) {
                dd.set("");
            }
        }
    }
}
