package evans18.realestatebrowser.ui.fragment.estate

import android.util.Log
import androidx.lifecycle.*
import com.squareup.picasso.Picasso
import evans18.realestatebrowser.data.model.Estate
import evans18.realestatebrowser.data.model.local.Resource
import evans18.realestatebrowser.data.network.PATH_HOUSE
import evans18.realestatebrowser.data.repository.EstatesRepository
import evans18.realestatebrowser.factory.NetworkRequestViewModelFactory
import evans18.realestatebrowser.util.TAG
import evans18.realestatebrowser.util.parallelForEach
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.URLBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//todo: would be better if HttpClient was exposed only to repositories and ViewModels react to those repositories
class EstatesViewModel(private val httpClient: HttpClient) : ViewModel() {

    private companion object {
        const val ESTATE_IMAGE_SIZE = 400
    }

    /**
     * LiveData subscribed to the single source of truth carrier for the latest Estates within the Estate repository/data source.
     * Note: Always [Resource.Success] regardless of collection content as empty or non-empty are both valid collection states.
     */
    private val wrappedLatestEstates: LiveData<Resource.Success<Set<Estate>>> = Transformations.map(EstatesRepository.setEstate) {
        Resource.Success(it)
    }

    /**
     * Local mutable LiveData receiving request status depicting ongoing status of estate list request.
     * Ie.:
     * Loading: [Resource.Loading]
     * Error: [Resource.Error]
     * Success: [Resource.Success]
     */
    private val _stateEstateRequest = MutableLiveData<Resource<Set<Estate>>>()

    val estateListRequestObservable: LiveData<Resource<Set<Estate>>> = Transformations.switchMap(_stateEstateRequest) {
        when (it) {
            is Resource.Success -> {
                //subscribe to repository's latest changes
                wrappedLatestEstates
                        as LiveData<Resource<Set<Estate>>> //cast necessary because Resource.Success's super-type, Resource, is not inferred for some reason

            }
            else ->
                //get value (don't switch for Loading or Error)
                MutableLiveData(it)  //todo: can't pass the original livedata? why?
        }
    }

    init {
        viewModelScope.launch {
            requestEstates()
        }
    }

    /**
     * Method performs GET request retrieving and deserializing List of [Estate].
     * Directly after for each [Estate.imagePath] URL performed are in parallel requests to get the image as a [Bitmap].
     * Once all requests are completed only then is the local list for [estateListRequestObservable] populated via its mutable backing property ([_stateEstateRequest]).
     */
    private suspend fun requestEstates() = coroutineScope {
        _stateEstateRequest.postValue(Resource.Loading())
        try {
            httpClient.get<List<Estate>> {
                url(PATH_HOUSE)
            }.apply {
                withContext(Dispatchers.IO) {
                    parallelForEach {
                        Log.d(TAG, "Parallel - Parallel ForEach #${it.id}")

                        URLBuilder(
                            host = NetworkRequestViewModelFactory.BASE_IMAGE_HOSTING_URL,
                            protocol = NetworkRequestViewModelFactory.BASE_URL_PROTOCOL,
                            encodedPath = it.imagePath
                        ).buildString().let { urlString ->
                            it.image = Picasso.get().load(urlString).resize(ESTATE_IMAGE_SIZE, ESTATE_IMAGE_SIZE).get()
                        }
                    }
                    Log.d(TAG, "Parallel - FINISHED")
                }

                Log.d(TAG, "Received estates list (size: $size)")
            }.let {
                // delay(2500) //todo uncomment if you want to enjoy the Shimmer anim for longer ;)
                EstatesRepository.insert(*it.toTypedArray()) //insert into repository
                _stateEstateRequest.postValue(Resource.Success()) //prompt Success
            }
        } catch (cause: Throwable) {
            Log.e(TAG, "Error getting estates: $cause", cause)
            _stateEstateRequest.postValue(Resource.Error("Couldn't fetch estates."))
        }
    }

    fun promptEstateClicked(estateId: Int) {
        val value = estateListRequestObservable.value!!
        if (value !is Resource.Success) {
            throw IllegalStateException("Selecting Estate unsupported while estate lists is not in a state of being populated.")
        }
        //check if exists
        if (value.data!!.all {
                it.id != estateId
            }) {
            throw IllegalArgumentException("Selected was an Estate which doesn't exist in collection.")
        }

        _navigateToEventDetailsById.value = Event(estateId)  // Trigger the event by setting a new Event as a new value
    }

    private val _navigateToEventDetailsById = MutableLiveData<Event<Int>>()

    val navigateToDetails: LiveData<Event<Int>> = _navigateToEventDetailsById


}

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 *
 * Reference: Courtesy of Jose Alcérreca, Google dev, over this blogpost:
 * https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150)
 */
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null
        else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}