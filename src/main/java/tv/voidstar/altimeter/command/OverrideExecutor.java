package tv.voidstar.altimeter.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import tv.voidstar.altimeter.AltimeterConfig;

import java.net.InetAddress;

public class OverrideExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        String ip = args.<InetAddress>getOne("ip").get().getHostAddress();
        int limit = args.<Integer>getOne("limit").get();

        String verb = AltimeterConfig.setOverride(ip, limit) ? "updated" : "set";

        src.sendMessage(Text.of("[Altimeter] Successfully ", verb, " override limit to ", limit, " for ", ip));

        return CommandResult.success();
    }
}
