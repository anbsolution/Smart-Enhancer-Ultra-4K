package com.anb.smartenhancer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private var original: Bitmap? = null
    private var result: Bitmap? = null
    private var pendingSaveBitmap: Bitmap? = null
    private var operationId = 0L
    private var busy = false

    private lateinit var beforeView: ImageView
    private lateinit var afterView: ImageView
    private lateinit var status: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var scaleSpinner: Spinner
    private lateinit var formatSpinner: Spinner
    private lateinit var strengthSeek: SeekBar
    private lateinit var strengthLabel: TextView
    private lateinit var compareButton: Button
    private lateinit var chooseButton: Button
    private lateinit var enhanceButton: Button
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var progressBar: ProgressBar

    @Volatile private var selectedMode = "Fast Enhance"
    private var showingOriginal = false
    private val worker = Executors.newSingleThreadExecutor()

    private val purple = Color.rgb(124, 92, 255)
    private val bg = Color.rgb(16, 17, 26)
    private val panel = Color.rgb(29, 31, 45)
    private val white = Color.rgb(245, 246, 255)
    private val muted = Color.rgb(171, 175, 197)
    private val buttonPanel = Color.rgb(48, 51, 70)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(18), dp(18), dp(18), dp(22))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(label("SMART ENHANCER", 12, purple, true))
        content.addView(label("Ultra 4K", 30, white, true))
        content.addView(label("Sharpen • Upscale • Export", 14, muted, false).apply {
            setPadding(0, 0, 0, dp(18))
        })

        chooseButton = button("＋   Choose an image")
        content.addView(chooseButton)
        chooseButton.setOnClickListener { chooseImage() }

        content.addView(sectionTitle("PREVIEW"))
        content.addView(label("Working preview is limited to a 2048 px long edge for memory safety.", 11, muted, false))
        val previews = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        beforeView = imagePanel("Original working preview")
        afterView = imagePanel("Enhanced preview")
        previews.addView(beforeView, LinearLayout.LayoutParams(0, dp(170), 1f).apply { marginEnd = dp(6) })
        previews.addView(afterView, LinearLayout.LayoutParams(0, dp(170), 1f).apply { marginStart = dp(6) })
        content.addView(previews)

        compareButton = button("⇄   Toggle original / enhanced", buttonPanel)
        content.addView(compareButton)
        compareButton.setOnClickListener {
            val src = original
            val out = result
            if (src == null || out == null) {
                toast("Pehle image enhance karein.")
            } else {
                showingOriginal = !showingOriginal
                afterView.setImageBitmap(if (showingOriginal) src else out)
                compareButton.text = if (showingOriginal) "⇄   Showing original — tap to compare" else "⇄   Toggle original / enhanced"
            }
        }

        content.addView(sectionTitle("ENHANCEMENT MODE"))
        modeGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        val fast = RadioButton(this).apply {
            text = "Fast Enhance · offline filters"
            setTextColor(white); textSize = 14f; id = View.generateViewId(); isChecked = true
        }
        val smart = RadioButton(this).apply {
            text = "AI Enhance · heuristic filters (not neural AI)"
            setTextColor(white); textSize = 14f; id = View.generateViewId()
        }
        modeGroup.addView(fast)
        modeGroup.addView(smart)
        modeGroup.setOnCheckedChangeListener { _, checked ->
            selectedMode = if (checked == smart.id) "AI Enhance" else "Fast Enhance"
            invalidateResultForSettingsChange()
        }
        content.addView(modeGroup)

        content.addView(sectionTitle("ENHANCEMENT STRENGTH"))
        strengthLabel = label("Strength: 65%", 13, muted, false)
        content.addView(strengthLabel)
        strengthSeek = SeekBar(this).apply {
            max = 100
            progress = 65
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
            thumbTintList = android.content.res.ColorStateList.valueOf(purple)
        }
        strengthSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                strengthLabel.text = "Strength: ${progress.coerceAtLeast(10)}%"
                if (fromUser) invalidateResultForSettingsChange()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        content.addView(strengthSeek)

        content.addView(sectionTitle("UPSCALE OUTPUT"))
        scaleSpinner = spinner(listOf(
            "Standard (working size, max 2048 px)",
            "2× upscale (output capped at 4096 px)",
            "4× upscale (output capped at 4096 px)",
            "4K output (long edge 3840 px)"
        ))
        scaleSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != 0) invalidateResultForSettingsChange()
            }
        }
        content.addView(scaleSpinner)

        content.addView(sectionTitle("EXPORT FORMAT"))
        formatSpinner = spinner(listOf("JPG", "PNG", "WebP"))
        content.addView(formatSpinner)

        enhanceButton = button("✨   Enhance image")
        content.addView(enhanceButton)
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            visibility = View.GONE
            max = 100
            progress = 0
            progressTintList = android.content.res.ColorStateList.valueOf(purple)
        }
        content.addView(progressBar, LinearLayout.LayoutParams(-1, dp(5)).apply { bottomMargin = dp(10) })
        enhanceButton.setOnClickListener { enhance() }

        saveButton = button("↓   Save enhanced image", buttonPanel)
        content.addView(saveButton)
        saveButton.isEnabled = false
        saveButton.setOnClickListener { saveImage() }

        resetButton = button("↺   Reset image and settings", buttonPanel)
        content.addView(resetButton)
        resetButton.setOnClickListener { resetImageAndSettings() }

        status = label("Choose an image to get started.", 13, muted, false).apply {
            setPadding(0, dp(14), 0, 0)
        }
        content.addView(status)
        content.addView(label(
            "AI Enhance is an offline contrast/sharpening approximation, not a trained AI model. Upscaling enlarges pixels; it cannot reliably restore detail that was never captured.",
            12, muted, false
        ).apply { setPadding(0, dp(16), 0, 0) })

        setContentView(root)
        if (Build.VERSION.SDK_INT >= 30) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
                val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    dp(18) + bars.left,
                    dp(18) + bars.top,
                    dp(18) + bars.right,
                    dp(22) + bars.bottom
                )
                windowInsets
            }
            ViewCompat.requestApplyInsets(root)
        }
        updateEnabledState()
    }

    private fun chooseImage() {
        if (busy) return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE)
        } catch (e: Exception) {
            status.text = "Could not open image picker: ${e.message ?: "unknown error"}"
        }
    }

    private fun loadImage(uri: Uri) {
        val token = ++operationId
        clearCurrentImages()
        setBusy(true)
        status.text = "Loading image safely…"
        worker.execute {
            var decoded: Bitmap? = null
            var error: Exception? = null
            try {
                decoded = decodeWorkingBitmap(uri)
            } catch (e: Exception) {
                error = e
            } catch (e: OutOfMemoryError) {
                error = IOException("Image is too large for available memory", e)
            }
            val loaded = decoded
            val failure = error
            runOnUiThread {
                if (isFinishing || isDestroyed || token != operationId) {
                    loaded?.recycle()
                    return@runOnUiThread
                }
                if (failure != null || loaded == null) {
                    status.text = "Image open failed: ${failure?.message ?: "unsupported image"}"
                    setBusy(false)
                    return@runOnUiThread
                }
                original = loaded
                beforeView.setImageBitmap(loaded)
                afterView.setImageDrawable(null)
                status.text = "Image loaded • ${loaded.width} × ${loaded.height} px working preview"
                setBusy(false)
            }
        }
    }

    private fun decodeWorkingBitmap(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: throw IOException("Could not open selected image")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Unsupported or corrupt image")
        val sample = ImageSizing.calculateInSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: throw IOException("Image decoder returned no bitmap")

        val exif = try {
            contentResolver.openInputStream(uri)?.use { input -> ExifInterface(input) }
        } catch (_: Exception) {
            null
        }
        val rotation = exif?.rotationDegrees ?: 0
        val flipped = exif?.isFlipped == true
        if (rotation == 0 && !flipped) return decoded

        val matrix = Matrix().apply {
            if (flipped) preScale(-1f, 1f)
            if (rotation != 0) postRotate(rotation.toFloat())
        }
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        return oriented
    }

    private fun enhance() {
        val src = original ?: run { toast("Pehle image choose karein."); return }
        if (busy) return
        val token = ++operationId
        val mode = selectedMode
        val strengthPercent = strengthSeek.progress.coerceAtLeast(10)
        val strength = (strengthPercent / 65f).coerceIn(0.15f, 1.55f)
        val outputMode = when (scaleSpinner.selectedItemPosition) {
            1 -> ImageSizing.OutputMode.TWO_X
            2 -> ImageSizing.OutputMode.FOUR_X
            3 -> ImageSizing.OutputMode.FOUR_K
            else -> ImageSizing.OutputMode.STANDARD
        }
        setBusy(true)
        status.text = "Enhancing in background…"
        worker.execute {
            var filtered: Bitmap? = null
            var output: Bitmap? = null
            var errorMessage: String? = null
            var outOfMemory = false
            try {
                filtered = enhanceBitmap(src, strength, mode == "AI Enhance")
                val target = ImageSizing.outputSize(filtered.width, filtered.height, outputMode)
                output = if (target.width == filtered.width && target.height == filtered.height) {
                    filtered
                } else {
                    Bitmap.createScaledBitmap(filtered, target.width, target.height, true)
                }
                if (output !== filtered) filtered.recycle()
                filtered = null
            } catch (e: OutOfMemoryError) {
                outOfMemory = true
                errorMessage = "Not enough memory. Try Standard or 2× output."
            } catch (e: Exception) {
                errorMessage = e.message ?: "unknown processing error"
            } finally {
                filtered?.let { if (!it.isRecycled) it.recycle() }
            }
            val completed = output
            val failure = errorMessage
            runOnUiThread {
                if (isFinishing || isDestroyed || token != operationId) {
                    completed?.let { if (!it.isRecycled) it.recycle() }
                    return@runOnUiThread
                }
                if (completed == null) {
                    result = null
                    afterView.setImageDrawable(null)
                    status.text = if (outOfMemory) failure ?: "Not enough memory." else "Could not enhance image: ${failure ?: "unknown error"}"
                    setBusy(false)
                    return@runOnUiThread
                }
                val oldResult = result
                result = completed
                afterView.setImageBitmap(completed)
                oldResult?.let { old -> if (old !== completed && !old.isRecycled) old.recycle() }
                showingOriginal = false
                compareButton.text = "⇄   Toggle original / enhanced"
                status.text = "Done • ${completed.width} × ${completed.height} px • $mode • $strengthPercent% strength"
                setBusy(false)
            }
        }
    }

    /** Single-pass contrast plus unsharp-mask approximation; pixel math is covered by JVM tests. */
    private fun enhanceBitmap(src: Bitmap, strength: Float, stronger: Boolean): Bitmap {
        val width = src.width
        val height = src.height
        val input = IntArray(Math.multiplyExact(width, height))
        src.getPixels(input, 0, width, 0, 0, width, height)
        val outputPixels = PixelEnhancer.transformArgb(input, width, height, strength, stronger)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(outputPixels, 0, width, 0, 0, width, height)
        }
    }

    private fun saveImage() {
        val bitmap = result ?: run { toast("Pehle image enhance karein."); return }
        if (busy) return
        pendingSaveBitmap = bitmap
        val format = formatSpinner.selectedItem.toString()
        val ext = when (format) { "PNG" -> "png"; "WebP" -> "webp"; else -> "jpg" }
        val mime = when (format) { "PNG" -> "image/png"; "WebP" -> "image/webp"; else -> "image/jpeg" }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, "SmartEnhancer_${System.currentTimeMillis()}.$ext")
        }
        setBusy(true)
        status.text = "Choose where to save the enhanced image."
        try {
            startActivityForResult(intent, REQUEST_SAVE_IMAGE)
        } catch (e: Exception) {
            pendingSaveBitmap = null
            setBusy(false)
            status.text = "Could not open save dialog: ${e.message ?: "unknown error"}"
        }
    }

    private fun writeImage(uri: Uri, bitmap: Bitmap) {
        val format = formatSpinner.selectedItem.toString()
        val token = ++operationId
        worker.execute {
            var failure: String? = null
            try {
                val output = contentResolver.openOutputStream(uri)
                    ?: throw IOException("Could not open destination stream")
                output.use { stream ->
                    val compressFormat = when (format) {
                        "PNG" -> Bitmap.CompressFormat.PNG
                        "WebP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                        else -> Bitmap.CompressFormat.JPEG
                    }
                    val quality = if (format == "PNG") 100 else 94
                    if (!bitmap.compress(compressFormat, quality, stream)) {
                        throw IOException("Image encoder failed")
                    }
                    stream.flush()
                }
            } catch (e: OutOfMemoryError) {
                failure = "Not enough memory to export this image"
                try { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } catch (_: Exception) {}
            } catch (e: Exception) {
                failure = e.message ?: "unknown save error"
                try { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } catch (_: Exception) {}
            }
            val message = failure
            runOnUiThread {
                if (isFinishing || isDestroyed || token != operationId) return@runOnUiThread
                status.text = if (message == null) "Image saved successfully • $format" else "Save failed: $message"
                pendingSaveBitmap = null
                setBusy(false)
            }
        }
    }

    private fun resetImageAndSettings() {
        if (busy) return
        operationId++
        clearCurrentImages()
        scaleSpinner.setSelection(0)
        formatSpinner.setSelection(0)
        strengthSeek.progress = 65
        modeGroup.check(modeGroup.getChildAt(0).id)
        status.text = "Reset complete • choose another image."
        setBusy(false)
    }

    private fun invalidateResultForSettingsChange() {
        if (!::status.isInitialized || busy || result == null) return
        val previousResult = result
        result = null
        afterView.setImageDrawable(null)
        previousResult?.let { if (!it.isRecycled) it.recycle() }
        showingOriginal = false
        saveButton.isEnabled = false
        status.text = "Settings changed • enhance again before saving."
    }

    private fun clearCurrentImages() {
        beforeView.setImageDrawable(null)
        afterView.setImageDrawable(null)
        val oldResult = result
        val oldOriginal = original
        result = null
        original = null
        showingOriginal = false
        oldResult?.let { if (it !== oldOriginal && !it.isRecycled) it.recycle() }
        oldOriginal?.let { if (!it.isRecycled) it.recycle() }
        saveButton.isEnabled = false
    }

    private fun setBusy(value: Boolean) {
        busy = value
        progressBar.visibility = if (value) View.VISIBLE else View.GONE
        progressBar.isIndeterminate = value
        updateEnabledState()
    }

    private fun updateEnabledState() {
        if (!::chooseButton.isInitialized) return
        chooseButton.isEnabled = !busy
        enhanceButton.isEnabled = !busy && original != null
        saveButton.isEnabled = !busy && result != null
        resetButton.isEnabled = !busy
        compareButton.isEnabled = !busy && result != null
        modeGroup.isEnabled = !busy
        for (i in 0 until modeGroup.childCount) modeGroup.getChildAt(i).isEnabled = !busy
        strengthSeek.isEnabled = !busy
        scaleSpinner.isEnabled = !busy
        formatSpinner.isEnabled = !busy
    }

    override fun onDestroy() {
        operationId++
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun imagePanel(caption: String): ImageView = ImageView(this).apply {
        setBackgroundColor(panel)
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = caption
        setPadding(dp(4), dp(4), dp(4), dp(4))
    }

    private fun spinner(items: List<String>): Spinner {
        val spinner = Spinner(this)
        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                super.getView(position, convertView, parent).also { view ->
                    (view as? TextView)?.apply { setTextColor(white); textSize = 13f }
                }

            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View =
                super.getDropDownView(position, convertView, parent).also { view ->
                    (view as? TextView)?.apply {
                        setTextColor(white)
                        setBackgroundColor(panel)
                        textSize = 13f
                        setPadding(dp(12), dp(12), dp(12), dp(12))
                    }
                }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setPadding(dp(8), dp(4), dp(8), dp(4))
        return spinner
    }

    private fun sectionTitle(text: String) = label(text, 12, muted, true).apply {
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(text: String, color: Int = purple) = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(color))
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                if (uri != null) loadImage(uri) else status.text = "No image was selected."
            }
        } else if (requestCode == REQUEST_SAVE_IMAGE) {
            val bitmap = pendingSaveBitmap
            val uri = data?.data
            if (resultCode == Activity.RESULT_OK && uri != null && bitmap != null) {
                status.text = "Saving image…"
                writeImage(uri, bitmap)
            } else {
                pendingSaveBitmap = null
                setBusy(false)
                status.text = "Save cancelled."
            }
        }
    }

    companion object {
        private const val REQUEST_PICK_IMAGE = 100
        private const val REQUEST_SAVE_IMAGE = 101
    }
}
