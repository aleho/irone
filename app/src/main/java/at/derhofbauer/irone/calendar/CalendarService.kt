package at.derhofbauer.irone.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Reminders
import android.util.Log

import java.text.DateFormat
import java.util.Calendar
import java.util.HashSet
import java.util.NoSuchElementException
import java.util.TimeZone
import java.util.concurrent.ConcurrentLinkedDeque

import at.derhofbauer.irone.notification.AppNotification
import at.derhofbauer.irone.notification.NotificationHandler
import at.derhofbauer.irone.query_handler.QueryHandler

class CalendarService private constructor(contentResolver: ContentResolver) : NotificationHandler()
{
    companion object {
        private const val TAG = "CalendarService"

        private const val CALENDAR_OWNER_ACCOUNT = "at.derhofbauer.irone"
        private const val CALENDAR_ACCOUNT_NAME  = "IronE"
        private const val CALENDAR_DISPLAY_NAME  = "Steel HR"

        private const val EVENT_REMOVE_TIMEOUT = 30 * 1000
        private const val EVENT_END_OFFSET     = 1000

        private const val CALENDAR_ID_INDEX  = 0
        private val CALENDAR_PROJECTION      = arrayOf(Calendars._ID)
        private const val CALENDAR_SELECTION = "((${Calendars.ACCOUNT_TYPE} = ?) AND (${Calendars.ACCOUNT_NAME} = ?) AND (${Calendars.OWNER_ACCOUNT} = ?))"
        private val CALENDAR_SELECTION_ARGS  = arrayOf(
            CalendarContract.ACCOUNT_TYPE_LOCAL,
            CALENDAR_ACCOUNT_NAME,
            CALENDAR_OWNER_ACCOUNT
        )

        @Volatile
        private var sInstance: CalendarService? = null

        fun getInstance(resolver: ContentResolver): CalendarService {
            return sInstance ?: synchronized(this) {
                sInstance ?: CalendarService(resolver).also { sInstance = it }
            }
        }
    }

    private val mHandler: Handler = Handler()
    private val mQueryHandler: QueryHandler = QueryHandler(contentResolver)
    private var mCalendarId: Long = 0

    @get:Synchronized
    private var isRetrievingCalendar = false
    private val mNotifications = HashSet<String>()

    private class Callback(
        val onSuccess: ((Long) -> Unit)?,
        val onError: ((RuntimeException?) -> Unit)?
    )

    private val mCallbackQueue = ConcurrentLinkedDeque<Callback>()


