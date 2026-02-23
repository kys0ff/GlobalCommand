package off.kys.gcmd

import kotlin.system.exitProcess

// ----------------------------
// Global Constants
// ----------------------------
const val GCMD_VERSION = "0.0.1"
const val BIN_PATH = "/usr/bin/"
const val LIB_PATH = "/usr/lib/"

// ----------------------------
// Main Entry Point
// ----------------------------
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
// Command Handlers
// ----------------------------
private fun handleInstallCommand(positionals: List<String>, flags: List<String>) {
    if (!isRoot()) {
        printError("gcmd requires root privileges to install packages. Try prefixing with 'sudo'.")
        exitProcess(1)
    }

    val pkgPath = positionals.getOrNull(1)
    if (pkgPath.isNullOrBlank()) {
        printError("No package path provided.")
        println("${YELLOW}Tip: Use 'gcmd help' to see usage examples.${RESET}")
        exitProcess(1)
    }

    val pkgFile = pkgPath.toFile()
    if (!pkgFile.exists()) {
        printError("Package file or directory not found: $pkgPath")
        exitProcess(1)
    }

    val deleteOriginal = flags.contains("--delete-original") || flags.contains("-d")
    println("${CYAN}●${RESET} Installing package: ${BOLD}$pkgPath${RESET}")

    when {
        pkgFile.isFile() && (pkgFile.isRunnable() || pkgFile.extension in listOf("sh", "bin", "run")) ->
            installFile(pkgFile, deleteOriginal)

        pkgFile.isDirectory() ->
            installDirectory(pkgFile, positionals.getOrNull(2) ?: pkgFile.name, deleteOriginal)

        else -> {
            printError("The provided path is not a valid executable file or directory: $pkgPath")
            exitProcess(1)
        }
    }
}

private fun installFile(file: File, deleteOriginal: Boolean) {
    try {
        if (deleteOriginal) {
            println("${YELLOW}!${RESET} Moving original file...")
            file.moveTo(BIN_PATH)
        } else {
            println("${CYAN}●${RESET} Copying file...")
            file.copyTo(BIN_PATH)
        }
        println("${GREEN}✔${RESET} Package installed successfully to ${BOLD}$BIN_PATH${RESET}")
    } catch (e: Exception) {
        printError("Installation failed: ${e.message}")
        exitProcess(1)
    }
}

private fun installDirectory(dir: File, exeNameOverride: String, deleteOriginal: Boolean) {
    try {
        if (deleteOriginal) {
            println("${YELLOW}!${RESET} Moving original directory...")
            dir.moveTo(LIB_PATH)
        } else {
            println("${CYAN}●${RESET} Copying directory...")
            dir.copyTo(LIB_PATH + dir.name)
        }

        println("${GREEN}✔${RESET} Directory installed successfully to ${BOLD}$LIB_PATH${dir.name}${RESET}")

        // Linking executable
        println("${CYAN}●${RESET} Linking executable to ${BOLD}$BIN_PATH$exeNameOverride${RESET}")
        val linkFile = File(BIN_PATH, exeNameOverride)
        if (linkFile.exists()) {
            printError("A file named '$exeNameOverride' already exists in $BIN_PATH.")
            println("Please remove it before reinstalling, or provide a custom executable name.")
            exitProcess(1)
        }

        val exeFile = File(
            LIB_PATH,
            exeNameOverride
        ).listFiles()?.find { it.isFile() && it.isRunnable() && it.name == exeNameOverride }
            ?: run {
                printError("No executable named '$exeNameOverride' found in directory: $LIB_PATH$exeNameOverride")
                exitProcess(1)
            }

        exeFile.createSymlinkTo(linkFile.absolutePath)
        println("${GREEN}✔${RESET} Symlink created successfully.")

        if (!exeFile.canExecute()) {
            println("${YELLOW}!${RESET} Setting executable permissions for ${BOLD}$exeFile${RESET}")
            exeFile.setExecutable(true)
        }

        println("${GREEN}✔${RESET} Installation complete!")
    } catch (e: Exception) {
        printError("Installation failed: ${e.message}")
        exitProcess(1)
    }
}

// ----------------------------
// Utility Functions
// ----------------------------
private fun printError(message: String) {
    println("${RED}${BOLD}error:${RESET} $message")
}