package com.maouuusama.ai.device.optimizer.monitor

import android.content.ContentResolver
import android.net.Uri

data class GameModePackageEntry(
    val packageName: String,
    val className: String?,
    val checked: Boolean
)

data class GameModeReadResult(
    val available: Boolean,
    val packages: List<GameModePackageEntry>,
    val error: String? = null
)

class SmartPanelGameModeReader(
    private val contentResolver: ContentResolver
) {
    fun read(): GameModeReadResult {
        return try {
            contentResolver.query(
                CONTENT_URI,
                PROJECTION,
                null,
                null,
                null
            )?.use { cursor ->
                val packageIndex = cursor.getColumnIndex(COLUMN_PACKAGE_NAME)
                val classIndex = cursor.getColumnIndex(COLUMN_CLASS_NAME)
                val checkedIndex = cursor.getColumnIndex(COLUMN_IS_CHECKED)

                if (packageIndex < 0 || checkedIndex < 0) {
                    return GameModeReadResult(
                        available = false,
                        packages = emptyList(),
                        error = "SmartPanel Game Mode provider returned an unexpected schema"
                    )
                }

                val packages = buildList {
                    while (cursor.moveToNext()) {
                        val packageName = cursor.getString(packageIndex)?.trim().orEmpty()
                        if (packageName.isEmpty()) continue
                        add(
                            GameModePackageEntry(
                                packageName = packageName,
                                className = if (classIndex >= 0) {
                                    cursor.getString(classIndex)?.trim()?.takeIf { it.isNotEmpty() }
                                } else null,
                                checked = parseCheckedFlag(cursor.getString(checkedIndex))
                            )
                        )
                    }
                }
                GameModeReadResult(available = true, packages = packages)
            } ?: GameModeReadResult(
                available = false,
                packages = emptyList(),
                error = "SmartPanel Game Mode provider returned no cursor"
            )
        } catch (security: SecurityException) {
            GameModeReadResult(
                available = false,
                packages = emptyList(),
                error = "SmartPanel Game Mode provider permission denied"
            )
        } catch (error: Exception) {
            GameModeReadResult(
                available = false,
                packages = emptyList(),
                error = "SmartPanel Game Mode provider query failed: " + error.javaClass.simpleName
            )
        }
    }

    companion object {
        const val AUTHORITY = "com.transsion.gamemode.provider"
        const val PATH = "listapp"
        const val READ_PERMISSION = "com.transsion.gamemode.permission.READ_APP_LIST"
        const val CONTENT_URI_STRING = "content://$AUTHORITY/$PATH"
        val CONTENT_URI: Uri
            get() = Uri.parse(CONTENT_URI_STRING)

        internal fun parseCheckedFlag(value: String?): Boolean = value?.trim()?.let {
            it == "1" || it.equals("true", ignoreCase = true)
        } == true
        const val COLUMN_ID = "_id"
        const val COLUMN_PACKAGE_NAME = "packagename"
        const val COLUMN_CLASS_NAME = "classname"
        const val COLUMN_IS_CHECKED = "ischeck"
        val PROJECTION = arrayOf(
            COLUMN_ID,
            COLUMN_PACKAGE_NAME,
            COLUMN_CLASS_NAME,
            COLUMN_IS_CHECKED
        )
    }
}
