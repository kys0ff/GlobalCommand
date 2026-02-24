package off.kys.gcmd

import off.kys.gcmd.cli.handleInstallCommand
import off.kys.gcmd.common.CYAN
import off.kys.gcmd.common.GCMD_VERSION
import off.kys.gcmd.common.GREEN
import off.kys.gcmd.common.RESET
import off.kys.gcmd.common.YELLOW
import off.kys.gcmd.common.printError
import off.kys.gcmd.common.printUsage
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    val flags = args.filter { it.startsWith("-") }
    val positionals = args.filter { !it.startsWith("-") }

    when (val command = args[0]) {
        "help", "-h", "--help" -> printUsage()
        "version", "-v", "--version" -> println("${CYAN}gcmd${RESET} version ${GREEN}${GCMD_VERSION}${RESET}")
        "install" -> handleInstallCommand(positionals, flags)
        else -> {
            printError("Unknown command: '$command'")
            println("${YELLOW}Tip: Use 'gcmd help' to see available commands.${RESET}")
            exitProcess(1)
        }
    }
}