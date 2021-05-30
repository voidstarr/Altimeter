package tv.voidstar.altimeter.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import tv.voidstar.altimeter.AltimeterConfig;

import java.net.InetAddress;

public class OverrideExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        InetAddress ip = args.<InetAddress>getOne("ip").get();
        int limit = args.<Integer>getOne("limit").get();

        AltimeterConfig.addOverride(ip.getHostAddress(), limit);

        return CommandResult.success();
    }
}
