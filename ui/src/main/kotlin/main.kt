import androidx.compose.desktop.AppManager
import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.shortcuts
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jpl7.Compound
import org.jpl7.JPL
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime
import kotlin.time.nanoseconds
import kotlin.time.seconds

data class Coordinate(val x: Int, val y: Int)

val colors get() = Colors(
    Color.Gray,
    Color.DarkGray,
    Color.White,
    Color(230, 230, 230),
    lightColors().background,
    lightColors().surface,
    lightColors().error,
    lightColors().onPrimary,
    lightColors().onSecondary,
    lightColors().onBackground,
    lightColors().onSurface,
    lightColors().onError,
    lightColors().isLight
    )

@ExperimentalTime
fun main() {
    JPL.setNativeLibraryPath("/lib/swi-prolog/lib/amd64/libjpl.so")
    game()
}

sealed class State
data class Playing(val thread: Thread? = null) : State()
object Won: State()
object Lost: State()
object Config : State()

@ExperimentalTime
fun game() {
    Window("Pathfinding") {
        var width by remember { mutableStateOf<Int?>(null) }
        var homeX by remember { mutableStateOf<Int?>(null) }
        var homeY by remember { mutableStateOf<Int?>(null) }
        var coordinates by remember { mutableStateOf<Coordinates?>(null) }
        val stateDelegate = remember { mutableStateOf<State>(Config) }
        var state by stateDelegate
        LocalAppWindow.current.events.onClose = {
            state.let {
                if (it is Playing && it.thread?.isAlive == true) {
                    it.thread.interrupt()
                }
            }
        }
        val actorDelegate = remember { mutableStateOf(Coordinate(1, 1)) }
        var actorCoordinates by actorDelegate

        MaterialTheme(colors) {
            if (state is Playing && (state as Playing).thread == null)
                state = (state as Playing).copy(thread = runPathfinding(coordinates!!, stateDelegate, actorDelegate))
            when (state) {
                is Config -> {
                    val startTheGame = {
                        coordinates = Coordinates(width!!, Coordinate(homeX!!, homeY!!))
                        state = Playing()
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.padding(100.dp).fillMaxSize()) {
                        Column {
                            Text("Board's width")
                            TextField(width?.toString() ?: "",
                                {
                                    width = it.toIntOrNull()
                                }, modifier = Modifier.shortcuts {
                                    on(Key.Enter, startTheGame)
                                }.width(220.dp))
                        }
                        Row {
                            Column {
                                Text("Home X:")
                                TextField(homeX?.toString() ?: "", {
                                    homeX = it.toIntOrNull()
                                }, modifier =  Modifier.width(100.dp))
                            }
                            Spacer(Modifier.width(20.dp))
                            Column {
                                Text("Home Y")
                                TextField(homeY?.toString() ?: "", {
                                    homeY = it.toIntOrNull()
                                }, modifier =  Modifier.width(100.dp))
                            }
                        }
                        Button(startTheGame) { Text("Start") }
                    }

                }
                is Playing -> {

                        Canvas(
                            Modifier
                                .width(gridSize * width!!)
                                .height(gridSize * width!!)
                                .offset(left.first, left.second)
                        ) {
                            drawRect(colors.secondaryVariant)
                        }
//                    for (x in 1..width) {
//                        for (y in 1..width) {
//                            Canvas(Modifier.grid(Coordinate(x, y), width)) {
//                                drawRoundRect(
//                                    Color.LightGray,
//                                    Offset(2F, 2F),
//                                    Size(gridSize.toPx() - 4, gridSize.toPx() - 4),
//                                    CornerRadius(5F, 5F),
//                                )
//                            }
//                        }
//                    }

                        coordinates!!.run {
                            Home(home, width!!)
                            Doctor(doctor, width!!)
                            Mask(mask, width!!)
                            covids.forEach { coordinate ->
                                (-1..1).flatMap { x ->
                                    (-1..1).map { y ->
                                        coordinate.moved(x, y)
                                    }
                                }.filter { (x, y) ->
                                    x in 1..width!! && y in 1..width!!
                                }.distinct().forEach {
                                    Canvas(Modifier.grid(it, width!!)) {
                                        drawRect(Color.Red, alpha = 0.1f)
                                    }
                                }
                                Covid(coordinate, width!!)
                            }
                            Actor(actorCoordinates, width!!)
                        }

                }
                is Won, Lost -> {
                    homeX = null
                    homeY = null
                    width = null
                    actorCoordinates = Coordinate(1, 1)
                    state = Config
                }
            }
        }
    }
}

