package com.github.jcbsm.bridge.discord.commands;

import com.github.jcbsm.bridge.Bridge;
import com.github.jcbsm.bridge.DatabaseClient;
import com.github.jcbsm.bridge.discord.ApplicationCommand;
import com.github.jcbsm.bridge.util.MojangRequest;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// TODO: sanamorii - add option to remove username (subcommands)
public class WhitelistCommand extends ApplicationCommand {

    private final DatabaseClient database = DatabaseClient.getDatabase();
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    public WhitelistCommand() {
        super("whitelist", "Add your Minecraft account to the whitelist");
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOption(OptionType.STRING, "username", "The Minecraft username you wish to whitelist.");
    }

    @Override
    public void run(SlashCommandInteractionEvent event) {

        // Get options
        long userId = event.getUser().getIdLong();
        OptionMapping option = event.getOption("username");

        if (option == null) {
            event.reply("You currently have \\_\\_\\_\\_\\_\\_ whitelisted on the server").setEphemeral(true).queue();
            return;
        }

        // Wait
        event.deferReply().setEphemeral(true).queue();

        // Get usernames & uuid
        String username = option.getAsString();
        MojangRequest mojangRequest = new MojangRequest();

        try{
            String uuid = mojangRequest.usernameToUUID(username);

            if (uuid == null) {
                event.getHook().sendMessage("No user exists").queue();
                return;
            }

            System.out.println("User " + event.getUser().getGlobalName() + " executed /whitelist from Discord.");


            int res = database.linkAccount(event.getUser(), uuid, username);
            if (res == 1) {
                database.unlinkAccount(uuid);
                Bukkit.getScheduler().runTask(Bridge.getPlugin(), () -> Bukkit.getServer().dispatchCommand(
                        Bukkit.getServer().getConsoleSender(),
                        "whitelist remove " + username
                ));
                event.getHook().sendMessage("Deleted: `" + username + "`").queue();
            } else { // Add name to whitelist

                Bukkit.getScheduler().runTask(Bridge.getPlugin(), () -> Bukkit.getServer().dispatchCommand(
                        Bukkit.getServer().getConsoleSender(),
                        "whitelist add " + username
                ));

                event.getHook().sendMessage("Added: `" + username + "`").queue();
                this.logger.info(String.format("User '%s' has registered their account '%s' to the whitelist",
                        event.getUser().getName(), username));
            }

        }
        catch (Exception e){
            e.printStackTrace();
            event.getHook().sendMessage("An unexpected error has occurred.").queue();
        }
    }
}
