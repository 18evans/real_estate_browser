package evans18.realestatebrowser.ui.fragment.estate.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import evans18.realestatebrowser.data.model.Estate
import evans18.realestatebrowser.data.repository.EstatesRepository

class EstateDetailViewModel : ViewModel() {

    private val selectedEstateId = MutableLiveData<Int>()
    val estate: LiveData<Estate> = Transformations.switchMap(selectedEstateId) { selectedEstateId ->
        //subscribe to the repository's livedata in case that data gets changed/refreshed
        //todo: should probably indicate some progrss when this happens or just disallow it until user refreshes fragment
        Transformations.map(EstatesRepository.setEstate) { estates ->
            estates.first {
                it.id == selectedEstateId
            }
        }
    }

    fun setSelectedEstateId(estateId: Int) {
        selectedEstateId.value = estateId
    }

}