private fun Coordinate.moved(dx: Int, dy: Int): Coordinate = copy(x = x + dx, y = y + dy)

@Composable
fun windowWithText(text: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Button({ AppManager.focusedWindow?.close() }) {
            Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@ExperimentalTime
fun runPathfinding(coordinates: Coordinates, stateDelegate: MutableState<State>, actorDelegate: MutableState<Coordinate>) = thread {
    var actorCoordinates by actorDelegate
    var state by stateDelegate
    consult("assignment.pl") {
        coordinates.run {
            val result = query(
                "solve",
                home.x,
                home.y,
                mask.x,
                mask.y,
                doctor.x,
                doctor.y,
                covids[0].x,
                covids[0].y,
                covids[1].x,
                covids[1].y,
                width,
                "BackTrack",
                0,
                2 * width
            )
            println(
                """solve(${
                    listOf(
                        home.x,
                        home.y,
                        mask.x,
                        mask.y,
                        doctor.x,
                        doctor.y,
                        covids[0].x,
                        covids[0].y,
                        covids[1].x,
                        covids[1].y,
                        width,
                    ).joinToString(", ")
                }, BackTrack, ${listOf(0, width * 2).joinToString(", ")})."""
            )
            val time = measureTime {
                if (!result.hasNext()) {
                    println("There is no path")
                    Thread.sleep(2000L)
                    state = Lost
                    return@thread
                }
            }

            val path = (result.next()["BackTrack"] as? Compound)
                ?.toTermArray()
                ?.map { it as Compound }
                ?.map { it.toTermArray().run { Coordinate(get(0).intValue(), get(1).intValue()) } }

            println("Number of steps: ${path?.size}\n" +
                    "Time: $time")
            if (path == null) {
                println("Prolog code was successful but no path has found")
                return@thread
            }
            println(path.joinToString(", ", "[", "]") { (x, y) -> "[$x, $y]" })
            println("Successfully found the path")

            GlobalScope.launch {
                delay(1.seconds)
                path.forEach {
                    actorCoordinates = it
                    delay(.5.seconds)
                }
                delay(1.5.seconds)
                state = Won
            }
        }
    }
}


class Coordinates(val width: Int, val home: Coordinate) {

    private val alreadyGeneratedCoordinates = mutableSetOf<Coordinate>(Coordinate(1, 1), home)
    val doctor = randomCoordinates()
    val mask = randomCoordinates()
    val covids = (1..2).map { randomCoordinates() }

    private fun randomCoordinates(): Coordinate {
        while (true) {
            val newCoordinates = Coordinate((1..width).random(), (1..width).random())
            if (newCoordinates in alreadyGeneratedCoordinates) continue
            alreadyGeneratedCoordinates += newCoordinates
            return newCoordinates
        }
    }
}

val gridSize = 50.dp

fun Modifier.grid(coordinate: Coordinate, width: Int) =
    offset(left.first + gridSize * (coordinate.x - 1), left.second + gridSize * (width - coordinate.y))
        .size(gridSize)

val left = 0.dp to 0.dp

@Composable
fun Coordinates.Entity(color: Color, coordinate: Coordinate, width: Int, modifier: Modifier = Modifier) {
    Canvas(Modifier.grid(coordinate, width)) {
        drawRect(color)
    }
}

@Composable
fun Coordinates.Actor(coordinate: Coordinate, width: Int) = Entity(Color.Black, coordinate, width)

@Composable
fun Coordinates.Mask(coordinate: Coordinate, width: Int) = Entity(Color.Cyan, coordinate, width)

@Composable
fun Coordinates.Doctor(coordinate: Coordinate, width: Int) = Entity(Color.White, coordinate, width)

@Composable
fun Coordinates.Home(coordinate: Coordinate, width: Int) = Entity(Color.Yellow, coordinate, width)

@Composable
fun Coordinates.Covid(coordinate: Coordinate, width: Int) = Entity(Color.Green, coordinate, width)
