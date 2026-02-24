package off.kys.gcmd.cli

import off.kys.gcmd.common.ATTR_GCMD_LIB_PATH
import off.kys.gcmd.common.ATTR_GCMD_VERSION
import off.kys.gcmd.common.BIN_PATH
import off.kys.gcmd.common.BOLD
import off.kys.gcmd.common.CYAN
import off.kys.gcmd.common.GCMD_VERSION
import off.kys.gcmd.common.GREEN
import off.kys.gcmd.common.LIB_PATH
import off.kys.gcmd.common.RESET
import off.kys.gcmd.common.YELLOW
import off.kys.gcmd.common.printError
import off.kys.gcmd.common.supportedExecutables
import off.kys.gcmd.util.File
import off.kys.gcmd.util.isRoot
import off.kys.gcmd.util.toFile
import kotlin.system.exitProcess

/**
 * Handles the 'install' command for gcmd, which allows users to install executable files or directories containing executables.
 * This function performs several checks and operations to ensure a smooth installation process, including:
 * - Verifying root privileges
 * - Validating the provided package path
 * - Handling both file and directory installations
 * - Managing options for deleting the original file, forcing installation, skipping symlink creation, and skipping version metadata
 *
 * @param positionals A list of positional arguments provided by the user (e.g., command and package path).
 * @param flags A list of flags provided by the user (e.g., --delete-original, --brutal, --no-symlink, --no-trace).
 */
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

    val deleteOriginal = flags.contains("--delete-original")
    val forceInstall = flags.contains("--brutal")
    val noSymlink = flags.contains("--no-symlink")
    val noTrace = flags.contains("--no-trace")

    println("${CYAN}▶ Starting installation...${RESET}")
    println("${CYAN}•${RESET} Source: ${BOLD}$pkgPath${RESET}")
    println("${CYAN}•${RESET} Mode: ${if (deleteOriginal) "Move (delete original)" else "Copy"}")

    when {
        pkgFile.isFile() && (pkgFile.isRunnable() || pkgFile.extension in supportedExecutables()) -> {
            if (positionals.getOrNull(2) != null)
                println("${YELLOW}• Warning:${RESET} Executable name argument will be ignored for single file installations.")
            if (noSymlink)
                println("${YELLOW}• Note:${RESET} Symlink option is not applicable for single file installations.")
            installFile(
                pkgFile,
                deleteOriginal,
                forceInstall,
                noTrace
            )
        }

        pkgFile.isDirectory() ->
            installDirectory(
                pkgFile,
                positionals.getOrNull(2)?.ifEmpty { pkgFile.name } ?: pkgFile.name,
                deleteOriginal,
                forceInstall,
                noSymlink,
                noTrace
            )

        else -> {
            printError("Unsupported package type.")
            println("${YELLOW}Expected:${RESET} Executable file or directory.")
            exitProcess(1)
        }
    }
}

/**
 * Installs a single executable file to the BIN_PATH directory.
 * This function handles both copying and moving the file based on the deleteOriginal flag.
 * It also sets the appropriate permissions and adds version metadata if noTrace is false.
 *
 * @param exeFile The executable file to be installed.
 * @param deleteOriginal If true, the original file will be moved instead of copied.
 * @param forceInstall If true, existing files will be overwritten without confirmation.
 * @param noTrace If true, version metadata will not be added to the installed
 */
private fun installFile(
    exeFile: File,
    deleteOriginal: Boolean,
    forceInstall: Boolean,
    noTrace: Boolean,
) {
    try {
        val destination = File(BIN_PATH, exeFile.name)

        if (destination.exists() && !forceInstall) {
            printError("Executable '${exeFile.name}' already exists in ${BIN_PATH}.")
            println("${YELLOW}Fix:${RESET} Remove it manually before reinstalling.")
            exitProcess(1)
        }

        if (deleteOriginal) {
            println("${YELLOW}• Moving executable to ${BOLD}${BIN_PATH}${RESET} ...")
            exeFile.moveTo(BIN_PATH)
        } else {
            println("${CYAN}• Copying executable to ${BOLD}${BIN_PATH}${RESET} ...")
            exeFile.copyTo(BIN_PATH)
        }

        println("${YELLOW}• Granting execute permission to ${BOLD}${destination.name}${RESET}")
        destination.setExecutable(true).let { can ->
            if (!can) {
                printError("Failed to set execute permission on the installed file.")
                println("${YELLOW}Tip:${RESET} You may need to set permissions manually with 'chmod +x ${BOLD}${destination.absolutePath}${RESET}'")
            }
        }

        if (noTrace) {
            println("${YELLOW}• Skipping version metadata due to --no-trace flag.${RESET}")
        } else {
            destination.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())
            println("${GREEN}✔ Version metadata added.${RESET}")
        }

        println("${GREEN}✔ Installation complete!${RESET}")
        println("${GREEN}Ready to use:${RESET} Type ${BOLD}${exeFile.name}${RESET} in your terminal.")

    } catch (e: Exception) {
        printError("Installation failed.")
        println("${YELLOW}Reason:${RESET} ${e.message}")
        exitProcess(1)
    }
}

