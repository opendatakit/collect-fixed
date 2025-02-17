package org.odk.collect.entities.browser

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.odk.collect.async.Scheduler
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.entities.storage.Entity

class EntitiesViewModel(
    private val scheduler: Scheduler,
    private val entitiesRepository: EntitiesRepository
) : ViewModel() {

    private val _lists = MutableLiveData<List<String>>(emptyList())
    val lists: LiveData<List<String>> = _lists

    init {
        scheduler.immediate {
            _lists.postValue(entitiesRepository.getLists().toList())
        }
    }

    fun getEntities(list: String): LiveData<List<Entity.Saved>> {
        val result = MutableLiveData<List<Entity.Saved>>(emptyList())
        scheduler.immediate {
            result.postValue(entitiesRepository.query(list))
        }

        return result
    }
}
