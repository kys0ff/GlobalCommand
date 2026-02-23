package off.kys.gcmd

/**
 * Prints an error message to the console in a standardized format.
 * The message is prefixed with "error:" and styled in red and bold for emphasis.
 * This function is used throughout the application to report errors to the user in a consistent manner.
 *
 * @param message The error message to be displayed to the user.
 */
fun printError(message: String) {
    println("${RED}${BOLD}error:${RESET} $message")
}