/**
 * Installs a directory containing an executable to the LIB_PATH directory and creates a symlink in BIN_PATH.
 * This function handles both copying and moving the directory based on the deleteOriginal flag.
 * It also sets the appropriate permissions on the executable and adds version metadata if noTrace is false.
 * @param dir The directory to be installed.
 *
 * @param exeNameOverride The name of the executable to link in BIN_PATH (defaults to the directory name if not provided).
 * @param deleteOriginal If true, the original directory will be moved instead of copied.
 * @param forceInstall If true, existing files will be overwritten without confirmation.
 * @param noSymlink If true, the symlink in BIN_PATH will not be created (not recommended for directory installations).
 * @param noTrace If true, version metadata will not be added to the installed package (can cause issues with package management).
 */
private fun installDirectory(
    dir: File,
    exeNameOverride: String,
    deleteOriginal: Boolean,
    forceInstall: Boolean,
    noSymlink: Boolean,
    noTrace: Boolean,
) {
    try {
        val targetDir = File(LIB_PATH, dir.name)

        if (targetDir.exists() && !forceInstall) {
            printError("Directory '${dir.name}' already exists in ${LIB_PATH}.")
            println("${YELLOW}Fix:${RESET} Remove it manually before reinstalling.")
            exitProcess(1)
        }

        if (deleteOriginal) {
            println("${YELLOW}• Moving directory to ${BOLD}${LIB_PATH}${RESET} ...")
            dir.moveTo(LIB_PATH)
        } else {
            println("${CYAN}• Copying directory to ${BOLD}${LIB_PATH}${dir.name}${RESET} ...")
            dir.copyTo(LIB_PATH + dir.name)
        }

        println("${GREEN}✔ Directory installed successfully.${RESET}")
        println("${GREEN}• Location:${RESET} ${BOLD}${LIB_PATH}${dir.name}${RESET}")

        if (noSymlink) {
            println("${YELLOW}• Skipping symlink creation due to --no-symlink flag.${RESET}")
            println("${YELLOW}Warning:${RESET} Without a symlink in ${BIN_PATH}, you will need to run the executable with its full path.")

            if (noTrace) {
                println("${YELLOW}• Skipping version metadata due to --no-trace flag.${RESET}")
            } else {
                targetDir.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())
                println("${GREEN}✔ Version metadata added to installed directory.${RESET}")
            }
        } else {
            // Linking executable
            val linkFile = File(BIN_PATH, exeNameOverride)

            println("${CYAN}• Creating executable link:${RESET} ${BOLD}${BIN_PATH}$exeNameOverride${RESET}")

            if (linkFile.exists() && !forceInstall) {
                printError("Executable '${exeNameOverride}' already exists in ${BIN_PATH}.")
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

            if (noTrace) {
                println("${YELLOW}• Skipping version metadata due to --no-trace flag.${RESET}")
            } else {
                targetDir.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())
                println("${GREEN}✔ Version metadata added to installed directory.${RESET}")

                exeFile.setExtendedAttribute(ATTR_GCMD_VERSION, GCMD_VERSION.encodeToByteArray())
                println("${GREEN}✔ Version metadata added to executable.${RESET}")

                exeFile.setExtendedAttribute(ATTR_GCMD_LIB_PATH, targetDir.absolutePath.encodeToByteArray())
                println("${GREEN}✔ Library path metadata added to executable.${RESET}")
            }
        }

        println("${GREEN}✔ Installation complete!${RESET}")

        if (!noSymlink)
            println("${GREEN}Ready to use:${RESET} Type ${BOLD}$exeNameOverride${RESET} in your terminal.")

    } catch (e: Exception) {
        printError("Installation failed.")
        println("${YELLOW}Reason:${RESET} ${e.message}")
        exitProcess(1)
    }
}