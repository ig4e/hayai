package yokai.util.widget

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VelocityScaledLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
    private var attachedRecycler: RecyclerView? = null
    private var previousFlingListener: RecyclerView.OnFlingListener? = null

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        attachedRecycler = view
        previousFlingListener = view.onFlingListener
        view.onFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                val count = view.adapter?.itemCount ?: 0
                return view.fling(scaleFlingVelocity(velocityX, count), scaleFlingVelocity(velocityY, count))
            }
        }
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        view.onFlingListener = previousFlingListener
        previousFlingListener = null
        attachedRecycler = null
        super.onDetachedFromWindow(view, recycler)
    }
}
