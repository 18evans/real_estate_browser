package evans18.realestatebrowser.ui.fragment.information

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InformationViewModel : ViewModel() {

    private val _description = MutableLiveData<String>().apply {
        value =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam sagittis turpis eu imperdiet finibus. Maecenas facilisis tincidunt libero, ac faucibus erat aliquet nec. Ut interdum ex lacus, eu rhoncus diam consectetur a. Duis tortor orci, hendrerit vel pellentesque sit amet, aliquam eu purus. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Aliquam at tellus a massa cursus semper. Quisque nec elementum tortor."
    }
    val description: LiveData<String> = _description
}