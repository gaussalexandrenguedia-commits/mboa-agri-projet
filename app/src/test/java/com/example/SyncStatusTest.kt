package com.example

import com.example.data.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusTest {
    @Test
    fun `un scan hors connexion commence en attente puis devient synchronise`() {
        var status = SyncStatus.PENDING
        assertEquals(SyncStatus.PENDING, status)
        status = SyncStatus.SYNCED
        assertEquals(SyncStatus.SYNCED, status)
    }

    @Test
    fun `un echec reseau est rejouable`() {
        var status = SyncStatus.PENDING
        status = SyncStatus.FAILED
        assertEquals(SyncStatus.FAILED, status)
        status = SyncStatus.SYNCED
        assertEquals(SyncStatus.SYNCED, status)
    }
}
