package com.github.frcsty.frozenjoin.load

import com.github.frcsty.frozenjoin.FrozenJoinPlugin
import com.github.frcsty.frozenjoin.`object`.FormatManager
import com.github.frcsty.frozenjoin.action.ActionHandler
import com.github.frcsty.frozenjoin.command.HelpCommand
import com.github.frcsty.frozenjoin.command.InfoCommand
import com.github.frcsty.frozenjoin.command.MotdCommand
import com.github.frcsty.frozenjoin.command.ReloadCommand
import com.github.frcsty.frozenjoin.extension.color
import com.github.frcsty.frozenjoin.listener.PlayerJoinListener
import com.github.frcsty.frozenjoin.listener.PlayerQuitListener
import me.mattstudios.mf.base.CommandManager
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit.getServer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.Listener
import java.util.logging.Level

class Loader(private val plugin: FrozenJoinPlugin) {

    val actionHandler: ActionHandler = ActionHandler(plugin)
    val formatManager = FormatManager(plugin)

    fun initialize() {
        plugin.saveDefaultConfig()

        if (Settings.METRICS) {
            val metrics = Metrics(plugin, 6743)
            if (metrics.isEnabled) {
                Settings.LOGGER.log(Level.INFO, "bStats Metrics Running!")
            }
        }

        if (Settings.HEX_USE) {
            logInfo("Hex support enabled! (#hex)")
        }

        val manager = CommandManager(plugin)
        manager.register(
                HelpCommand(plugin),
                InfoCommand(plugin),
                MotdCommand(this),
                ReloadCommand(plugin)
        )

        formatManager.setFormats()

        val messages = plugin.config.getConfigurationSection("messages")

        if (messages == null) {
            logError("Configuration section 'messages' not found!")
        }

        registerMessages(manager, messages)
        registerListeners(PlayerJoinListener(this), PlayerQuitListener(this))
    }

    private fun registerMessages(manager: CommandManager, messages: ConfigurationSection?) {
        val handler = manager.messageHandler
        val console = messages?.getString("player-only-message")
                ?: "&8[&bFrozenJoin&8] &cThis command can not be executed through console!"
        val permission = messages?.getString("deny-message")
                ?: "&8[&bFrozenJoin&8] &7You do not have permission to execute this."
        val exists = messages?.getString("unknown-command-message")
                ?: "&8[&bFrozenJoin&8] &7Executed command is invalid!"
        val usage = messages?.getString("usage-message")
                ?: "&8[&bFrozenJoin&8] &7Incorrect usage for specified command!"
        with(handler) {
            register("cmd.no.console") { sender: CommandSender ->
                sender.sendMessage(console.color())
            }
            register("cmd.no.permission") { sender: CommandSender ->
                sender.sendMessage(permission.color())
            }
            register("cmd.no.exists") { sender: CommandSender ->
                sender.sendMessage(exists.color())
            }
            register("cmd.wrong.usage") { sender: CommandSender ->
                sender.sendMessage(usage.color())
            }
        }
    }

    private fun registerListeners(vararg listeners: Listener) {
        listeners.forEach { listener: Listener? ->
            if (listener != null)
                getServer().pluginManager.registerEvents(listener, plugin)
        }
    }

    fun terminate() {
        plugin.reloadConfig()
    }
}