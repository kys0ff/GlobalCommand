@file:Suppress("unused")

package off.kys.gcmd.util

import kotlinx.cinterop.*
import platform.linux.getxattr
import platform.linux.setxattr
import platform.posix.*

class File {
    private val path: String

    val absolutePath: String
        get() = path.replace("\\", "/")

    constructor(path: String) {
        this.path = path.replace("\\", "/")
    }

    constructor(root: String, child: String) {
        this.path = "${root.replace("\\", "/").trimEnd('/')}/$child"
    }

    constructor(root: File, child: String) {
        this.path = "${root.absolutePath.replace("\\", "/").trimEnd('/')}/$child"
    }

    /**
     * Extracts the file name from a given file path.
     *
     * @return the file name
     */
    val name: String
        get() = absolutePath.substringAfterLast('/').substringAfterLast('\\')

    /**
     * Extracts the file name without its extension from a given file path.
     *
     * @return the file name without the extension or the full file name if there is no extension
     */
    val nameWithoutExtension: String
        get() = name.substringBeforeLast('.')

    /**
     * Extracts the file extension from a given file path.
     *
     * @return the file extension or an empty string if there is no extension
     */
    val extension: String
        get() = name.substringAfterLast('.', "")

    /**
     * Checks if the file at the specified path exists.
     *
     * @return true if the file exists, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun exists(): Boolean {
        val file = fopen(absolutePath, "r")
        return if (file != null) {
            fclose(file)
            true
        } else {
            false
        }
    }

    /**
     * Checks if the path represents a file.
     *
     * @return true if the path is a file, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun isFile(): Boolean {
        val file = fopen(absolutePath, "r")
        return if (file != null) {
            fclose(file)
            true
        } else {
            false
        }
    }

    /**
     * Checks if the path represents a directory.
     *
     * @return true if the path is a directory, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun isDirectory(): Boolean {
        val dirPath = absolutePath
        val result = opendir(dirPath)
        return if (result != null) {
            closedir(result)
            true
        } else {
            false
        }
    }

    /**
     * Creates a symbolic link to this file or directory at the specified [linkPath].
     * * @param linkPath The path where the symlink should be created.
     * @return true if the symlink was created successfully, false otherwise.
     */
    fun createSymlinkTo(linkPath: String): Boolean {
        // symlink(target, linkPath)
        // target is 'this.absolutePath', linkPath is where the "shortcut" goes
        val result = symlink(this.absolutePath, linkPath)

        if (result != 0) {
            println("Failed to create $linkPath: $result")
            return false
        }
        return true
    }

    /**
     * Checks if the file at the specified path is executable.
     *
     * @return true if the file is executable, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun canExecute(): Boolean = access(absolutePath, X_OK) == 0

    /**
     * Sets the executable bit for the owner of the file.
     * * @param executable true to add execute permission, false to remove it.
     * @return true if the operation was successful, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun setExecutable(executable: Boolean): Boolean = memScoped {
        val st = alloc<stat>()

        if (stat(absolutePath, st.ptr) != 0) return@memScoped false

        val execBits = S_IXUSR or S_IXGRP or S_IXOTH
        val currentMode = st.st_mode.toInt()

        val newMode = if (executable) {
            currentMode or execBits        // chmod +x
        } else {
            currentMode and execBits.inv() // chmod -x
        }.toUInt()

        chmod(absolutePath, newMode) == 0
    }

    /**
     * Checks if the file is an ELF binary or a script with a shebang,
     * and if the current user has execution permissions.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun isRunnable(): Boolean {
        // 1. Check if the OS even allows execution (chmod +x)
        // if (access(absolutePath, X_OK) != 0) return false

        // 2. Check the file header for ELF magic or Shebang
        val file = fopen(absolutePath, "rb") ?: return false
        return try {
            memScoped {
                val header = allocArray<ByteVar>(4)
                val bytesRead = fread(header, 1u, 4u, file)

                // If we couldn't even read 2 bytes, it can't be a shebang or an ELF
                if (bytesRead < 2u) return@memScoped false

                val b0 = header[0].toInt() and 0xFF
                val b1 = header[1].toInt() and 0xFF

                // Check for Shebang: #! (0x23 0x21)
                val isScript = b0 == 0x23 && b1 == 0x21
                var isElf = false

                // Only check for ELF if we actually read at least 4 bytes
                if (bytesRead >= 4u) {
                    val b2 = header[2].toInt() and 0xFF
                    val b3 = header[3].toInt() and 0xFF

                    // Check for ELF Magic: 0x7F 'E' 'L' 'F'
                    isElf = b0 == 0x7F && b1 == 'E'.code && b2 == 'L'.code && b3 == 'F'.code
                }

                isElf || isScript
            }
        } finally {
            fclose(file)
        }
    }

    /**
     * Creates a directory at the specified path.
     * 
     * @return true if the directory was created successfully, false otherwise.
     */
    fun mkdir(): Boolean {
        val dirPath = absolutePath
        val result = mkdir(dirPath, 493u) // rwxr-xr-x permissions
        return result == 0
    }

