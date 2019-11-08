package com.example.student_camera.camera

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.student_camera.database.Photo
import com.example.student_camera.database.PhotoDatabaseDao
import kotlinx.coroutines.*
import java.util.*

class CameraViewModelFactory(
    private val dataSource: PhotoDatabaseDao,
    private val application: Application) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            return CameraViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// viewModel
class CameraViewModel(
    val database: PhotoDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private val _lastPhoto = MutableLiveData<Photo>()
    val lastPhoto: LiveData<Photo>
        get() = _lastPhoto

    fun insert(uri: String) {
        uiScope.launch {
            val now = Date()
//            todo timeCellNum, dayCellNumを判別するコードが必要
//            1: monday, 7: sunday
            var newPhoto = Photo(0, uri, 1, 1, now)
            _lastPhoto.value = newPhoto

            Log.d("insert", _lastPhoto.value.toString())
        }
    }

    fun clear() {
        uiScope.launch {
            _clear()
        }

    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    private suspend fun _insert(photo: Photo) {
        withContext(Dispatchers.IO) {
            database.insert(photo)
        }
    }

    private suspend fun _clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

}