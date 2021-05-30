package tv.voidstar.altimeter.command;

import com.google.common.net.InetAddresses;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import tv.voidstar.altimeter.AltimeterData;

import java.util.Locale;
import java.util.Optional;

public class ClearExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Optional<String> targetOpt = args.getOne("target");

        if (targetOpt.isPresent()) {
            String target = targetOpt.get().toLowerCase(Locale.ROOT);
            if (target.equals("all")) {
                if (!src.hasPermission("altimeter.clear.all")) {
                    src.sendMessage(Text.of("You don't have permission to do that."));
                    return CommandResult.success();
                }
                AltimeterData.clear(target);
            } else if (InetAddresses.isInetAddress(target)) {
                if (!src.hasPermission("altimeter.clear.ip")) {
                    src.sendMessage(Text.of("You don't have permission to do that."));
                    return CommandResult.success();
                }
                AltimeterData.clear(target);
            } else {
                src.sendMessage(Text.of("You must supply 'all' or an IP address."));
                return CommandResult.success();
            }
        } else {
            src.sendMessage(Text.of("You must supply 'all' or an IP address."));
            return CommandResult.success();
        }

        return CommandResult.success();
    }
}
