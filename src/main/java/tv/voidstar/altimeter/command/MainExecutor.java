package tv.voidstar.altimeter.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MainExecutor implements Command<CommandSource> {

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource src = context.getSource();
        src.sendMessage(Component.text("------ Altimeter command list ------", NamedTextColor.AQUA));
        src.sendMessage(Component.text("/altimeter clear [all|x.x.x.x] - clear accounts for all IPs, or for a specific IP.", NamedTextColor.GOLD));
        src.sendMessage(Component.text("/altimeter override <x.x.x.x> <limit> - sets a limit for a given IP. (ignores the global limit)", NamedTextColor.GOLD));
        src.sendMessage(Component.text("-------------------------------------", NamedTextColor.AQUA));
        return 1;
    }

}
