package com.pulse.minimove

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Local on-device camera exercise counting. No video recording, no Internet permission.
 * Supported: squats, push-ups (side view), single-arm biceps curls.
 */
class PoseCounterActivity : ComponentActivity() {
    private lateinit var preview: PreviewView
    private lateinit var overlay: SkeletonView
    private lateinit var countView: TextView
    private lateinit var statusView: TextView
    private lateinit var instructionView: TextView
    private lateinit var pauseButton: Button
    private var cameraProvider: ProcessCameraProvider? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
    )
    private val permissionCode = 311
    private val exerciseId by lazy { intent.getStringExtra(EXERCISE) ?: "squats" }
    private val goal by lazy { intent.getIntExtra(GOAL, 5).coerceIn(1, 100) }
    private val name by lazy { intent.getStringExtra(NAME) ?: "Тренировка" }
    private val original by lazy { intent.getIntExtra(INITIAL, 0).coerceIn(0, 100) }
    private val isPushup by lazy { exerciseId in listOf("pushups", "close-pushups", "wide-pushups", "wall-pushups") }
    private val isCurl by lazy { exerciseId == "biceps-curl" }
    private val tracker by lazy { when {
        isPushup -> RepTracker(112f, 152f)
        isCurl -> RepTracker(75f, 145f)
        else -> RepTracker(110f, 154f)
    } }
    private var facing = CameraSelector.LENS_FACING_FRONT
    private var paused = false
    private var lastUiUpdate = 0L
    private var lastDetected = 0L
    private var smoothAngle: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 13, 17)
        window.navigationBarColor = Color.rgb(10, 13, 17)
        facing = if (isPushup) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        tracker.reset(original)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(11,14,19)) }
        preview = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        overlay = SkeletonView(this)
        root.addView(preview, FrameLayout.LayoutParams(-1, -1))
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        val dp = resources.displayMetrics.density
        fun pad(v: Int) = (v*dp).toInt()
        fun label(text:String, size:Float, color:Int, weight:Float=1f):TextView = TextView(this).apply {
            this.text=text; textSize=size;setTextColor(color);setLineSpacing(0f,weight)
        }
        val panelColor=Color.argb(225, 15,20,27)
        val top=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(pad(20),pad(18),pad(20),pad(16))
            setBackgroundColor(panelColor)
        }
        val title=label("● СЧЁТЧИК КАМЕРОЙ · $name", 14f, Color.rgb(200,245,135)).apply { setTypeface(null,1) }
        countView=label("${tracker.count} / $goal",48f,Color.WHITE).apply { setTypeface(null,1) }
        statusView=label("Покажи всё тело в кадре",16f,Color.WHITE)
        instructionView=label(
            when { isPushup->"Поставь телефон сбоку: должны быть видны плечо, локоть, кисть, таз и нога.";
                isCurl->"Поставь телефон перед собой: одна рука целиком в кадре, сгибай и разгибай её.";
                else->"Поставь телефон перед собой или сбоку: видны таз, колено и стопа." },
            13f,Color.LTGRAY
        ).apply { setPadding(0,pad(7),0,0) }
        top.addView(title);top.addView(countView);top.addView(statusView);top.addView(instructionView)
        root.addView(top, FrameLayout.LayoutParams(-1,-2,Gravity.TOP))

        val bottom=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(pad(14),pad(12),pad(14),pad(18))
            setBackgroundColor(panelColor)
        }
        val advice=label("Камера распознаёт движение, но может ошибаться. Повторы можно отметить вручную.",12f,Color.LTGRAY)
        bottom.addView(advice)
        val buttons=LinearLayout(this).apply {
            orientation=LinearLayout.HORIZONTAL
            setPadding(0,pad(12),0,0)
        }
        fun action(name:String, onClick:()->Unit):Button = Button(this).apply {
            text=name;isAllCaps=false;textSize=12f;setOnClickListener{onClick()}
        }
        buttons.addView(action("← Назад"){setResult(RESULT_CANCELED);finish()},LinearLayout.LayoutParams(0,pad(53),1f))
        pauseButton=action("Пауза"){paused=!paused;pauseButton.text=if(paused)"Продолжить" else "Пауза";statusView.text=if(paused)"Пауза" else "Продолжай"}
        buttons.addView(pauseButton,LinearLayout.LayoutParams(0,pad(53),1f))
        buttons.addView(action("Камера ↺"){
            facing=if(facing==CameraSelector.LENS_FACING_FRONT)CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            smoothAngle=null
            tracker.reset(tracker.count)
            cameraProvider?.let{bindCamera(it)}
            statusView.text="Покажи всё тело в кадре"
        },LinearLayout.LayoutParams(0,pad(53),1f))
        buttons.addView(action("Готово ✓"){complete()},LinearLayout.LayoutParams(0,pad(53),1.1f))
        bottom.addView(buttons)
        root.addView(bottom,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED) startCamera()
        else requestPermissions(arrayOf(Manifest.permission.CAMERA),permissionCode)
    }

    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==permissionCode){
            if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)startCamera()
            else {statusView.text="Без доступа к камере счёт невозможен";instructionView.text="Разреши камеру в настройках Android или отмечай повторы вручную."}
        }
    }

    private fun startCamera(){
        val future=ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider=future.get();cameraProvider=provider;bindCamera(provider)
            }catch(t:Throwable){statusView.text="Не удалось запустить камеру";instructionView.text=t.localizedMessage?:"Нет доступа к камере"}
        },ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(provider:ProcessCameraProvider){
        provider.unbindAll()
        val selector=CameraSelector.Builder().requireLensFacing(facing).build()
        val view=Preview.Builder().build().also{it.setSurfaceProvider(preview.surfaceProvider)}
        val analysis=ImageAnalysis.Builder()
            .setTargetResolution(Size(640,480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        analysis.setAnalyzer(executor){proxy->
            val media=proxy.image
            if(media==null){proxy.close();return@setAnalyzer}
            try{
                val input=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)
                val rotatedWidth=if(proxy.imageInfo.rotationDegrees%180==0)proxy.width else proxy.height
                val rotatedHeight=if(proxy.imageInfo.rotationDegrees%180==0)proxy.height else proxy.width
                detector.process(input)
                    .addOnSuccessListener { pose ->
                        if(isFinishing || isDestroyed)return@addOnSuccessListener
                        processPose(pose,rotatedWidth,rotatedHeight)
                    }
                    .addOnFailureListener{statusView.text="Не удаётся распознать человека"}
                    .addOnCompleteListener{proxy.close()}
            }catch(t:Throwable){proxy.close();runOnUiThread{statusView.text="Камера недоступна: ${t.localizedMessage?:"ошибка"}"}}
        }
        provider.bindToLifecycle(this,selector,view,analysis)
    }

    private fun processPose(pose:Pose,width:Int,height:Int){
        val now=android.os.SystemClock.elapsedRealtime()
        overlay.show(pose,width,height,facing==CameraSelector.LENS_FACING_FRONT)
        val angle=angleForPose(pose)
        if(angle==null){
            tracker.observe(null,now)
            if(now-lastDetected>600L)statusView.text="Отойди так, чтобы нужные суставы были видны"
            smoothAngle=null
            return
        }
        lastDetected=now
        smoothAngle=if(smoothAngle==null)angle else 0.5f*angle+0.5f*smoothAngle!!
        if(!paused){
            val counted=tracker.observe(smoothAngle,now)
            if(counted){countView.text="${tracker.count} / $goal";countView.announceForAccessibility("${tracker.count}")}
        }
        if(now-lastUiUpdate<140L)return
        lastUiUpdate=now
        if(tracker.count>=goal){statusView.text="Цель достигнута! Нажми «Готово ✓»";paused=true;pauseButton.text="Продолжить";return}
        statusView.text=when(tracker.phase){
            RepTracker.Phase.SEARCHING -> "Начни с прямых рук или ног"
            RepTracker.Phase.READY -> if(isCurl)"Согни руку плавно" else "Опускайся плавно"
            RepTracker.Phase.BENT -> if(isCurl)"Теперь выпрями руку" else "Поднимайся обратно"
        }
    }

    private fun confidence(pose:Pose, ids:IntArray):Float{
        val landmarks=ids.mapNotNull(pose::getPoseLandmark)
        if(landmarks.size!=ids.size)return 0f
        return landmarks.minOf{it.inFrameLikelihood}
    }
    private fun angle(pose:Pose,a:Int,b:Int,c:Int):Float? {
        val ids=intArrayOf(a,b,c)
        if(confidence(pose,ids)<0.55f)return null
        val p1=pose.getPoseLandmark(a)!!.position
        val p2=pose.getPoseLandmark(b)!!.position
        val p3=pose.getPoseLandmark(c)!!.position
        val ux=(p1.x-p2.x).toDouble();val uy=(p1.y-p2.y).toDouble()
        val vx=(p3.x-p2.x).toDouble();val vy=(p3.y-p2.y).toDouble()
        val norm=sqrt((ux*ux+uy*uy)*(vx*vx+vy*vy))
        if(norm<1e-6)return null
        return Math.toDegrees(acos(( (ux*vx+uy*vy)/norm).coerceIn(-1.0,1.0))).toFloat()
    }

    private fun angleForPose(pose:Pose):Float? {
        val left=when {
            isCurl || isPushup->angle(pose,PoseLandmark.LEFT_SHOULDER,PoseLandmark.LEFT_ELBOW,PoseLandmark.LEFT_WRIST)
            else->angle(pose,PoseLandmark.LEFT_HIP,PoseLandmark.LEFT_KNEE,PoseLandmark.LEFT_ANKLE)
        }
        val right=when {
            isCurl || isPushup->angle(pose,PoseLandmark.RIGHT_SHOULDER,PoseLandmark.RIGHT_ELBOW,PoseLandmark.RIGHT_WRIST)
            else->angle(pose,PoseLandmark.RIGHT_HIP,PoseLandmark.RIGHT_KNEE,PoseLandmark.RIGHT_ANKLE)
        }
        val result=when {
            left!=null&&right!=null-> if(isCurl) min(left,right) else (left+right)/2f
            else->left?:right
        }
        if(isPushup && result!=null){
            val body=angle(pose,PoseLandmark.LEFT_SHOULDER,PoseLandmark.LEFT_HIP,PoseLandmark.LEFT_KNEE)
                ?:angle(pose,PoseLandmark.RIGHT_SHOULDER,PoseLandmark.RIGHT_HIP,PoseLandmark.RIGHT_KNEE)
            if(body!=null&&body<125f)return null
        }
        return result
    }

    private fun complete(){
        setResult(RESULT_OK,Intent().apply{
            putExtra(EXERCISE,exerciseId)
            putExtra(RESULT_COUNT,tracker.count)
        })
        finish()
    }

    override fun onDestroy(){
        cameraProvider?.unbindAll()
        detector.close()
        executor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXERCISE="exercise_id"
        const val NAME="exercise_name"
        const val GOAL="target"
        const val INITIAL="initial"
        const val RESULT_COUNT="count"
        val supported=setOf("pushups","close-pushups","wide-pushups","wall-pushups","squats","biceps-curl")
    }
}

