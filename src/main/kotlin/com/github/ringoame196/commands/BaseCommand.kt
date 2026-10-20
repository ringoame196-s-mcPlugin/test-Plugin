package com.github.ringoame196.commands

import com.github.ringoame196.commands.subCommands.SubCommand
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

abstract class BaseCommand :
    CommandExecutor,
    TabCompleter {
    protected abstract val subCommands: Map<String, SubCommand>

    override fun onCommand(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        val subCommandName = args.firstOrNull() ?: return false
        val subCommand = subCommands[subCommandName] ?: return false

        // サブコマンド実行（1番目の引数を除いた配列を渡す）
        val subArgs = args.drop(1).toTypedArray()
        subCommand.execute(sender, subArgs)
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: org.bukkit.command.Command,
        label: String,
        args: Array<out String>,
    ): List<String?>? {
        if (args.isEmpty()) return emptyList()

        // 1. 生の候補リストを取得（1番目：サブコマンド一覧 / 2番目以降：各サブコマンドに委任）
        val rawCandidates =
            if (args.size == 1) {
                subCommands.keys.toList()
            } else {
                val subCommand = subCommands[args[0]] ?: return emptyList()
                val subArgs = args.drop(1).toTypedArray()
                subCommand.tabComplete(sender, subArgs)
            }

        // 2. 現在入力中の末尾文字に対して共通で一括フィルターを適用
        val currentInput = args.last()
        return rawCandidates.filter { it.startsWith(currentInput, ignoreCase = true) }
    }
}
