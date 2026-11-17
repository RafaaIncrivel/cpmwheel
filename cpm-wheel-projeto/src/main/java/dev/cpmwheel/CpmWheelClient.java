package dev.cpmwheel;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class CpmWheelClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("CPM Wheel");

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        IconCache.ensureDir();
        // A tecla G ja e do gesture menu do CPM, entao o padrao aqui e V (troque em Controles).
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cpm_wheel.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.cpm_wheel"));
        ClientTickEvents.END_CLIENT_TICK.register(CpmWheelClient::onTick);
    }

    private static void onTick(MinecraftClient client) {
        while (openKey.wasPressed()) {
            if (client.currentScreen != null || client.player == null) {
                continue;
            }
            open(client);
        }
    }

    private static void open(MinecraftClient client) {
        try {
            IconCache.clear();
            List<WheelItem> items = CpmBridge.readItems();
            if (items.isEmpty()) {
                client.player.sendMessage(Text.translatable("cpm_wheel.nothing"), true);
                return;
            }
            client.setScreen(new WheelScreen(openKey, items));
        } catch (Throwable t) {
            LOG.error("Erro ao abrir a roda (versao do CPM incompativel?)", t);
            client.player.sendMessage(Text.translatable("cpm_wheel.error"), false);
        }
    }
}
