package de.fdhoho007

import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.commands.option
import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.subcommand
import dev.minn.jda.ktx.interactions.commands.updateCommands
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.jdabuilder.light
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant

class DiscordWhitelist: JavaPlugin() {

    private lateinit var jda: JDA

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        jda = light(config["bot_token"] as String, enableCoroutines = true)
        jda.updateCommands {
            slash("whitelist", "Interact with the minecraft server whitelist") {
                subcommand("request", "Request to be added to the server whitelist") {
                    option<String>("name", "Your minecraft player name", true)
                }
            }
            slash("whitelist-admin", "Whitelist admin") {
                subcommand("add", "Add somebody to the server whitelist") {
                    option<String>("name", "Minecraft player name", true)
                }
                subcommand("remove", "Remove somebody from the server whitelist") {
                    option<String>("name", "Minecraft player name", true)
                }
                subcommand("list", "List all players currently on the server whitelist")
            }
        }.queue()
        jda.onCommand("whitelist") { event ->
            val playerName = event.getOption<String>("name")!!
            val message = MessageCreate {
                embeds += getWhitelistRequestEmbed(event.user, playerName)
                components += row(
                    jda.button(ButtonStyle.SUCCESS, "Approve") { button ->
                        exec("whitelist add $playerName")
                        button.message.editMessageEmbeds(
                            getWhitelistRequestEmbed(event.user, playerName, approvedBy = button.user)
                        ).setComponents(row(
                            jda.button(ButtonStyle.DANGER, "Revoke") { button2 ->
                                exec("whitelist remove $playerName")
                                button2.message.editMessageEmbeds(
                                    getWhitelistRequestEmbed(event.user, playerName, approvedBy = button.user, revokedBy = button2.user)
                                ).setComponents().queue()
                                button.user.openPrivateChannel().queue {
                                    it.sendMessage("Your access to the minecraft server has been revoked.").queue()
                                }
                            }
                        )).queue()
                        button.user.openPrivateChannel().queue {
                            it.sendMessage("Your request to be added to the minecraft server whitelist was accepted.").queue()
                        }
                    },
                    jda.button(ButtonStyle.DANGER, "Deny") { button ->
                        button.message.editMessageEmbeds(
                            getWhitelistRequestEmbed(event.user, playerName, deniedBy = button.user)
                        ).setComponents().queue()
                        button.user.openPrivateChannel().queue {
                            it.sendMessage("Your request to be added to the minecraft server whitelist was denied.").queue()
                        }
                    }
                )
            }
            event.guild?.getTextChannelById(config["request_channel_id"] as Long)?.sendMessage(message)?.queue()
            event.reply("Your request to be added to the server whitelist has been submitted and will be checked by an admin soon.").setEphemeral(true).queue()
        }
        jda.onCommand("whitelist-admin") { event ->
            if(event.subcommandName == "list") {
                event.reply_("Currently whitelisted players: `${server.whitelistedPlayers.joinToString(", ") { it.name!! }}`").setEphemeral(true).queue()
            } else {
                val playerName = event.getOption<String>("name")!!
                if (event.subcommandName == "add") {
                    exec("whitelist add $playerName")
                    event.reply_("$playerName was added the server whitelist").queue()
                } else if (event.subcommandName == "remove") {
                    exec("whitelist remove $playerName")
                    event.reply_("$playerName was removed from the server whitelist").queue()
                }
            }
        }
    }

    override fun onDisable() {
        jda.shutdown()
    }

    private fun exec(cmd: String) {
        server.scheduler.runTask(this) { t ->
            server.dispatchCommand(server.consoleSender, cmd)
        }
    }

    private fun getWhitelistRequestEmbed(user: User, playerName: String, approvedBy: User? = null, deniedBy: User? = null, revokedBy: User? = null): MessageEmbed {
        return Embed {
            title = "Whitelist Request"
            description = "Somebody requested to be added to the server whitelist."
            field {
                name = "Discord User"
                value = user.asMention
                inline = true
            }
            field {
                name = "Player Name"
                value = playerName
                inline = true
            }
            if(approvedBy != null) {
                field {
                    name = "Approved by"
                    value = approvedBy.asMention
                    inline = true
                }
            }
            if(deniedBy != null) {
                field {
                    name = "Denied by"
                    value = deniedBy.asMention
                    inline = true
                }
            }
            if(revokedBy != null) {
                field {
                    name = "Revoked by"
                    value = revokedBy.asMention
                    inline = true
                }
            }
            timestamp = Instant.now()
            color = 0xFF0000
        }
    }

}