import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.get
import org.w3c.dom.set

fun main() {
    var count: Int by mutableStateOf(0)


    renderComposable(rootElementId = "root") {
        val viewModel = MainViewModel()
        viewModel.fetch()
        Div({ style { padding(25.px) } }) {
            Button(attrs = {
                onClick { count -= 1 }
            }) {
                Text("-")
            }

            Span({ style { padding(15.px) } }) {
                Text("$count")
                Text("${viewModel.ubikeText}")
            }

            Button(attrs = {
                onClick { count += 1 }
            }) {
                Text("+")
            }
        }
    }
}

class MainViewModel() {
    val scope = MainScope()
    var ubikeList: List<Ubike> by mutableStateOf((emptyList()))
        private set

    var ubikeText: String by mutableStateOf("Loading...")
        private set

    fun fetch() {
        scope.storageCache(TopicEnum.UBIKE,{
            Ubike.fromJson(it)
        },{
            Ubike.toJson(it)
        },{
            WebApi.getUBikeList()
        }){
            ubikeList = it
        }.let{
            val ubike = ubikeList.takeIf { it.isNotEmpty() }?.random()?.let{
                it.stationName +":"+ it.totalCount
            } ?: "Loading..."
            ubikeText = ubike
        }
    }
}


enum class TopicEnum(val value: String){
    UBIKE("ubike")
}

fun <T> CoroutineScope.storageCache(
    topic: TopicEnum,
    loadFromJson: (String) -> T,
    objectToString: (T) -> String,
    fetchFromService: suspend () -> T,
    onUpdate: (T) -> Unit
) {
    val topicType = topic.value
    localStorage.get(topicType)?.let {
        onUpdate.invoke(loadFromJson(it))
    }

    launch {
        fetchFromService().let{ result ->
            onUpdate.invoke(result)
            objectToString(result).let{
                localStorage.set(topicType,it)
            }
        }
    }
}