    private fun buildCalendarAttributes(): ContentValues {
        val values = ContentValues()

        values.put(Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
        values.put(Calendars.VISIBLE, 1)
        values.put(Calendars.SYNC_EVENTS, 0)

        return values
    }

    private fun processCallbackQueue(exception: RuntimeException?) {
        try {
            var callback = mCallbackQueue.pop()

            while (callback != null) {
                if (mCalendarId > 0) {
                    callback.onSuccess?.invoke(mCalendarId)
                } else {
                    callback.onError?.invoke(exception)
                }

                callback = mCallbackQueue.pop()
            }
        } catch (e: NoSuchElementException) {
            // all done, continue
        }
    }

    /**
     * Creates the "sync" calendar we'll use.
     */
    private fun createCalendar() {
        if (mCalendarId > 0) {
            // we should actually never reach this point
            Log.w(TAG, "Calendar exists, ID=$mCalendarId")

            return
        }

        val values = buildCalendarAttributes()

        // these values can only be written using sync adapter URI below
        values.put(Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
        values.put(Calendars.OWNER_ACCOUNT, CALENDAR_OWNER_ACCOUNT)
        values.put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        values.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_NONE)
        values.put(Calendars.ALLOWED_REMINDERS, Reminders.METHOD_ALERT)

        val uri = Calendars.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()

        mQueryHandler.insert(uri, values,
            { insertUri: Uri ->
                mCalendarId = java.lang.Long.parseLong(insertUri.lastPathSegment)

                Log.d(TAG, "Created calendar, ID=$mCalendarId")
                processCallbackQueue(null)
            },
            {
                Log.e(TAG, "Could not create calendar", it)
                processCallbackQueue(it)
            }
        )
    }

    /**
     * Ensures we've got a calendar ID.
     */
    private fun runWithCalendarId(callback: Callback) {
        if (mCalendarId > 0) {
            callback.onSuccess?.invoke(mCalendarId)

            return
        }

        mCallbackQueue.add(callback)

        if (isRetrievingCalendar) {
            return
        }

        isRetrievingCalendar = true

        mQueryHandler.query(Calendars.CONTENT_URI,
            CALENDAR_PROJECTION,
            CALENDAR_SELECTION,
            CALENDAR_SELECTION_ARGS,
            Calendars._ID + " ASC",
            { cursor: Cursor ->
                cursor.moveToNext()
                mCalendarId = cursor.getLong(CALENDAR_ID_INDEX)

                Log.d(TAG, "Found calendar, ID=$mCalendarId")
                processCallbackQueue(null)
            },
            {
                if (it != null) {
                    Log.w(TAG, "Could not query calendars", it)
                } else {
                    this.createCalendar()
                }
            }
        )
    }

    /**
     * Inserts a reminder for an event ID to actually get a notification.
     *
     * @param eventId The previously inserted event ID.
     */
    private fun insertReminder(eventId: Long, callback: Callback) {
        val reminder = ContentValues()
        reminder.put(Reminders.EVENT_ID, eventId)
        reminder.put(Reminders.MINUTES, 2)
        reminder.put(Reminders.METHOD, Reminders.METHOD_ALERT)

        mQueryHandler.insert(
            Reminders.CONTENT_URI,
            reminder,
            { uri: Uri -> callback.onSuccess?.invoke(java.lang.Long.parseLong(uri.lastPathSegment)) },
            { callback.onError?.invoke(it) }
        )
    }

    /**
     * Inserts an event simulating a notification (includes a reminder).
     *
     * @param calendarId The calendar ID to use
     * @param title Title of the event
     * @param location Location of the event
     * @param onSuccess On success callback
     * @param onError On error callback
     */
    private fun insertEvent(
        calendarId: Long,
        title: String?,
        location: String?,
        onSuccess: (eventId: Long, reminderId: Long) -> Unit,
        onError: ((RuntimeException?) -> Unit)?
    ) {
        val beginTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()

        beginTime.add(Calendar.MINUTE, 1)
        beginTime.set(Calendar.SECOND, 0)
        endTime.timeInMillis = beginTime.timeInMillis + EVENT_END_OFFSET

        val beginMs = beginTime.timeInMillis
        val endMs = endTime.timeInMillis

        val event = ContentValues()
        event.put(Events.CALENDAR_ID, calendarId)
        event.put(Events.DTSTART, beginMs)
        event.put(Events.DTEND, endMs)
        event.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        event.put(Events.TITLE, title ?: "")
        event.put(Events.EVENT_LOCATION, location ?: "")

        mQueryHandler.insert(Events.CONTENT_URI, event,
            { uri: Uri ->
                val eventId = java.lang.Long.parseLong(uri.lastPathSegment)

                val eventUid = eventId.toString() + "-" + beginMs + "-" + endMs
                val eventKey = title + "@" + DateFormat.getTimeInstance(DateFormat.SHORT).format(beginTime.time)
                mNotifications.add(eventUid)
                mNotifications.add(eventKey)
                Log.d(TAG, "uid: $eventUid, key: $eventKey")

                insertReminder(eventId, Callback(
                    { reminderId: Long -> onSuccess.invoke(eventId, reminderId) }, onError
                ))
            },
            { onError?.invoke(it) }
        )
    }

    /**
     * Adds an event and a reminder, producing a notification that will be handled by HealthMate.
     *
     * @param calendarId The calendar ID to use
     * @param mainContent Main text
     * @param secondaryContent Secondary text
     */
    private fun doAddEvent(calendarId: Long, mainContent: String, secondaryContent: String) {
        insertEvent(calendarId, mainContent, secondaryContent,
            { eventId: Long, reminderId: Long ->
                Log.d(
                    TAG, "Event added, ID=" + eventId
                    + " (reminder ID=" + reminderId
                    + "), main: " + mainContent
                    + ", secondary: " + secondaryContent
                )

                mHandler.postDelayed({
                    removeEvent(eventId,
                        { Log.d(TAG, "Event $eventId deleted") },
                        { Log.e(TAG, "Could not delete event $eventId") }
                    )
                }, EVENT_REMOVE_TIMEOUT.toLong())
            },

            { Log.e(TAG, "Could not insert event") }
        )
    }

    private fun removeEvent(
        eventId: Long,
        onSuccess: (() -> Unit)?,
        onError: ((RuntimeException?) -> Unit)?
    ) {
        mQueryHandler.delete(
            ContentUris.withAppendedId(Events.CONTENT_URI, eventId),
            null,
            null,
            { _: Int -> onSuccess?.invoke() },
            { onError?.invoke(it) }
        )
    }

    /**
     * Deletes all calendars matching account name and account type.
     */
    private fun deleteCalendar() {
        mCalendarId = 0

        mQueryHandler.delete(
            Calendars.CONTENT_URI, CALENDAR_SELECTION, CALENDAR_SELECTION_ARGS,
            { rows: Int -> Log.d(TAG, "Deleted $rows calendars") },
            null
        )
    }

    override fun addNotification(notification: AppNotification): Int {
        val message = notification.strings

        addEvent(message[0], message[1])

        return RESULT_SUCCESS
    }

    /**
     * Adds an event and a reminder, producing a notification that will be handled by HealthMate.
     *
     * @param mainContent Main text to push. This can be at max CalendarService::TEXT_MAX_LENGTH.
     * @param secondaryContent Secondary info (shown after hyphen, if enough space left). This is cut off by HealthMate.
     */
    private fun addEvent(mainContent: String, secondaryContent: String) {
        runWithCalendarId(Callback(
            { calendarId: Long -> doAddEvent(calendarId, mainContent, secondaryContent) },
            { Log.e(TAG, "No calendar, can't add event") }
        ))
    }

    fun isOwnEvent(uid: String?, title: String?, time: String?): Boolean {
        if (uid != null && uid.isNotEmpty()) {
            val parts = uid.split("-")

            if (parts.size > 2) {
                val eventUid = parts[0] + "-" + parts[1] + "-" + parts[2]

                if (mNotifications.contains(eventUid)) {
                    mNotifications.remove(eventUid)
                    return true
                }
            }
        }

        if (title != null && title.isNotEmpty()
            && time != null && time.isNotEmpty()) {
            val eventKey = "$title@$time"

            if (mNotifications.contains(eventKey)) {
                mNotifications.remove(eventKey)
                return true
            }
        }

        Log.d(TAG, "not our event, uid=$uid, title=$title time=$time")

        return false
    }

    /**
     * Deletes our calendars.
     */
    fun cleanUp() {
        deleteCalendar()
    }
}
