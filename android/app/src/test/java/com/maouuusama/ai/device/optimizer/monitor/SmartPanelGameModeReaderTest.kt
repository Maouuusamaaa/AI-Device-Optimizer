package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPanelGameModeReaderTest {
    @Test
    fun contractMatchesDiscoveredSmartPanelProvider() {
        assertEquals(
            "content://com.transsion.gamemode.provider/listapp",
            SmartPanelGameModeReader.CONTENT_URI_STRING
        )
        assertEquals(
            "com.transsion.gamemode.permission.READ_APP_LIST",
            SmartPanelGameModeReader.READ_PERMISSION
        )
        assertArrayEquals(
            arrayOf("_id", "packagename", "classname", "ischeck"),
            SmartPanelGameModeReader.PROJECTION
        )
    }

    @Test
    fun checkedValueParsingIsConservative() {
        assertEquals(true, SmartPanelGameModeReader.parseCheckedFlag("1"))
        assertEquals(true, SmartPanelGameModeReader.parseCheckedFlag("true"))
        assertEquals(true, SmartPanelGameModeReader.parseCheckedFlag(" TRUE "))
        assertEquals(false, SmartPanelGameModeReader.parseCheckedFlag("0"))
        assertEquals(false, SmartPanelGameModeReader.parseCheckedFlag("unexpected"))
        assertEquals(false, SmartPanelGameModeReader.parseCheckedFlag(null))
    }
}
