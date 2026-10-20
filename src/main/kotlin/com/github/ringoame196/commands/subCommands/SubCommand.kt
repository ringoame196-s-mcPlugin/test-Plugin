package com.github.ringoame196.commands.subCommands

import org.bukkit.command.CommandSender

interface SubCommand {
    val name: String

    fun execute(
        sender: CommandSender,
        args: Array<out String>,
    )

    fun tabComplete(
        sender: CommandSender,
        args: Array<out String>,
    ): List<String> = emptyList()
}
