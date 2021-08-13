package tv.voidstar.altimeter.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;

public class MainExecutor implements CommandExecutor {
    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        List<Text> contents = new ArrayList<>();

        contents.add(Text.of("/altimeter clear [all|x.x.x.x] - clear accounts for all IPs, or for a specific IP."));
        contents.add(Text.of("/altimeter override <x.x.x.x> <limit> - sets a limit for a given IP. (ignores the global limit)"));

        PaginationList.builder()
                .title(Text.of("Altimeter command list"))
                .contents(contents)
                .padding(Text.of("-"))
                .sendTo(src);

        return CommandResult.success();
    }
}
