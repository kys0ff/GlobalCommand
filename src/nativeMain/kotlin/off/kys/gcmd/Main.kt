package off.kys.gcmd

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
        "version", "-v", "--version" -> println("${CYAN}gcmd${RESET} version ${GREEN}$GCMD_VERSION${RESET}")
        "install" -> handleInstallCommand(positionals, flags)
        else -> {
            printError("Unknown command: '$command'")
            println("${YELLOW}Tip: Use 'gcmd help' to see available commands.${RESET}")
            exitProcess(1)
        }
    }
}

// ----------------------------
// Utility Functions
// ----------------------------
fun printError(message: String) {
    println("${RED}${BOLD}error:${RESET} $message")
}