package off.kys.gcmd

/**
 * Returns a list of supported executable file extensions for the current platform.
 * This function can be used to determine which types of executable files the application can handle.
 *
 * @return A list of supported executable file extensions.
 */
fun supportedExecutables(): List<String> = listOf("sh", "bin", "run")
