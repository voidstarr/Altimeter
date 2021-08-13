package tv.voidstar.altimeter.command;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tv.voidstar.altimeter.AltimeterData;

public class ClearExecutor implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        String target = StringArgumentType.getString(context, "target");
        if (target.equals("all")) {
            if (!src.hasPermission("altimeter.clear.all")) {
                src.sendMessage(Component.text("[Altimeter] You don't have permission to do that.", NamedTextColor.RED));
                return 1;
            }
        } else if (InetAddresses.isInetAddress(target)) {
            if (!src.hasPermission("altimeter.clear.ip")) {
                src.sendMessage(Component.text("[Altimeter] You don't have permission to do that.", NamedTextColor.RED));
                return 1;
            }
        } else {
            src.sendMessage(Component.text("[Altimeter] You must supply 'all' or an IP address.", NamedTextColor.RED));
            return 1;
        }

        if (AltimeterData.clear(target)) {
            src.sendMessage(Component.text("[Altimeter] Successfully cleared account entries for " + target, NamedTextColor.GREEN));
        } else {
            src.sendMessage(Component.text("[Altimeter] Unable to clear account entries for " + target, NamedTextColor.RED));
        }
        return 1;
    }
}
