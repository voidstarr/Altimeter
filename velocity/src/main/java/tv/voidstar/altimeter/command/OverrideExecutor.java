package tv.voidstar.altimeter.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import tv.voidstar.altimeter.Altimeter;
import tv.voidstar.altimeter.AltimeterConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OverrideExecutor implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        if (!src.hasPermission("altimeter.override")) {
            src.sendMessage(Component.text("You don't have permission to do this.", NamedTextColor.RED));
            return 1;
        }

        InetAddress address;
        try {
            address = InetAddress.getByName(StringArgumentType.getString(context, "ip"));
        } catch (UnknownHostException var8) {
            src.sendMessage(Component.text("Invalid IP address!", NamedTextColor.RED));
            return 1;
        }

        String ip = address.getHostAddress();
        int limit = IntegerArgumentType.getInteger(context, "limit");

        Altimeter.getLogger().info("ip: " + ip + ", limit: " + limit);
        String verb = AltimeterConfig.setOverride(ip, limit) ? "updated" : "set";

        src.sendMessage(Component.text("[Altimeter] Successfully " + verb + " override limit to " + limit + " for " + ip, NamedTextColor.GREEN));
        return 1;
    }

}
