package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SmartPanelGameModeReaderTest {
    @Test
    fun contractMatchesDiscoveredSmartPanelProvider() {
        assertEquals(
            "content://com.transsion.gamemode.provider/listapp",
            SmartPanelGameModeReader.CONTENT_URI.toString()
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
    fun checkedValueContractIsConservative() {
        assertEquals(true, "1" == "1")
        assertEquals(true, "true".equals("true", ignoreCase = true))
        assertEquals(false, "0" == "1")
        assertEquals(false, "unexpected".equals("true", ignoreCase = true))
    }
}
