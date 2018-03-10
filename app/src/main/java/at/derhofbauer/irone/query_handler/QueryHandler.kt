package at.derhofbauer.irone.query_handler

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import java.util.concurrent.ConcurrentHashMap

class QueryHandler(cr: ContentResolver) : AsyncQueryHandler(cr)
{
    companion object {
        private const val TAG = "QueryHandler"

        /** @see AsyncQueryHandler */
        private const val EVENT_ARG_QUERY  = 1
        private const val EVENT_ARG_INSERT = 2
        private const val EVENT_ARG_UPDATE = 3
        private const val EVENT_ARG_DELETE = 4
    }

    private open class Callback

    private class CursorCallback(
        val onSuccess: ((Cursor) -> Unit)?,
        val onError: ((RuntimeException?) -> Unit)?
    ) : Callback()

    private class UriCallback(
        val onSuccess: ((Uri) -> Unit)?,
        val onError: ((RuntimeException?) -> Unit)?
    ) : Callback()

    private class RowsCallback(
        val onSuccess: ((Int) -> Unit)?,
        val onError: ((RuntimeException?) -> Unit)?
    ) : Callback()


    private val mCallbacks = ConcurrentHashMap<Int, Callback>()
    private var mTokenCounter = 0

    /**
     * Makes sure we get a valid token.
     *
     * @return The token integer.
     */
    private val nextToken: Int
        @Synchronized get() = ++mTokenCounter

    /**
     * Returns and deletes callbacks for a token.
     *
     * @param token The query token.
     * @return The callbacks or null.
     */
    private inline fun <reified T> getCallbackForToken(token: Int): T? {
        val callback = mCallbacks[token]

        if (callback != null) {
            mCallbacks.remove(token)
        }

        return callback as T?
    }


    private inner class WorkerHandler(looper: Looper) : AsyncQueryHandler.WorkerHandler(looper) {
        override fun handleMessage(msg: Message) {
            try {
                super.handleMessage(msg)

            } catch (exception: RuntimeException) {
                val args  = msg.obj as WorkerArgs
                val reply = args.handler.obtainMessage(msg.what)
                with (reply) {
                    obj  = args
                    arg1 = msg.arg1
                }

                args.result = exception

                Log.w(TAG, "WorkerHandler.handleMessage threw exception", exception)
                reply.sendToTarget()
            }
        }
    }

    /**
     * Returns our own worker handler that knows about exceptions.
     */
    override fun createHandler(looper: Looper): Handler {
        return WorkerHandler(looper)
    }

    /**
     * Handles exception messages. Passes all others to parent.
     */
    override fun handleMessage(msg: Message) {
        if (msg.obj is AsyncQueryHandler.WorkerArgs) {
            val args      = msg.obj as AsyncQueryHandler.WorkerArgs
            val exception = args.result

            if (exception is RuntimeException) {
                val event  = msg.arg1
                val token  = msg.what

                when (event) {
                    EVENT_ARG_QUERY  -> onQueryCompleteLocal(token, null, exception)
                    EVENT_ARG_INSERT -> onInsertCompleteLocal(token, null, exception)
                    EVENT_ARG_UPDATE -> onUpdateCompleteLocal(token, -1, exception)
                    EVENT_ARG_DELETE -> onDeleteCompleteLocal(token, -1, exception)
                }

                return
            }
        }

        super.handleMessage(msg)
    }


    private fun onQueryCompleteLocal(token: Int, cursor: Cursor?, exception: RuntimeException?)
    {
        val callback = getCallbackForToken<CursorCallback>(token) ?: return

        if (cursor != null && cursor.count > 0) {
            callback.onSuccess?.invoke(cursor)
        } else {
            callback.onError?.invoke(exception)
        }
    }

    private fun onInsertCompleteLocal(token: Int, uri: Uri?, exception: RuntimeException?) {
        val callback = getCallbackForToken<UriCallback>(token) ?: return

        if (uri != null) {
            callback.onSuccess?.invoke(uri)
        } else {
            callback.onError?.invoke(exception)
        }
    }

    private fun onUpdateCompleteLocal(token: Int, result: Int, exception: RuntimeException?) {
        val callback = getCallbackForToken<RowsCallback>(token) ?: return

        if (result > 0) {
            callback.onSuccess?.invoke(result)
        } else {
            callback.onError?.invoke(exception)
        }
    }

    private fun onDeleteCompleteLocal(token: Int, result: Int, exception: RuntimeException?) {
        val callback = getCallbackForToken<RowsCallback>(token) ?: return

        if (result > 0) {
            callback.onSuccess?.invoke(result)
        } else {
            callback.onError?.invoke(exception)
        }
    }

    override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
        onQueryCompleteLocal(token, cursor, null)
    }

    override fun onInsertComplete(token: Int, cookie: Any?, uri: Uri?) {
        onInsertCompleteLocal(token, uri, null)
    }

    override fun onUpdateComplete(token: Int, cookie: Any?, result: Int) {
        onUpdateCompleteLocal(token, result, null)
    }

    override fun onDeleteComplete(token: Int, cookie: Any?, result: Int) {
        onDeleteCompleteLocal(token, result, null)
    }


    /**
     * @see AsyncQueryHandler::startQuery
     */
    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        orderBy: String?,
        callback: ((Cursor) -> Unit)?,
        empty: ((RuntimeException?) -> Unit)?
    ) {
        val token = nextToken

        if (callback != null) {
            mCallbacks[token] = CursorCallback(callback, empty)
        }

        Log.d(TAG, "query: $uri, $projection, sel=$selection, args=${selectionArgs?.joinToString(",", "[", "]")}, order=$orderBy")
        super.startQuery(token, null, uri, projection, selection, selectionArgs, orderBy)
    }

    /**
     * @see AsyncQueryHandler::startInsert
     */
    fun insert(
        uri: Uri,
        initialValues: ContentValues,
        callback: ((Uri) -> Unit)?,
        empty: ((RuntimeException?) -> Unit)?
    ) {
        val token = nextToken

        if (callback != null) {
            mCallbacks[token] = UriCallback(callback, empty)
        }

        Log.d(TAG, "insert: $uri, vals=${initialValues.size()}")
        super.startInsert(token, null, uri, initialValues)
    }

    /**
     * @see AsyncQueryHandler::startUpdate
     */
    fun update(
        uri: Uri,
        values: ContentValues,
        selection: String?,
        selectionArgs: Array<String>?,
        callback: ((Int) -> Unit)?,
        empty: ((RuntimeException?) -> Unit)?
    ) {
        val token = nextToken

        if (callback != null) {
            mCallbacks[token] = RowsCallback(callback, empty)
        }

        Log.d(TAG, "update: $uri, vals=${values.size()}, sel=$selection, args=${selectionArgs?.joinToString(",", "[", "]")}")
        super.startUpdate(token, null, uri, values, selection, selectionArgs)
    }

    /**
     * @see AsyncQueryHandler::startDelete
     */
    fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
        callback: ((Int) -> Unit)?,
        empty: ((RuntimeException?) -> Unit)?
    ) {
        val token = nextToken

        if (callback != null) {
            mCallbacks[token] = RowsCallback(callback, empty)
        }

        Log.d(TAG, "delete: $uri, sel=$selection, args=${selectionArgs?.joinToString(",", "[", "]")}")
        super.startDelete(token, null, uri, selection, selectionArgs)
    }
}
