package nl.sogeti.android.gpstracker.ng.common.abstractpresenters

import android.content.Context
import nl.sogeti.android.gpstracker.ng.common.GpsTrackerApplication
import nl.sogeti.android.gpstracker.ng.dagger.AppComponent
import nl.sogeti.android.gpstracker.ng.rules.MockAppComponentTestRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit

class ContextedPresenterTest {

    @get:Rule
    var mockitoRule = MockitoJUnit.rule()
    @get:Rule
    var appComponentRule = MockAppComponentTestRule()
    @Mock
    lateinit var mockContext: Context
    @Mock
    lateinit var mockAppComponent: AppComponent

    @Test
    fun start() {
        // Prepare
        val sut = MyContextedPresenter()

        // Execute
        sut.start(mockContext)

        // Verify
        assertTrue(sut.didStart)
        assertEquals(sut.context, mockContext)
    }

    @Test
    fun stop() {
        // Prepare
        val sut = MyContextedPresenter()
        sut.start(mockContext)

        // Execute
        sut.stop()

        // Verify
        assertTrue(sut.willStop)
        assertNull(sut.context)
    }

    class MyContextedPresenter : ContextedPresenter() {
        var willStop = false
        var didStart = false

        override fun willStop() {
            willStop = true
        }

        override fun didStart() {
            didStart = true
        }
    }

}
