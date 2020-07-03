package evans18.realestatebrowser.ui.fragment.estate.detail.map

import android.R
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.maps.SupportMapFragment

/**
 * Workaround for [SupportMapFragment] touch gestures if [SupportMapFragment] is child of a [ScrollView].
 */
class NestedScrollViewMapFragment : SupportMapFragment() {

    private lateinit var onTouchListener: () -> Unit

    override fun onCreateView(layoutInflater: LayoutInflater, viewGroup: ViewGroup?, savedInstance: Bundle?): View? {
        return super.onCreateView(layoutInflater, viewGroup, savedInstance).apply {
            val frameLayout = TouchableWrapper(activity)
            frameLayout.setBackgroundColor(resources.getColor(R.color.transparent, requireActivity().theme))

            (this as ViewGroup).addView(
                frameLayout,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )
        }
    }

    fun setTouchListener(onTouchAction: () -> Unit) {
        this.onTouchListener = onTouchAction
    }

    private inner class TouchableWrapper(context: Context?) : FrameLayout(context!!) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> onTouchListener.invoke()
            }
            return super.dispatchTouchEvent(event)
        }
    }
}