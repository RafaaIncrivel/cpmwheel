package dev.cpmwheel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

/** Carrega icones PNG de config/cpm_wheel/icons/ (um arquivo por item, com o mesmo nome do toggle). */
public final class IconCache {
    public static final class Icon {
        public final Identifier id;
        public final int w;
        public final int h;

        Icon(Identifier id, int w, int h) {
            this.id = id;
            this.w = w;
            this.h = h;
        }
    }

    private static final Map<String, Icon> CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static int counter = 0;

    private IconCache() {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("cpm_wheel").resolve("icons");
    }

    public static void ensureDir() {
        try {
            Path d = dir();
            Files.createDirectories(d);
            Path readme = d.resolve("LEIA-ME.txt");
            if (!Files.exists(readme)) {
                String txt = "Coloque aqui imagens PNG para servir de icone na roda.\n"
                        + "O nome do arquivo deve ser igual ao nome do toggle no CPM.\n"
                        + "Exemplo: toggle \"Oculos\" -> Oculos.png\n"
                        + "Para opcoes de dropdown use: NomeDoDropdown_NomeDaOpcao.png (ex: Estilos_Punk.png)\n"
                        + "Caracteres proibidos em nome de arquivo (/ \\ : * ? \" < > |) viram _\n"
                        + "Botao de reset: _reset.png   Botao de voltar: _back.png\n"
                        + "Imagens quadradas, de 64x64 ou 128x128, funcionam melhor.\n";
                Files.write(readme, txt.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            CpmWheelClient.LOG.warn("Nao consegui criar a pasta de icones: {}", e.toString());
        }
    }

    /** Libera tudo para que icones novos sejam lidos na proxima vez que a roda abrir. */
    public static void clear() {
        TextureManager tm = MinecraftClient.getInstance().getTextureManager();
        for (Icon i : CACHE.values()) {
            tm.destroyTexture(i.id);
        }
        CACHE.clear();
        MISSING.clear();
    }

    private static String safeFile(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static Icon get(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        Icon cached = CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        if (MISSING.contains(name)) {
            return null;
        }
        Path file = dir().resolve(safeFile(name) + ".png");
        if (!Files.isRegularFile(file)) {
            MISSING.add(name);
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            int w = img.getWidth();
            int h = img.getHeight();
            NativeImageBackedTexture tex = new NativeImageBackedTexture(img);
            Identifier id = new Identifier("cpm_wheel", "icons/i" + (counter++));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            Icon icon = new Icon(id, w, h);
            CACHE.put(name, icon);
            return icon;
        } catch (Throwable t) {
            CpmWheelClient.LOG.warn("Icone invalido '{}': {}", file.getFileName(), t.toString());
            MISSING.add(name);
            return null;
        }
    }
}
