package off.kys.gcmd

import platform.posix.geteuid

/**
 * Checks if the current process is running with root privileges.
 * On Unix-like systems, the effective user ID (EUID) of 0 indicates root.
 *
 * @return true if running as root, false otherwise.
 */
fun isRoot(): Boolean = geteuid() == 0u
