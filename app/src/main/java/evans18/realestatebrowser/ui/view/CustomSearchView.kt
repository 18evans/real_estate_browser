package evans18.realestatebrowser.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import evans18.realestatebrowser.R
import kotlinx.android.synthetic.main.custom_search_input.view.*


/**
 * Custom view encapsulating an [EditText] and some action buttons mimicking a [SearchView] widget.
 *
 * Unlike [SearchView] buttons positioning is configured to be specific to the design of the application. //todo: add helper methods to adjust button positioning (make more generic).
 */
@SuppressLint("ClickableViewAccessibility")
class CustomSearchView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : ConstraintLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    //    var onTextChange: ((CharSequence) -> Unit)? = null
    private var onButtonStateShouldChange: ((Boolean, Boolean) -> Unit)? = { isBlank, hasFocus ->
        if (!isBlank) isClearable = true
        else if (!hasFocus) isClearable = false //blank AND no focus
        else {
            iv_search.isVisible = false
            btn_clear.isVisible = false
            // isClearable will consequently default to false getter
        }

    }

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomSearchView,
            0, 0
        )
        val hint = typedArray.getString(R.styleable.CustomSearchView_hint)
        val elevation = typedArray.getDimension(R.styleable.CustomSearchView_elevation, 0f)
        typedArray.recycle()

        LayoutInflater.from(context).inflate(R.layout.custom_search_input, this, true).apply {
            this.elevation = elevation
            hint?.let { search_input.hint = it }

            btn_clear.setOnClickListener {
                search_input.text.clear()
            }

            iv_search.setOnTouchListener { _, motionEvent ->
                search_input.onTouchEvent(motionEvent)
            }

            search_input.setOnFocusChangeListener { view, hasFocus ->
                val editText = (view as EditText)
                val isBlank = editText.text.isBlank()

                onButtonStateShouldChange?.invoke(isBlank, hasFocus)
                this.onFocusChangeListener.onFocusChange(view, hasFocus) //invoke focus listener of parent
            }
        }

    }

    private var isClearable
        get() = search_input.text.trim().isNotEmpty()
        private set(value) {
            iv_search.isVisible = !value
            btn_clear.isVisible = value
        }

    /**
     * Exposes [SearchView]-like listener for the use of this edit text.
     * Supports on [EditText.doAfterTextChanged] of the local [search_input] [EditText],
     * and also on "enter" click of the Android soft keyboard action [EditorInfo.IME_ACTION_SEARCH]
     * via [EditText.setOnEditorActionListener].
     */
    public fun setOnQueryTextListener(onQueryTextListener: SearchView.OnQueryTextListener) {
        //on soft-keyboard submit
        search_input.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onQueryTextListener.onQueryTextSubmit((view as EditText).text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        //on text change
        search_input.doAfterTextChanged {
            onButtonStateShouldChange?.invoke(it!!.isBlank(), true)
            onQueryTextListener.onQueryTextChange(it!!.toString())
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = CustomOutline(w, h)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class CustomOutline internal constructor(var width: Int, var height: Int) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, width, height, view.resources.getDimension(R.dimen.default_corner_radius))
        }

    }


}