    /**
     * Creates the directory named by this abstract pathname, including any necessary but nonexistent parent
     * directories.
     * 
     * @return true if the directory was created successfully, false otherwise.
     */
    fun mkdirs(): Boolean {
        val parts = absolutePath.split('/', '\\')
        var currentPath = ""
        for (part in parts) {
            if (part.isEmpty()) continue
            currentPath += "/$part"
            val dir = File(currentPath)
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Sets an extended attribute on this file.
     *
     * @param name Attribute name (e.g., "user.comment")
     * @param value Attribute value as ByteArray
     * @return true if successful, false otherwise
     */
    @OptIn(ExperimentalForeignApi::class)
    fun setExtendedAttribute(name: String, value: ByteArray): Boolean = memScoped {
        val result = setxattr(
            absolutePath,
            name,
            value.refTo(0),
            value.size.convert(),
            0
        )
        result == 0
    }

    /**
     * Gets an extended attribute from this file.
     *
     * @param name Attribute name
     * @return ByteArray value or null if not found
     */
    @OptIn(ExperimentalForeignApi::class)
    fun getExtendedAttribute(name: String): ByteArray? = memScoped {
        // First call to determine required size
        val size = getxattr(absolutePath, name, null, 0u)
        if (size <= 0) return@memScoped null

        val buffer = ByteArray(size.toInt())

        val result = getxattr(
            absolutePath,
            name,
            buffer.refTo(0),
            buffer.size.convert()
        )

        if (result < 0) null else buffer
    }

    /**
     * Moves this file to the specified destination path.
     *
     * @param destinationPath The path where the file should be moved.
     * @param overwrite If true, overwrite existing file (mv -f).
     *                  If false, do not overwrite (mv -n).
     *
     * @return true if the move was successful, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun moveTo(destinationPath: String, overwrite: Boolean = true): Boolean = memScoped {
        val pid = fork()
        if (pid < 0) return false

        if (pid == 0) {
            if (overwrite) {
                execlp("mv", "mv", "-f", absolutePath, destinationPath, null)
            } else {
                execlp("mv", "mv", "-n", absolutePath, destinationPath, null)
            }
            exit(127) // exec failed
        }

        val status = alloc<IntVar>()
        if (waitpid(pid, status.ptr, 0) < 0) return false

        return status.value == 0
    }

    /**
     * Copies this file to the specified destination path using the system `cp` command.
     *
     * @param destinationPath The path where the file should be copied.
     * @param overwrite If true, overwrite existing file (cp -f).
     *                  If false, do not overwrite (cp -n).
     * @param recursive If true, copy directories recursively (cp -r).
     *
     * @return true if the copy was successful, false otherwise.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun copyTo(
        destinationPath: String,
        overwrite: Boolean = true,
        recursive: Boolean = true,
    ): Boolean = memScoped {
        val pid = fork()
        if (pid < 0) return false

        if (pid == 0) {
            val overwriteFlag = if (overwrite) "-f" else "-n"

            when {
                recursive -> execlp(
                    "cp",
                    "cp",
                    overwriteFlag,
                    "-r",
                    absolutePath,
                    destinationPath,
                    null
                )

                else -> execlp(
                    "cp",
                    "cp",
                    overwriteFlag,
                    absolutePath,
                    destinationPath,
                    null
                )
            }

            exit(127) // exec failed
        }

        val status = alloc<IntVar>()
        if (waitpid(pid, status.ptr, 0) < 0) return false

        return status.value == 0
    }

    /**
     * Writes the given text to a file at the specified path.
     * Overwrites the file if it already exists.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun writeText(text: String) {
        val file = fopen(absolutePath, "w") ?: throw Exception("Cannot open file for writing: $absolutePath")
        try {
            fprintf(file, "%s", text)
        } finally {
            fclose(file)
        }
    }

    /**
     * Reads the entire content of a file at the specified path.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun readText(): String {
        val file = fopen(absolutePath, "r") ?: throw Exception("Cannot open file for reading: $absolutePath")
        val sb = StringBuilder()

        try {
            memScoped {
                val bufferSize = 1024
                val buffer = allocArray<ByteVar>(bufferSize)
                while (fgets(buffer, bufferSize, file) != null) {
                    sb.append(buffer.toKString())
                }
            }
        } finally {
            fclose(file)
        }

        return sb.toString()
    }

    /**
     * Returns a list of File objects contained within this directory.
     * * @return A List of File objects, or null if this is not a directory or cannot be read.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun listFiles(): List<File>? {
        val dir = opendir(absolutePath) ?: return null
        val files = mutableListOf<File>()

        try {
            while (true) {
                val entry = readdir(dir) ?: break
                val name = entry.pointed.d_name.toKString()

                // Skip the navigation shortcuts "." and ".."
                if (name != "." && name != "..") {
                    files.add(File(this, name))
                }
            }
        } finally {
            closedir(dir)
        }

        return files
    }

    /**
     * Returns the string representation of this File object, which is its absolute path.
     *
     * @return the absolute path of the file
     */
    override fun toString(): String = absolutePath

    companion object {
        /**
         * The character used to separate directories in file paths.
         * 
         * @return the separator character
         */
        val separatorChar: Char
            get() = '/'
    }
}

/**
 * Converts a string representing a file path to a File object.
 *
 * @param this the string representing the file path
 * @return the File object representing the given path
 */
fun String.toFile(): File {
    return File(this)
}