package org.ensodai.avalonmediacard.service.watchparty

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class WatchRoomChatStoreTest {

    @Test
    fun `test add and get chat history for episode`() {
        val store = WatchRoomChatStore(maxMessagesPerEpisode = 10)
        val roomId = Uuid.random()
        val userId1 = Uuid.random()

        store.addMessage(
            roomId = roomId,
            senderUserId = userId1,
            senderUsername = "Alex",
            senderAvatarUrl = null,
            text = "Hello season 1 episode 1!",
            playbackPositionMs = 120_000L,
            season = 1,
            episode = 1
        )

        val history = store.getHistory(season = 1, episode = 1)
        assertEquals(1, history.size)
        assertEquals("Alex", history[0].senderUsername)
        assertEquals("Hello season 1 episode 1!", history[0].text)
        assertEquals(120_000L, history[0].playbackPositionMs)
    }

    @Test
    fun `test episode partitioning isolates messages`() {
        val store = WatchRoomChatStore(maxMessagesPerEpisode = 10)
        val roomId = Uuid.random()
        val userId1 = Uuid.random()

        store.addMessage(
            roomId = roomId,
            senderUserId = userId1,
            senderUsername = "Alex",
            senderAvatarUrl = null,
            text = "Msg in Episode 1",
            playbackPositionMs = 5000L,
            season = 1,
            episode = 1
        )

        store.addMessage(
            roomId = roomId,
            senderUserId = userId1,
            senderUsername = "Alex",
            senderAvatarUrl = null,
            text = "Msg in Episode 2",
            playbackPositionMs = 8000L,
            season = 1,
            episode = 2
        )

        val ep1History = store.getHistory(season = 1, episode = 1)
        val ep2History = store.getHistory(season = 1, episode = 2)
        val ep3History = store.getHistory(season = 1, episode = 3)

        assertEquals(1, ep1History.size)
        assertEquals("Msg in Episode 1", ep1History[0].text)

        assertEquals(1, ep2History.size)
        assertEquals("Msg in Episode 2", ep2History[0].text)

        assertTrue(ep3History.isEmpty())
    }

    @Test
    fun `test ring buffer rotation when limit exceeded`() {
        val maxLimit = 3
        val store = WatchRoomChatStore(maxMessagesPerEpisode = maxLimit)
        val roomId = Uuid.random()
        val userId1 = Uuid.random()

        for (i in 1..5) {
            store.addMessage(
                roomId = roomId,
                senderUserId = userId1,
                senderUsername = "User",
                senderAvatarUrl = null,
                text = "Message $i",
                playbackPositionMs = i * 1000L,
                season = 1,
                episode = 1
            )
        }

        val history = store.getHistory(season = 1, episode = 1)
        assertEquals(3, history.size)
        assertEquals("Message 3", history[0].text)
        assertEquals("Message 4", history[1].text)
        assertEquals("Message 5", history[2].text)
    }

    @Test
    fun `test clearAll empties all episodes`() {
        val store = WatchRoomChatStore(maxMessagesPerEpisode = 10)
        val roomId = Uuid.random()
        val userId1 = Uuid.random()

        store.addMessage(roomId, userId1, "A", null, "M1", 100L, 1, 1)
        store.addMessage(roomId, userId1, "A", null, "M2", 200L, 1, 2)

        store.clearAll()

        assertTrue(store.getHistory(1, 1).isEmpty())
        assertTrue(store.getHistory(1, 2).isEmpty())
    }
}