/** Display of detected joints as simple lines and circles. No frames are stored. */
private class SkeletonView(context:android.content.Context):View(context){
    private val lines=Paint(Paint.ANTI_ALIAS_FLAG).apply{
        color=Color.rgb(196,245,115);strokeWidth=7f;strokeCap=Paint.Cap.ROUND;style=Paint.Style.STROKE
    }
    private val dots=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;style=Paint.Style.FILL}
    private var points:Map<Int,PointF> = emptyMap()
    private var srcW=1;private var srcH=1;private var mirrored=false
    fun show(pose:Pose,width:Int,height:Int,isMirrored:Boolean){
        srcW=width.coerceAtLeast(1);srcH=height.coerceAtLeast(1);mirrored=isMirrored
        points=pose.allPoseLandmarks.filter{it.inFrameLikelihood>0.6f}.associate{it.landmarkType to it.position}
        invalidate()
    }
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val scale=max(width.toFloat()/srcW,height.toFloat()/srcH)
        val dx=(width-srcW*scale)/2;val dy=(height-srcH*scale)/2
        fun map(point:PointF):PointF{
            val x=point.x*scale+dx
            return PointF(if(mirrored)width-x else x,point.y*scale+dy)
        }
        val edges=listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )
        edges.forEach{(a,b)->
            val p=points[a];val q=points[b]
            if(p!=null&&q!=null){val p1=map(p);val p2=map(q);canvas.drawLine(p1.x,p1.y,p2.x,p2.y,lines)}
        }
        points.values.forEach{point->val p=map(point);canvas.drawCircle(p.x,p.y,5f,dots)}
    }
}
