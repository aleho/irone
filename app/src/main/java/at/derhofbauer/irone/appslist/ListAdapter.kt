/*
 * Copyright © 2018 Alexander Hofbauer
 *
 * This file is part of IronE.
 *
 * IronE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IronE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IronE.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.derhofbauer.irone.appslist

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import at.derhofbauer.irone.R

class ListAdapter(private val mContext: Context) : ArrayAdapter<AppEntry>(mContext, R.layout.listview_apps_item)
{
    private val mInflater: LayoutInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private var mIsEnabled = false

    /**
     * View holder for convert views.
     */
    private class ViewHolder(
        internal var position: Int,
        internal var image: ImageView,
        internal var text: TextView,
        internal var toggle: SwitchCompat
    ) {
        internal var loadTask: AsyncLoader? = null

        internal fun setViewsEnabled(enabled: Boolean) {
            text.isEnabled   = enabled
            image.isEnabled  = enabled
            toggle.isEnabled = enabled
        }
    }

    /**
     * Params for the async item loader.
     */
    private class TaskParams internal constructor(
        internal val context: Context,
        internal val holder: ViewHolder,
        internal val appEntry: AppEntry
    )

    /**
     * Async loader for images.
     */
    private class AsyncLoader : AsyncTask<TaskParams, Void, Drawable>() {
        private var position: Int = 0
        private lateinit var holder: ViewHolder

        override fun doInBackground(vararg taskParams: TaskParams): Drawable? {
            val params = taskParams[0]
            holder     = params.holder
            position   = holder.position

            return params.appEntry.getIcon(params.context, params.context.packageManager)
        }

        override fun onPostExecute(drawable: Drawable?) {
            super.onPostExecute(drawable)

            if (holder.position == position && !isCancelled) {
                holder.image.visibility = View.VISIBLE
                holder.image.setImageDrawable(drawable)
            }
        }
    }

    fun setData(data: List<AppEntry>?) {
        clear()

        if (data != null) {
            addAll(data)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View

        if (convertView == null) {
            view = mInflater.inflate(R.layout.listview_apps_item, parent, false)

            holder = ViewHolder(
                position,
                view.findViewById(R.id.list_icon),
                view.findViewById(R.id.list_text),
                view.findViewById(R.id.list_switch)
            )

            view.tag = holder

        } else {
            view   = convertView
            holder = convertView.tag as ViewHolder

            if (holder.loadTask != null) {
                holder.loadTask?.cancel(true)
                holder.loadTask = null
            }
        }

        holder.position = position
        val item = getItem(position)

        if (item == null) {
            view?.isEnabled = false
            holder.setViewsEnabled(false)
            return view
        }

        val setting = item.getAppSetting(mContext.packageManager)
        val label = item.getLabel(mContext.packageManager)

        view.isEnabled = mIsEnabled
        holder.setViewsEnabled(mIsEnabled)

        holder.text.text = label
        holder.image.setImageDrawable(null)

        holder.loadTask = AsyncLoader()
        holder.loadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, TaskParams(mContext, holder, item))

        // remove listener first to never trigger state changes
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = setting?.isEnabled ?: false
        holder.toggle.setOnCheckedChangeListener { _, isChecked -> setting?.apply {
            isEnabled = isChecked
            persist()
        }}

        return view
    }

    fun setEnabled(enabled: Boolean) {
        if (mIsEnabled == enabled) {
            return
        }

        mIsEnabled = enabled
        notifyDataSetChanged()
    }

    override fun isEnabled(position: Int): Boolean {
        return mIsEnabled
    }

    override fun areAllItemsEnabled(): Boolean {
        return mIsEnabled
    }
}
