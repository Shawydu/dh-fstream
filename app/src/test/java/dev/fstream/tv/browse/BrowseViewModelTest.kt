package dev.fstream.tv.browse

import dev.fstream.tv.provider.fake.FakeProviderAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    @Test
    fun homeLoadsThenErrorsThenRetrySucceeds() = runTest {
        val fake = FakeProviderAdapter(failing = true)
        val vm = HomeViewModel(fake, TestScope(StandardTestDispatcher(testScheduler)))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value.events is Load.Error)
        fake.failing = false
        vm.retry(); advanceUntilIdle()
        val content = vm.state.value.events as Load.Content
        assertEquals(2, content.data.size)
        assertEquals(2, vm.state.value.categories.size)
    }

    @Test
    fun homeFilterByCategoryAndSelection() = runTest {
        val vm = HomeViewModel(FakeProviderAdapter(), TestScope(StandardTestDispatcher(testScheduler)))
        vm.load(); advanceUntilIdle()
        vm.selectEvent("e2")
        assertEquals("e2", vm.state.value.selectedEventId)
        vm.selectFilter(HomeFilter.Sport(vm.state.value.categories.first())); advanceUntilIdle()
        val content = vm.state.value.events as Load.Content
        assertEquals(listOf("e1"), content.data.map { it.id })
        assertEquals(null, vm.state.value.selectedEventId)
    }

    @Test
    fun streamsErrorThenRetry() = runTest {
        val fake = FakeProviderAdapter(failing = true)
        val event = FakeProviderAdapter().events(dev.fstream.tv.provider.EventFilter.ALL).first()
        val vm = StreamsViewModel(fake, event, TestScope(StandardTestDispatcher(testScheduler)))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.state.value is Load.Error)
        fake.failing = false
        vm.retry(); advanceUntilIdle()
        assertEquals(2, (vm.state.value as Load.Content).data.size)
    }
}
