package off.kys.gcmd.common

/**
 * Prints the usage information for the gcmd tool, including available commands, options, and examples.
 * This function is called when the user requests help or when they provide invalid input.
 * The usage message is formatted with terminal colors and styles for better readability.
 */
fun printUsage() {
    val usage = """
${BOLD}USAGE:${RESET}
    gcmd <COMMAND> [OPTIONS] [ARGS...]

${BOLD}COMMANDS:${RESET}
    ${GREEN}install${RESET} <path> [exe_name]   Install a package
    ${GREEN}version${RESET}                   Show version information
    ${GREEN}help${RESET}                      Show this help message

${BOLD}INSTALL BEHAVIOR:${RESET}
    ${DIM}• Single File:${RESET} Copies the binary directly to ${BIN_PATH}.
    ${DIM}• Directory:${RESET} Copies the directory to $LIB_PATH and symlinks the 
                   executable to ${BIN_PATH}. You can optionally specify 
                   [exe_name] if the executable name differs from the folder name.

${BOLD}OPTIONS (install):${RESET}
    --delete-original     Move the file/directory instead of copying it
    --brutal              Force installation by overwriting existing files (use with caution)
    --no-symlink          Do not create a symlink in $BIN_PATH for directory installations
    --no-trace            Do not add GCMD version metadata to the installed package (can cause package management issues)

${BOLD}EXAMPLES:${RESET}
    sudo gcmd install ./my_script.sh              ${DIM}# Installs a single file${RESET}
    sudo gcmd install ./tool_folder -d            ${DIM}# Moves directory, uses folder name for symlink${RESET}
    sudo gcmd install ./tool_folder custom_name   ${DIM}# Installs directory, links 'custom_name'${RESET}
    """.trimIndent()
    println(usage)
}