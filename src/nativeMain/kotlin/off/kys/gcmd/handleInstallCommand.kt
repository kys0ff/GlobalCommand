package off.kys.gcmd

import kotlin.system.exitProcess

fun handleInstallCommand(positionals: List<String>, flags: List<String>) {
    if (!isRoot()) {
        printError("Root privileges are required to install packages.")
        println("${YELLOW}Tip:${RESET} Run with ${BOLD}sudo${RESET} (e.g. ${BOLD}sudo gcmd install <path>${RESET})")
        exitProcess(1)
    }

    val pkgPath = positionals.getOrNull(1)
    if (pkgPath.isNullOrBlank()) {
        printError("No package path specified.")
        println("${YELLOW}Usage:${RESET} ${BOLD}gcmd install <path> [options]${RESET}")
        println("${YELLOW}Help:${RESET} Run ${BOLD}gcmd help${RESET} for examples.")
        exitProcess(1)
    }

    val pkgFile = pkgPath.toFile()
    if (!pkgFile.exists()) {
        printError("Path not found: ${BOLD}$pkgPath${RESET}")
        println("${YELLOW}Hint:${RESET} Verify the file or directory exists and try again.")
        exitProcess(1)
    }

    val deleteOriginal = flags.contains("--delete-original") || flags.contains("-d")

    println("${CYAN}▶ Starting installation...${RESET}")
    println("${CYAN}•${RESET} Source: ${BOLD}$pkgPath${RESET}")
    println("${CYAN}•${RESET} Mode: ${if (deleteOriginal) "Move (delete original)" else "Copy"}")

    when {
        pkgFile.isFile() && (pkgFile.isRunnable() || pkgFile.extension in supportedExecutables()) -> {
            if (positionals.getOrNull(2) != null)
                println("${YELLOW}• Warning:${RESET} Executable name argument will be ignored for single file installations.")
            installFile(
                pkgFile,
                deleteOriginal
            )
        }

        pkgFile.isDirectory() ->
            installDirectory(
                pkgFile,
                positionals.getOrNull(2) ?: pkgFile.name,
                deleteOriginal
            )

        else -> {
            printError("Unsupported package type.")
            println("${YELLOW}Expected:${RESET} Executable file or directory.")
            exitProcess(1)
        }
    }
}

private fun installFile(exeFile: File, deleteOriginal: Boolean) {
    try {
        val destination = File(BIN_PATH, exeFile.name)

        if (destination.exists()) {
            printError("Executable '${exeFile.name}' already exists in $BIN_PATH.")
            println("${YELLOW}Fix:${RESET} Remove it manually before reinstalling.")
            exitProcess(1)
        }

        if (deleteOriginal) {
            println("${YELLOW}• Moving executable to ${BOLD}$BIN_PATH${RESET} ...")
            exeFile.moveTo(BIN_PATH)
        } else {
            println("${CYAN}• Copying executable to ${BOLD}$BIN_PATH${RESET} ...")
            exeFile.copyTo(BIN_PATH)
        }

        println("${YELLOW}• Granting execute permission to ${BOLD}${destination.name}${RESET}")
        destination.setExecutable(true).let { can ->
            if (!can) {
                printError("Failed to set execute permission on the installed file.")
                println("${YELLOW}Tip:${RESET} You may need to set permissions manually with 'chmod +x ${BOLD}${destination.absolutePath}${RESET}'")
            }
        }

        destination.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())

        println("${GREEN}✔ Installation complete!${RESET}")
        println("${GREEN}Ready to use:${RESET} Type ${BOLD}${exeFile.name}${RESET} in your terminal.")

    } catch (e: Exception) {
        printError("Installation failed.")
        println("${YELLOW}Reason:${RESET} ${e.message}")
        exitProcess(1)
    }
}

private fun installDirectory(dir: File, exeNameOverride: String, deleteOriginal: Boolean) {
    try {
        val targetDir = File(LIB_PATH, dir.name)

        if (targetDir.exists()) {
            printError("Directory '${dir.name}' already exists in $LIB_PATH.")
            println("${YELLOW}Fix:${RESET} Remove it manually before reinstalling.")
            exitProcess(1)
        }

        if (deleteOriginal) {
            println("${YELLOW}• Moving directory to ${BOLD}$LIB_PATH${RESET} ...")
            dir.moveTo(LIB_PATH)
        } else {
            println("${CYAN}• Copying directory to ${BOLD}$LIB_PATH${dir.name}${RESET} ...")
            dir.copyTo(LIB_PATH + dir.name)
        }

        println("${GREEN}✔ Directory installed successfully.${RESET}")
        println("${GREEN}• Location:${RESET} ${BOLD}$LIB_PATH${dir.name}${RESET}")

        // Linking executable
        val linkFile = File(BIN_PATH, exeNameOverride)

        println("${CYAN}• Creating executable link:${RESET} ${BOLD}$BIN_PATH$exeNameOverride${RESET}")

        if (linkFile.exists()) {
            printError("Executable '${exeNameOverride}' already exists in $BIN_PATH.")
            println("${YELLOW}Fix:${RESET} Remove it manually or choose a different executable name.")
            exitProcess(1)
        }

        val installedDir = File(LIB_PATH, dir.name)

        val exeFile = installedDir
            .listFiles()
            ?.find { it.isFile() && it.name == exeNameOverride }
            ?: run {
                printError("Executable '${exeNameOverride}' not found inside installed directory.")
                println("${YELLOW}Expected:${RESET} ${installedDir.absolutePath}/$exeNameOverride")
                exitProcess(1)
            }

        println("${YELLOW}• Granting execute permission to ${BOLD}${exeFile.name}${RESET}")
        exeFile.setExecutable(true)

        exeFile.createSymlinkTo(linkFile.absolutePath)
        println("${GREEN}✔ Symlink created successfully.${RESET}")

        exeFile.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())

        println("${GREEN}✔ Installation complete!${RESET}")
        println("${GREEN}Ready to use:${RESET} Type ${BOLD}$exeNameOverride${RESET} in your terminal.")

    } catch (e: Exception) {
        printError("Installation failed.")
        println("${YELLOW}Reason:${RESET} ${e.message}")
        exitProcess(1)
    }
}