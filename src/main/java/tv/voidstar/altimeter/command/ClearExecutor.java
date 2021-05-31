package tv.voidstar.altimeter.command;

import com.google.common.net.InetAddresses;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import tv.voidstar.altimeter.AltimeterData;

public class ClearExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        String target = args.<String>getOne("target").get();

        if (target.equals("all")) {
            if (!src.hasPermission("altimeter.clear.all")) {
                src.sendMessage(Text.of("[Altimeter] You don't have permission to do that."));
                return CommandResult.success();
            }
        } else if (InetAddresses.isInetAddress(target)) {
            if (!src.hasPermission("altimeter.clear.ip")) {
                src.sendMessage(Text.of("[Altimeter] You don't have permission to do that."));
                return CommandResult.success();
            }
        } else {
            src.sendMessage(Text.of("[Altimeter] You must supply 'all' or an IP address."));
            return CommandResult.success();
        }

        if (AltimeterData.clear(target)) {
            src.sendMessage(Text.of("[Altimeter] Successfully cleared account entries for ", target));
        } else {
            src.sendMessage(Text.of("[Altimeter] Unable to clear account entries for ", target));
        }

        return CommandResult.success();
    }
}
