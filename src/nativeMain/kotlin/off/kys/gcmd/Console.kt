package off.kys.gcmd

/**
 * This file contains ANSI escape codes for coloring and styling console output.
 * These codes can be used to enhance the readability of messages printed to the terminal.
 * The constants defined here can be used throughout the application to maintain a consistent style.
 */

/**
 * Resets all styles and colors to default.
 */
const val RESET = "\u001B[0m"

/**
 * ANSI escape code for red text, typically used for errors or warnings.
 */
const val RED = "\u001B[31m"

/**
 * ANSI escape code for green text, often used to indicate success or positive messages.
 */
const val GREEN = "\u001B[32m"

/**
 * ANSI escape code for yellow text, commonly used for warnings or important notes.
 */
const val YELLOW = "\u001B[33m"

/**
 * ANSI escape code for cyan text, which can be used for informational messages or to highlight certain parts of the output.
 */
const val CYAN = "\u001B[36m"

/**
 * ANSI escape code for bold text, used to emphasize important information or headings.
 */
const val BOLD = "\u001B[1m"

/**
 * ANSI escape code for dim text, which can be used to de-emphasize less important information or to provide additional context without overwhelming the main message.
 */
const val DIM = "\u001B[2m"