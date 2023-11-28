package de.marmaro.krt.ffupdater.activity.add

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.dialog.CardviewOptionsDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddRecyclerView(
    private val elements: List<ItemWrapper>,
    private val activity: AppCompatActivity,
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemType(val id: Int) {
        APP(0), TITLE(1)
    }

    interface ItemWrapper {
        fun getType(): ItemType
    }

    class WrappedApp(val app: App) : ItemWrapper {
        override fun getType() = ItemType.APP
    }

    class WrappedTitle(val text: String) : ItemWrapper {
        override fun getType() = ItemType.TITLE
    }

    inner class AppHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewWithTag("title")
        val icon: ImageView = itemView.findViewWithTag("icon")
        val addAppButton: ImageButton = itemView.findViewWithTag("add_app")
    }

    inner class HeadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewWithTag("text")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ItemType.APP.id -> {
                val appView = inflater.inflate(R.layout.activity_add_app_cardview, parent, false)
                AppHolder(appView)
            }

            ItemType.TITLE.id -> {
                val titleView = inflater.inflate(R.layout.activity_add_app_title, parent, false)
                HeadingHolder(titleView)
            }

            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ItemType.APP.id -> onBindViewHolderApp(viewHolder as AppHolder, position)
            ItemType.TITLE.id -> onBindViewHolderTitle(viewHolder as HeadingHolder, position)
            else -> throw IllegalArgumentException()
        }
    }

    override fun getItemCount(): Int {
        return elements.size
    }

    override fun getItemViewType(position: Int): Int {
        return elements[position].getType().id
    }

    private fun onBindViewHolderApp(viewHolder: AppHolder, position: Int) {
        val wrappedApp = elements[position] as WrappedApp
        val app = wrappedApp.app
        val appImpl = app.findImpl()
        viewHolder.title.setText(appImpl.title)
        viewHolder.icon.setImageResource(appImpl.icon)
        viewHolder.addAppButton.setOnClickListener { showAddAppDialog(app) }
    }

    private fun showAddAppDialog(app: App) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val dialog = CardviewOptionsDialog(app)
            dialog.hideAutomaticUpdateSwitch = true
            dialog.show(activity.supportFragmentManager, activity.applicationContext)
            dialog.setFragmentResultListener(CardviewOptionsDialog.DOWNLOAD_ACTIVITY_WAS_STARTED) { _, _ ->
                activity.finish()
            }
        }
    }

    private fun onBindViewHolderTitle(viewHolder: HeadingHolder, position: Int) {
        val heading = elements[position] as WrappedTitle
        viewHolder.text.text = heading.text
    }

    @UiThread
    private fun showToast(message: Int) {
        val layout = activity.findViewById<View>(R.id.coordinatorLayout)
        Snackbar.make(layout, message, Snackbar.LENGTH_LONG).show()
    }
}