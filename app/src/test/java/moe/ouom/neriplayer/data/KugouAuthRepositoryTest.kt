package moe.ouom.neriplayer.data

import moe.ouom.neriplayer.data.auth.common.SavedCookieAuthState
import moe.ouom.neriplayer.data.auth.kugou.KugouAuthBundle
import moe.ouom.neriplayer.data.auth.kugou.evaluateKugouAuthHealth
import moe.ouom.neriplayer.data.auth.kugou.withoutKugouLoginCookies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouAuthRepositoryTest {

    @Test
    fun kugouAuthBundle_jsonRoundTripKeepsCookieSnapshotAndSavedAt() {
        val original = KugouAuthBundle(
            cookies = linkedMapOf(
                "token" to "token-value",
                "userid" to "12345",
                "dfid" to "device-value"
            ),
            savedAt = 123L
        )

        val restored = KugouAuthBundle.fromJson(original.toJson())

        assertEquals(original.cookies, restored.cookies)
        assertEquals(123L, restored.savedAt)
    }

    @Test
    fun evaluateKugouAuthHealth_returnsMissingForZeroUserId() {
        val health = evaluateKugouAuthHealth(
            KugouAuthBundle(
                cookies = mapOf("token" to "token-value", "userid" to "0"),
                savedAt = 1_000L
            ),
            now = 2_000L
        )

        assertEquals(SavedCookieAuthState.Missing, health.state)
        assertFalse(health.shouldPromptRelogin)
    }

    @Test
    fun evaluateKugouAuthHealth_returnsValidForPositiveUserId() {
        val health = evaluateKugouAuthHealth(
            KugouAuthBundle(
                cookies = mapOf("token" to "token-value", "userid" to "12345"),
                savedAt = 1_000L
            ),
            now = 2_000L
        )

        assertEquals(SavedCookieAuthState.Valid, health.state)
        assertTrue(health.loginCookieKeys.containsAll(listOf("token", "userid")))
    }

    @Test
    fun withoutKugouLoginCookies_removesCredentialsAndKeepsDeviceIdentity() {
        val retained = withoutKugouLoginCookies(
            linkedMapOf(
                "token" to "token-value",
                "userid" to "12345",
                "vip_token" to "vip-value",
                "vip_type" to "1",
                "dfid" to "device-value",
                "KUGOU_API_MID" to "mid-value",
                "blank" to ""
            )
        )

        assertEquals(
            mapOf(
                "dfid" to "device-value",
                "KUGOU_API_MID" to "mid-value"
            ),
            retained
        )
    }
}
