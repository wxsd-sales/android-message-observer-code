package com.example.webexandroid.messaging.spaces.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.webexandroid.BaseViewModel
import com.example.webexandroid.WebexRepository
import com.example.webexandroid.messaging.spaces.SpaceMessageModel
import com.example.webexandroid.messaging.spaces.SpaceModel
import com.example.webexandroid.messaging.spaces.SpacesRepository
import com.example.webexandroid.person.PersonModel
import com.example.webexandroid.person.PersonRepository
import com.ciscowebex.androidsdk.message.Message
import io.reactivex.android.schedulers.AndroidSchedulers

class SpaceDetailViewModel(private val spacesRepo: SpacesRepository, private val personRepo: PersonRepository, private val webexRepository: WebexRepository) : BaseViewModel() {
    private val tag = "SpaceDetailViewModel"
    lateinit var spaceId: String
    private var person: PersonModel? = null
    private val _deleteMessage = MutableLiveData<SpaceMessageModel>()
    val deleteMessage: LiveData<SpaceMessageModel> = _deleteMessage

    private val _markMessageAsReadStatus = MutableLiveData<SpaceMessageModel>()
    val markMessageAsReadStatus: LiveData<SpaceMessageModel> = _markMessageAsReadStatus

    private val _messageEventLiveData = MutableLiveData<Pair<WebexRepository.MessageEvent, Any?>>()
    val messageEventLiveData: LiveData<Pair<WebexRepository.MessageEvent, Any?>> = _messageEventLiveData

    private val _getMeData = MutableLiveData<PersonModel>()
    val getMeData: LiveData<PersonModel> = _getMeData

    init {
        getMe()
        webexRepository._messageEventLiveData = _messageEventLiveData
    }

    override fun onCleared() {
        super.onCleared()
        webexRepository._messageEventLiveData = null
    }

    fun getMe() {
        personRepo.getMe().observeOn(AndroidSchedulers.mainThread()).subscribe {
            person = it
            _getMeData.postValue(person)
            Log.e("personinfo",person.toString())
//            getMessages()
        }.autoDispose()
    }


    private val _space = MutableLiveData<SpaceModel>()
    val space: LiveData<SpaceModel> = _space

    private val _messageError = MutableLiveData<String>()
    val messageError: LiveData<String> = _messageError

    private val _spaceMessages = MutableLiveData<List<SpaceMessageModel>>()
    val spaceMessages: LiveData<List<SpaceMessageModel>> = _spaceMessages

    fun isSelfMessage(personId: String): Boolean {
        return personId == person?.personId ?: false
    }

    fun getPersonId(): String? {
        Log.e("personinfo",person?.personId)
        return person?.personId
    }

    fun observeMessages(){
        webexRepository.messageObserver().observeOn(AndroidSchedulers.mainThread()).subscribe({ message ->
            Log.e("messagereceived",message)
        }, { error -> _messageError.postValue(error?.message ?: "") }).autoDispose()
    }

    fun getSpaceById() {
        spacesRepo.fetchSpaceById(spaceId).observeOn(AndroidSchedulers.mainThread()).subscribe({ spaceModel ->
            _space.postValue((spaceModel))
        }, { _space.postValue(null) }).autoDispose()
    }

    fun getMessages() {
        spacesRepo.listMessages(spaceId).observeOn(AndroidSchedulers.mainThread()).subscribe({ messageModels ->
            _spaceMessages.postValue(messageModels)
        }, { _spaceMessages.postValue(emptyList()) }).autoDispose()
    }

    fun deleteMessage(message: SpaceMessageModel) {
        spacesRepo.deleteMessage(message.messageId).observeOn(AndroidSchedulers.mainThread()).subscribe({ success ->
            _deleteMessage.postValue(message)
        }, { error -> _messageError.postValue(error?.message ?: "") }).autoDispose()
    }

    fun markMessageAsRead(message: SpaceMessageModel) {
        spacesRepo.markMessageAsRead(spaceId, message.messageId).observeOn(AndroidSchedulers.mainThread()).subscribe({ success ->
            _markMessageAsReadStatus.postValue(message)
        }, { error -> _messageError.postValue(error?.message ?: "") }).autoDispose()
    }
}