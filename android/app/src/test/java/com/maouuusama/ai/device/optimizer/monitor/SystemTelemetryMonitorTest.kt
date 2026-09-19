package com.maouuusama.ai.device.optimizer.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SystemTelemetryMonitorTest {

    private val monitor = SystemTelemetryMonitor()

    @Test
    fun parsesSystemMemory() {
        val memory = monitor.parseMemoryInfo(
            """
            MemTotal:        5769688 kB
            MemFree:           85528 kB
            MemAvailable:    1731816 kB
            Cached:          1572408 kB
            SwapTotal:       3165084 kB
            SwapFree:         979812 kB
            Shmem:             15204 kB
            SReclaimable:     113116 kB
            """.trimIndent()
        )

        assertEquals(5769688L, memory.memTotalKb)
        assertEquals(1731816L, memory.memAvailableKb)
        assertEquals(2165272L, memory.swapUsedKb)
    }

    @Test
    fun parsesSystemProcessesAndSortsByPss() {
        val processes = monitor.parseProcesses(
            """
              1,090,688K: com.example.large (pid 100 / activities)
                439,188K: system (pid 200)
                130,976K: rish (pid 300)
            """.trimIndent()
        )

        assertEquals(3, processes.size)
        assertEquals("com.example.large", processes[0].processName)
        assertEquals(100, processes[0].pid)
        assertEquals(1_090_688L, processes[0].pssKb)
    }

    @Test
    fun parsesCpuCounters() {
        val counters = monitor.parseCpuLine(
            "cpu  15246156 1726784 8810238 70604002 90445 0 553340 0 0 0"
        )
        assertNotNull(counters)
        assertEquals(15246156L, counters!!.userJiffies)
        assertEquals(70604002L, counters.idleJiffies)
    }

    @Test
    fun rejectsInvalidCpuLine() {
        assertNull(monitor.parseCpuLine("not cpu data"))
    }

    @Test
    fun calculatesCpuUtilizationFromTwoSnapshots() {
        val cpu = SystemCpuMonitor()
        cpu.collect(
            SystemTelemetryMonitor.SystemCpuCounters(
                15246156L, 1726784L, 8810238L, 70604002L, 90445L, 0L, 553340L
            )
        )
        val result = cpu.collect(
            SystemTelemetryMonitor.SystemCpuCounters(
                15247044L, 1726876L, 8810711L, 70608350L, 90447L, 0L, 553384L
            )
        )

        assertNotNull(result)
        assertEquals(25.63, result!!.utilizationPercent!!, 0.05)
    }
}
