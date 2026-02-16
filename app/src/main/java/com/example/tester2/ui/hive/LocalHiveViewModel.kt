package com.example.tester2.ui.hive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.model.Topic
import com.example.tester2.data.repository.TopicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocalHiveViewModel @Inject constructor(
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _popularTopics = MutableStateFlow<List<Topic>>(emptyList())
    val popularTopics = _popularTopics.asStateFlow()

    init {
        fetchTopics()
    }
    
    fun fetchTopics() {
        viewModelScope.launch {
            topicRepository.getPopularTopics().collect { topics ->
                _popularTopics.value = topics.ifEmpty { 
                    // Fallback to dummy data if empty for demo
                    getDummyPopularTopics() 
                }
            }
        }
    }
    
    private fun getDummyPopularTopics(): List<Topic> {
        return listOf(
            Topic("d1", "Traffic", 0.0, 0.0, 500, true, 83),
            Topic("d2", "Late Night Food", 0.0, 0.0, 500, true, 42),
            Topic("d3", "Lost Dog", 0.0, 0.0, 500, true, 3),
            Topic("d4", "Construction Noise", 0.0, 0.0, 500, true, 15),
            Topic("d5", "Weather", 0.0, 0.0, 500, true, 8)
        )
    }
}
