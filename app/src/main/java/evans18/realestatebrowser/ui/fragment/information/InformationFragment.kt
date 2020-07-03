package evans18.realestatebrowser.ui.fragment.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import evans18.realestatebrowser.R
import kotlinx.android.synthetic.main.fragment_information.*

class InformationFragment : Fragment() {

    private lateinit var informationViewModel: InformationViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        informationViewModel = ViewModelProvider(this).get(InformationViewModel::class.java)
        return inflater.inflate(R.layout.fragment_information, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        informationViewModel.description.observe(viewLifecycleOwner, Observer {
            tv_description.text = it
        })
    }
}