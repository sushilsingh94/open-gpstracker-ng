/*------------------------------------------------------------------------------
 **     Ident: Sogeti Smart Mobile Solutions
 **    Author: René de Groot
 ** Copyright: (c) 2016 Sogeti Nederland B.V. All Rights Reserved.
 **------------------------------------------------------------------------------
 ** Sogeti Nederland B.V.            |  No part of this file may be reproduced
 ** Distributed Software Engineering |  or transmitted in any form or by any
 ** Lange Dreef 17                   |  means, electronic or mechanical, for the
 ** 4131 NJ Vianen                   |  purpose, without the express written
 ** The Netherlands                  |  permission of the copyright holder.
 *------------------------------------------------------------------------------
 *
 *   This file is part of OpenGPSTracker.
 *
 *   OpenGPSTracker is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   OpenGPSTracker is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with OpenGPSTracker.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package nl.sogeti.android.gpstracker.ng.recording

import android.location.Location
import android.net.Uri
import android.os.AsyncTask
import com.google.android.gms.maps.model.LatLng
import nl.sogeti.android.gpstracker.integration.ContentConstants
import nl.sogeti.android.gpstracker.integration.ServiceConstants.*
import nl.sogeti.android.gpstracker.ng.common.abstractpresenters.ConnectedServicePresenter
import nl.sogeti.android.gpstracker.ng.common.controllers.ContentController
import nl.sogeti.android.gpstracker.ng.utils.ResultHandler
import nl.sogeti.android.gpstracker.ng.utils.readTrack
import nl.sogeti.android.gpstracker.v2.R

class RecordingPresenter constructor(private val viewModel: RecordingViewModel) : ConnectedServicePresenter(), ContentController.ContentListener {

    private val FIVE_MINUTES_IN_MS = 5L * 60L * 1000L

    private var isReading: Boolean = false
    private var contentController: ContentController? = null
    override fun didStart() {
        super.didStart()
        contentController = ContentController(context!!, this)
        contentController?.registerObserver(viewModel.trackUri.get())
    }

    override fun willStop() {
        super.willStop()
        contentController?.unregisterObserver()
        contentController = null
    }

    //region Service connection

    override fun didConnectToService(trackUri: Uri?, name: String?, loggingState: Int) {
        updateRecording(trackUri, loggingState)
    }

    override fun didChangeLoggingState(trackUri: Uri?, name: String?, loggingState: Int) {
        updateRecording(trackUri, loggingState)
    }

    //endregion

    //region ContentController

    override fun onChangeUriContent(contentUri: Uri, changesUri: Uri) {
        if (!isReading) {
            readTrackSummary(contentUri)
        }
    }

    private fun readTrackSummary(contentUri: Uri) {
        TrackReader(contentUri, viewModel).execute()
    }

    //endregion

    //region Private

    private fun updateRecording(trackUri: Uri?, loggingState: Int) {
        if (trackUri != null) {
            contentController?.registerObserver(trackUri)
            viewModel.trackUri.set(trackUri)
        }

        val isRecording = (loggingState == STATE_LOGGING) || (loggingState == STATE_PAUSED)
        viewModel.isRecording.set(isRecording)
        if (isRecording && trackUri != null) {
            readTrackSummary(trackUri)
        }
        when (loggingState) {
            STATE_LOGGING -> viewModel.state.set(context?.getString(R.string.state_logging))
            STATE_PAUSED -> viewModel.state.set(context?.getString(R.string.state_paused))
            STATE_STOPPED -> viewModel.state.set(context?.getString(R.string.state_stopped))
        }
    }

    private inner class TrackReader internal constructor(internal val trackUri: Uri, internal val viewModel: RecordingViewModel) : AsyncTask<Void, Void, List<LatLng>>(), ResultHandler {
        internal val collectedWaypoints = mutableListOf<LatLng>()
        internal val collectedTimes = mutableListOf<Long>()
        internal var speed = 0.0

        override fun addTrack(uri: Uri, name: String) {
            viewModel.name.set(name)
            viewModel.trackUri.set(uri)
        }

        @SuppressWarnings("EmptyMethod")
        override fun addSegment() {
            /* NO-OP */
        }

        override fun addWaypoint(latLng: LatLng, millisecondsTime: Long) {
            collectedWaypoints.add(latLng)
            collectedTimes.add(millisecondsTime)
        }

        override fun onPreExecute() {
            isReading = true
        }

        override fun doInBackground(params: Array<Void>): List<LatLng> {
            val sel = ContentConstants.WaypointsColumns.TIME + " > ?"
            val args = listOf<String>(java.lang.Long.toString(System.currentTimeMillis() - FIVE_MINUTES_IN_MS))
            val selection = Pair(sel, args)
            context?.let { trackUri.readTrack(it, this, selection) }
            val waypoints = mutableListOf<LatLng>()
            var seconds = 0.0
            var meters = 0.0
            if (waypoints.size > 0) {
                for (i in 1..waypoints.size - 1) {
                    seconds += (collectedTimes[i] / 1000 - collectedTimes[i - 1] / 1000).toDouble()
                    val start = collectedWaypoints[i]
                    val end = collectedWaypoints[i - 1]
                    val result = FloatArray(1)
                    Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, result)
                    meters += result[0].toDouble()
                }
                speed = meters / 1000.0 / (seconds / 3600)
            }

            return waypoints
        }

        override fun onPostExecute(segmentedWaypoints: List<LatLng>) {
            val myContext = context
            if (myContext != null) {
                val waypoints = myContext.resources.getQuantityString(R.plurals.fragment_recording_waypoints, segmentedWaypoints.size, segmentedWaypoints.size)
                val speed = myContext.getString(R.string.fragment_recording_summary, waypoints, speed, "km/h")
                viewModel.summary.set(speed)
            }
            isReading = false
        }
    }
    //endregion
}
