package nl.sogeti.android.gpstracker.ng.track

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns._ID
import nl.sogeti.android.gpstracker.integration.ContentConstants.Tracks.NAME
import nl.sogeti.android.gpstracker.ng.common.controllers.content.ContentController
import nl.sogeti.android.gpstracker.ng.common.controllers.content.ContentControllerFactory
import nl.sogeti.android.gpstracker.ng.model.TrackSelection
import nl.sogeti.android.gpstracker.ng.rules.MockAppComponentTestRule
import nl.sogeti.android.gpstracker.ng.rules.any
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit

class TrackPresenterTest {

    lateinit var sut: TrackPresenter
    lateinit var viewModel: TrackViewModel
    @get:Rule
    var appComponentRule = MockAppComponentTestRule()
    @get:Rule
    var mockitoRule = MockitoJUnit.rule()
    @Mock
    lateinit var view: TrackViewModel.View
    @Mock
    lateinit var contentController: ContentController
    @Mock
    lateinit var contentControllerFactory: ContentControllerFactory
    @Mock
    lateinit var context: Context
    @Mock
    lateinit var trackSelection: TrackSelection
    @Mock
    lateinit var trackUri: Uri

    @Before
    fun setUp() {
        viewModel = TrackViewModel()
        sut = TrackPresenter(viewModel, view)
        sut.context = context
        `when`(trackSelection.trackUri).thenReturn(trackUri)
        `when`(trackSelection.trackName).thenReturn("selected")
        sut.trackSelection = trackSelection
        Mockito.`when`(contentControllerFactory.createContentController(any(), any())).thenReturn(contentController)
        sut.contentControllerFactory = contentControllerFactory
    }

    @Test
    fun didStart() {
        // Act
        sut.didStart()
        // Assert
        verify(trackSelection).addListener(sut)
    }

    @Test
    fun willStop() {
        // Arrange
        sut.didStart()
        // Act
        sut.willStop()
        // Assert
        verify(trackSelection).removeListener(sut)
        verify(contentController).registerObserver(trackUri)
    }

    @Test
    fun testOptionSelected() {
        // Act
        sut.onListOptionSelected()
        // Assert
        verify(view).showTrackSelection()
    }

    @Test
    fun testAboutSelected() {
        // Act
        sut.onAboutOptionSelected()
        // Assert
        verify(view).showAboutDialog()
    }

    @Test
    fun testEditSelected() {
        // Arrange
        viewModel.trackUri.set(trackUri)
        // Act
        sut.onEditOptionSelected()
        // Assert
        verify(view).showTrackEditDialog(trackUri)
    }

    @Test
    fun testContentChange() {
        // Arrange
        val cursor = mock(Cursor::class.java)
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndex(any())).thenReturn(1)
        `when`(cursor.getString(1)).thenReturn("mockname")
        val resolver = mock(ContentResolver::class.java)
        `when`(context.contentResolver).thenReturn(resolver)
        `when`(resolver.query(any(), any(), any(), any(), any())).thenReturn(cursor)
        // Act
        sut.onChangeUriContent(trackUri, trackUri)
        // Assert
        verify(view).showTrackName("mockname")
        assertThat(viewModel.name.get(), `is`("mockname"))
    }
}
