package com.zawmyat.arlearn

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.zawmyat.arlearn.ui.theme.ARLearnTheme
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.getUpdatedPlanes
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.getDescription
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ARLearnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                  //  ARModelViewer()
                    ARComposable()

                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun ARModelViewer() {

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine = engine)
    val materialLoader = rememberMaterialLoader(engine = engine)
    val cameraNode = rememberARCameraNode(engine = engine)
    val childNodes = rememberNodes()

    val view = rememberView(engine = engine)
    val collisionSystem = rememberCollisionSystem(view = view)

    var planeRenderer by remember {
        mutableStateOf(true)
    }

    val modelInstances = remember {
        mutableListOf<ModelInstance>()
    }

    var trackingFailureReason by remember {
        mutableStateOf<TrackingFailureReason?>(null)
    }

    var frame by remember {
        mutableStateOf<Frame?>(null)
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {


        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            view = view,
            modelLoader = modelLoader,
            collisionSystem = collisionSystem,
            sessionConfiguration = {
                    session, config ->

                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }

                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            },
            cameraNode = cameraNode,
            planeRenderer = planeRenderer,
            onTrackingFailureChanged = {
                trackingFailureReason = it
            },
            onSessionUpdated = {
                    session, updatedFrame ->

                frame = updatedFrame

                if(childNodes.isNotEmpty()) {
                    updatedFrame.getUpdatedPlanes().firstOrNull {
                        it.type == Plane.Type.HORIZONTAL_UPWARD_FACING
                    }?.let {
                        it.createAnchorOrNull(it.centerPose)
                    }?.let {
                            anchor ->
                        childNodes += createAnchorNode(
                            engine = engine,
                            modelLoader = modelLoader,
                            materialLoader = materialLoader,
                            modelInstances = modelInstances,
                            anchor = anchor
                        )
                    }
                }
            },
            onGestureListener = rememberOnGestureListener(
                onSingleTapConfirmed = {
                        motionEvent, node ->

                    if (node == null && childNodes.isEmpty()) {
                        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)

                        hitResults?.firstOrNull() {
                            it.isValid(
                                depthPoint = false,
                                point = false
                            )
                        }?.createAnchorOrNull()?.let {
                                anchor ->
                            planeRenderer = false

                            childNodes.clear()

                            childNodes += createAnchorNode(
                                engine = engine,
                                modelLoader = modelLoader,
                                materialLoader = materialLoader,
                                modelInstances = modelInstances,
                                anchor = anchor
                            )
                        }
                    }
                }
            )
        )


        Text(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 32.dp, end = 32.dp),
            text = trackingFailureReason?.let {
                it.getDescription(LocalContext.current)
            } ?: if (childNodes.isEmpty()) {
                "Point your phone down"
            } else {
                "Tap Anywhere to add model"
            },
            color = Color.Green
        )
    }

}

val modelFile = "models/ant.glb"
val maxModelInstances = 1

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    materialLoader: MaterialLoader,
    modelInstances: MutableList<ModelInstance>,
    anchor: Anchor
) : AnchorNode {
    val anchorNode = AnchorNode(engine = engine, anchor = anchor)

    if(modelInstances.isEmpty()) {
        modelInstances += modelLoader.createInstancedModel(modelFile, 1)
    }

    val modelNode = ModelNode(
        modelInstance = modelInstances.first(),
//        modelInstance = modelInstances.apply {
//            if (isEmpty()) {
//                this += modelLoader.createInstancedModel(
//                    modelFile,
//                    maxModelInstances
//                )
//            }
//        }.removeLast(),
        scaleToUnits = 0.5f
    ).apply {
        isEditable = true

    }

    val boundingBoxNode = CubeNode(
        engine = engine,
        size = modelNode.extents,
        center = modelNode.center,
        materialInstance = materialLoader.createColorInstance(Color.Green.copy(alpha = 0.3f))
    ).apply {
        isVisible = false
    }

    modelNode.addChildNode(boundingBoxNode)
    anchorNode.addChildNode(modelNode)

    listOf(modelNode, anchorNode).forEach {
        it.onEditingChanged = {
            editingTransforms ->
            boundingBoxNode.isVisible = editingTransforms.isNotEmpty()
        }
    }

    return anchorNode
}

//Model
private const val kModelFile = "https://ahuamana.github.io/models-ar/insect/ant.glb"

@Composable
fun ARComposable() {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        var isLoading by remember { mutableStateOf(false) }
        var planeRenderer by remember { mutableStateOf(true) }
        val engine = rememberEngine()
        val renderer = rememberRenderer(engine)
        val modelLoader = rememberModelLoader(engine)
        val childNodes = rememberNodes()
        val coroutineScope = rememberCoroutineScope()

        ARScene(
            modifier = Modifier.fillMaxSize(),
            childNodes = childNodes,
            engine = engine,
            renderer = renderer,
            modelLoader = modelLoader,
            planeRenderer = planeRenderer,
            sessionConfiguration = { session, config ->
                config.depthMode =
                    when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        true -> Config.DepthMode.AUTOMATIC
                        else -> Config.DepthMode.DISABLED
                    }
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                //config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                //config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
                config.lightEstimationMode =
                    Config.LightEstimationMode.DISABLED
            },
            onSessionUpdated = { _, frame ->
                if (childNodes.isNotEmpty()) return@ARScene

                frame.getUpdatedPlanes()
                    .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
                    ?.let { plane ->
                        isLoading = true
                        childNodes += AnchorNode(
                            engine = engine,
                            anchor = plane.createAnchor(plane.centerPose)
                        ).apply {
                            isEditable = false

                            coroutineScope.launch {

                                modelLoader.loadModelInstance(modelFile)?.let {
                                    addChildNode(
                                        ModelNode(
                                            modelInstance = it,
                                            // Scale to fit in a 0.5 meters cube
                                            scaleToUnits = 1f,
                                            // Bottom origin instead of center so the
                                            // model base is on floor level
                                            centerOrigin = Position(0f, 0f)
                                        ).apply {
                                            isEditable = true
                                        }
                                    )
                                }
                                planeRenderer = false
                                isLoading = false
                            }
                        }
                    }
            },
            onSessionFailed = { exception ->
                Log.i("ZAW", exception.message.toString())
                Toast.makeText(context, "Session failed ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center),
                color = Color.Magenta
            )
        }
    }

}