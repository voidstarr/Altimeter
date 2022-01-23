package tv.voidstar.altimeter;

import com.google.inject.Inject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import tv.voidstar.altimeter.command.ClearExecutor;
import tv.voidstar.altimeter.command.MainExecutor;
import tv.voidstar.altimeter.command.OverrideExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

@Plugin(
        id = "altimeter",
        name = "Altimeter",
        version = "1.0",
        description = "Limit number of accounts joining per IP address",
        authors = {"ZeHeisenberg"}
)
public class Altimeter {

    private static Altimeter plugin;

    @Inject
    @DataDirectory
    private Path defaultConfigDir;

    @Inject
    private Logger logger;

    @Inject
    ProxyServer server;

    private static Altimeter getInstance() {
        return plugin;
    }

    public static Logger getLogger() {
        return plugin.logger;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) throws IOException {
        plugin = this;

        File rootDir = defaultConfigDir.toFile(); //new File(defaultConfigDir.toFile(), "altimeter");
        if (!rootDir.exists()) {
            if (!rootDir.mkdirs()) {
                Altimeter.getLogger().error("Unable to create root config dir");
            }
        }

        logger.info("Altimeter is starting");

        this.loadLibraries();

        AltimeterConfig.init(rootDir);
        AltimeterData.init(rootDir);

        //AltimeterConfig.load();
        AltimeterData.load();

        registerCommands();

        this.server.getScheduler()
                .buildTask(this, () -> AltimeterData.checkAndClearAccounts(Instant.now()))
                .repeat(AltimeterConfig.getCheckIntervalValue(), AltimeterConfig.getCheckIntervalUnit())
                .schedule();
    }

    @Subscribe
    public void onReload(ProxyReloadEvent event) {
        AltimeterConfig.load();
        AltimeterData.reload();
        getLogger().info("Altimeter reloaded");
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        String ip = event.getPlayer().getRemoteAddress().getAddress().getHostAddress();
        Altimeter.getLogger().info("{} logging in from {}", player, ip);
        if (!AltimeterData.canLogIn(player, ip)) {
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(
                    AltimeterConfig.getDisconnectMessage(),
                    NamedTextColor.RED
            )));
        }
    }

    @Subscribe
    public void onStop(ProxyShutdownEvent event) {
        AltimeterData.save();
        AltimeterConfig.save();
    }

    private void loadLibraries() {
        try {
            logger.info("Loading libraries");
            Path libraries = this.defaultConfigDir.resolve("libraries");
            Files.createDirectories(libraries);

            try (Stream<Path> stream = Files.walk(libraries)) {
                stream.forEach(filePath -> {
                    logger.info("Adding library file " + filePath.getFileName() + " to the classpath");
                    server.getPluginManager().addToClasspath(this, filePath);
                });
            }
        } catch (Exception e) {
            logger.error("Could not load libraries.");
        }
    }

    private void registerCommands() {
        LiteralCommandNode<CommandSource> altimeterCommand = LiteralArgumentBuilder
                .<CommandSource>literal("altimeter")
                .executes(new MainExecutor())
                .then(LiteralArgumentBuilder.<CommandSource>literal("clear")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("target", StringArgumentType.word())
                                .executes(new ClearExecutor())
                        )
                ).then(LiteralArgumentBuilder.<CommandSource>literal("override")
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("ip", StringArgumentType.word())
                                .then(RequiredArgumentBuilder.<CommandSource, Integer>argument("limit", IntegerArgumentType.integer())
                                        .executes(new OverrideExecutor())
                                )
                        )
                )
                .build();

        BrigadierCommand altimeter = new BrigadierCommand(altimeterCommand);
        server.getCommandManager().register(altimeter);
    }
}
