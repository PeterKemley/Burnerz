package burnerz;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Burnerz implements ModInitializer {

    public static final String MOD_ID = "burnerz";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("burnerz.json");

    private static BurnerzConfig config = new BurnerzConfig();

    @Override
    public void onInitialize() {

        // Load the JSON before Minecraft builds its fuel values.
        loadConfig();

        // Register our custom fuels.
        registerFuels();

        // Register /burnerz commands.
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) ->
                        registerCommands(dispatcher)
        );

        System.out.println("[Burnerz] Loaded successfully.");
        System.out.println(
                "[Burnerz] " + config.fuels.size()
                        + " custom fuel(s) configured."
        );
    }

    /*
     * Register custom fuels with Minecraft.
     */
    private static void registerFuels() {

        FuelValueEvents.BUILD.register((builder, context) -> {

            if (!config.enabled) {
                System.out.println(
                        "[Burnerz] Custom fuels are disabled."
                );
                return;
            }

            int registered = 0;

            for (Map.Entry<String, Integer> entry
                    : config.fuels.entrySet()) {

                String itemId = entry.getKey();
                Integer burnTime = entry.getValue();

                /*
                 * Ignore invalid burn times.
                 */
                if (burnTime == null || burnTime <= 0) {

                    System.err.println(
                            "[Burnerz] Invalid burn time for "
                                    + itemId
                                    + ": "
                                    + burnTime
                    );

                    continue;
                }

                try {

                    Identifier id =
                            Identifier.parse(itemId);

                    /*
                     * Check that the item actually exists.
                     */
                    if (!BuiltInRegistries.ITEM.containsKey(id)) {

                        System.err.println(
                                "[Burnerz] Unknown item: "
                                        + itemId
                        );

                        continue;
                    }

                    Item item =
                            BuiltInRegistries.ITEM.getValue(id);

                    if (item == null) {

                        System.err.println(
                                "[Burnerz] Could not resolve item: "
                                        + itemId
                        );

                        continue;
                    }

                    /*
                     * Add the item to Minecraft's fuel values.
                     */
                    builder.add(
                            item,
                            burnTime
                    );

                    registered++;

                    System.out.println(
                            "[Burnerz] Registered fuel: "
                                    + itemId
                                    + " -> "
                                    + burnTime
                                    + " ticks"
                    );

                } catch (Exception exception) {

                    System.err.println(
                            "[Burnerz] Invalid item ID: "
                                    + itemId
                    );
                }
            }

            System.out.println(
                    "[Burnerz] Registered "
                            + registered
                            + " custom fuel(s)."
            );
        });
    }

    /*
     * Register /burnerz commands.
     */
    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {

        dispatcher.register(
                Commands.literal("burnerz")

                        /*
                         * /burnerz help
                         */
                        .then(
                                Commands.literal("help")
                                        .executes(context -> {

                                            sendHelp(
                                                    context.getSource()
                                            );

                                            return 1;
                                        })
                        )

                        /*
                         * /burnerz reload
                         */
                        .then(
                                Commands.literal("reload")

                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_ADMIN
                                                        )
                                        )

                                        .executes(context -> {

                                            loadConfig();

                                            context.getSource()
                                                    .sendSuccess(
                                                            () -> Component.literal(
                                                                    "§a[Burnerz] Config reloaded. "
                                                                            + "§7Loaded "
                                                                            + config.fuels.size()
                                                                            + " fuel definition(s)."
                                                            ),
                                                            false
                                                    );

                                            return 1;
                                        })
                        )

                        /*
                         * /burnerz enable
                         */
                        .then(
                                Commands.literal("enable")

                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_ADMIN
                                                        )
                                        )

                                        .executes(context -> {

                                            config.enabled = true;

                                            saveConfig();

                                            context.getSource()
                                                    .sendSuccess(
                                                            () -> Component.literal(
                                                                    "§a[Burnerz] Custom fuels enabled."
                                                            ),
                                                            false
                                                    );

                                            return 1;
                                        })
                        )

                        /*
                         * /burnerz disable
                         */
                        .then(
                                Commands.literal("disable")

                                        .requires(source ->
                                                source.permissions()
                                                        .hasPermission(
                                                                Permissions.COMMANDS_ADMIN
                                                        )
                                        )

                                        .executes(context -> {

                                            config.enabled = false;

                                            saveConfig();

                                            context.getSource()
                                                    .sendSuccess(
                                                            () -> Component.literal(
                                                                    "§c[Burnerz] Custom fuels disabled."
                                                            ),
                                                            false
                                                    );

                                            return 1;
                                        })
                        )

                        /*
                         * /burnerz
                         */
                        .executes(context -> {

                            sendHelp(
                                    context.getSource()
                            );

                            return 1;
                        })
        );
    }

    /*
     * Help menu.
     */
    private static void sendHelp(
            CommandSourceStack source
    ) {

        source.sendSuccess(
                () -> Component.literal(
                        "§6§lBurnerz\n"
                                + "§7Custom furnace fuels\n\n"

                                + "§e/burnerz help"
                                + " §7- Show this menu\n"

                                + "§e/burnerz reload"
                                + " §7- Reload burnerz.json\n"

                                + "§e/burnerz enable"
                                + " §7- Enable custom fuels\n"

                                + "§e/burnerz disable"
                                + " §7- Disable custom fuels"
                ),
                false
        );
    }

    /*
     * Load burnerz.json.
     */
    public static void loadConfig() {

        try {

            if (!Files.exists(CONFIG_PATH)) {

                createDefaultConfig();

                return;
            }

            try (Reader reader =
                         Files.newBufferedReader(CONFIG_PATH)) {

                BurnerzConfig loaded =
                        GSON.fromJson(
                                reader,
                                BurnerzConfig.class
                        );

                if (loaded == null) {

                    System.err.println(
                            "[Burnerz] Config was empty. "
                                    + "Creating default config."
                    );

                    createDefaultConfig();

                    return;
                }

                if (loaded.fuels == null) {

                    loaded.fuels =
                            new LinkedHashMap<>();
                }

                config = loaded;
            }

            System.out.println(
                    "[Burnerz] Config loaded."
            );

            System.out.println(
                    "[Burnerz] Enabled: "
                            + config.enabled
            );

            System.out.println(
                    "[Burnerz] Fuel definitions: "
                            + config.fuels.size()
            );

        } catch (Exception exception) {

            System.err.println(
                    "[Burnerz] Failed to load burnerz.json"
            );

            exception.printStackTrace();
        }
    }

    /*
     * Create default config.
     */
    private static void createDefaultConfig() {

        config = new BurnerzConfig();

        /*
         * Burn times:
         *
         * 20 ticks   = 1 second
         * 200 ticks  = 10 seconds / 1 normal smelt
         * 1600 ticks = 80 seconds / coal equivalent
         */

        config.fuels.put(
                "minecraft:cobblestone",
                200
        );

        config.fuels.put(
                "minecraft:rotten_flesh",
                400
        );

        config.fuels.put(
                "minecraft:poisonous_potato",
                100
        );

        saveConfig();

        System.out.println(
                "[Burnerz] Created config/burnerz.json"
        );
    }

    /*
     * Save config.
     */
    public static void saveConfig() {

        try {

            Files.createDirectories(
                    CONFIG_PATH.getParent()
            );

            try (Writer writer =
                         Files.newBufferedWriter(CONFIG_PATH)) {

                GSON.toJson(
                        config,
                        writer
                );
            }

        } catch (IOException exception) {

            System.err.println(
                    "[Burnerz] Failed to save burnerz.json"
            );

            exception.printStackTrace();
        }
    }

    public static boolean isEnabled() {

        return config.enabled;
    }

    public static Integer getBurnTime(
            String itemId
    ) {

        if (!config.enabled) {
            return null;
        }

        return config.fuels.get(itemId);
    }

    public static Map<String, Integer> getFuels() {

        return config.fuels;
    }

    /*
     * JSON structure.
     */
    public static class BurnerzConfig {

        public boolean enabled = true;

        public Map<String, Integer> fuels =
                new LinkedHashMap<>();
    }
}