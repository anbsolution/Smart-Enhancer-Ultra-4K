package com.anb.smartenhancer

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private var original: Bitmap? = null
    private var result: Bitmap? = null
    private lateinit var beforeView: ImageView
    private lateinit var afterView: ImageView
    private lateinit var status: TextView
    private lateinit var modeGroup: RadioGroup
    private lateinit var scaleSpinner: Spinner
    private lateinit var formatSpinner: Spinner
    private lateinit var strengthSeek: SeekBar
    private lateinit var strengthLabel: TextView
    private lateinit var compareButton: Button
    private var showingOriginal = false
    private var selectedMode = "Fast Enhance"
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var enhanceButton: Button
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar

    private val purple = Color.rgb(124, 92, 255)
    private val bg = Color.rgb(16, 17, 26)
    private val panel = Color.rgb(29, 31, 45)
    private val white = Color.rgb(245, 246, 255)
    private val muted = Color.rgb(171, 175, 197)

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

        val pick = button("＋   Choose an image")
        content.addView(pick)
        pick.setOnClickListener {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(i, 100)
        }

        content.addView(sectionTitle("PREVIEW"))
        val previews = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        beforeView = imagePanel("Original")
        afterView = imagePanel("Enhanced")
        previews.addView(beforeView, LinearLayout.LayoutParams(0, dp(170), 1f).apply { marginEnd = dp(6) })
        previews.addView(afterView, LinearLayout.LayoutParams(0, dp(170), 1f).apply { marginStart = dp(6) })
        content.addView(previews)
        compareButton = button("⇄   Toggle original / enhanced").apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(48, 51, 70)))
        }
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
            text = "Fast Enhance  ·  offline filters"
            setTextColor(white); textSize = 14f; isChecked = true; id = View.generateViewId()
        }
        val ai = RadioButton(this).apply {
            text = "AI Enhance  ·  smart detail approximation"
            setTextColor(white); textSize = 14f; id = View.generateViewId()
        }
        modeGroup.addView(fast); modeGroup.addView(ai)
        modeGroup.setOnCheckedChangeListener { _, checked ->
            selectedMode = if (checked == ai.id) "AI Enhance" else "Fast Enhance"
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
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        content.addView(strengthSeek)

        content.addView(sectionTitle("UPSCALE OUTPUT"))
        scaleSpinner = spinner(listOf("Original size", "2× upscale", "4× upscale", "4K (long edge 3840 px)"))
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

        saveButton = button("↓   Save enhanced image").apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(48, 51, 70)))
        }
        content.addView(saveButton)
        saveButton.setOnClickListener { saveImage() }
        saveButton.isEnabled = false

        val reset = button("↺   Reset image and settings").apply {
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(48, 51, 70)))
        }
        content.addView(reset)
        reset.setOnClickListener {
            original = null
            result = null
            showingOriginal = false
            beforeView.setImageDrawable(null)
            afterView.setImageDrawable(null)
            scaleSpinner.setSelection(0)
            formatSpinner.setSelection(0)
            strengthSeek.progress = 65
            modeGroup.check(modeGroup.getChildAt(0).id)
            status.text = "Reset complete • choose another image."
            saveButton.isEnabled = false
        }

        status = label("Choose an image to get started.", 13, muted, false)
        status.setPadding(0, dp(14), 0, 0)
        content.addView(status)

        content.addView(label(
            "Note: AI Enhance currently uses an offline smart-filter approximation, not a trained generative AI model. It can improve sharpness and contrast, but cannot reliably recreate details that were never captured. A real AI super-resolution model can be added as a separate model integration.",
            12, muted, false
        ).apply { setPadding(0, dp(16), 0, 0) })

        setContentView(root)
    }

    private fun enhance() {
        val src = original ?: run { toast("Pehle image choose karein."); return }
        val mode = selectedMode
        val strengthPercent = strengthSeek.progress.coerceAtLeast(10)
        val strength = (strengthPercent / 65f).coerceIn(0.15f, 1.55f)
        val scaleChoice = scaleSpinner.selectedItemPosition
        enhanceButton.isEnabled = false
        saveButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = true
        status.text = "Processing in background… app remains responsive."
        worker.execute {
            try {
                val filtered = if (mode == "AI Enhance") smartEnhance(src, strength) else fastEnhance(src, strength)
                val target = when (scaleChoice) {
                    1 -> dimensions((filtered.width.toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                                    (filtered.height.toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    2 -> dimensions((filtered.width.toLong() * 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                                    (filtered.height.toLong() * 4).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    3 -> {
                        val longEdge = max(filtered.width, filtered.height)
                        val factor = 3840f / longEdge
                        if (longEdge >= 3840) Pair(filtered.width, filtered.height)
                        else dimensions((filtered.width * factor).roundToInt(), (filtered.height * factor).roundToInt())
                    }
                    else -> Pair(filtered.width, filtered.height)
                }
                val output = if (target.first == filtered.width && target.second == filtered.height) filtered
                    else Bitmap.createScaledBitmap(filtered, target.first, target.second, true)
                runOnUiThread {
                    result = output
                    afterView.setImageBitmap(output)
                    showingOriginal = false
                    compareButton.text = "⇄   Toggle original / enhanced"
                    status.text = "Done • ${output.width} × ${output.height} px • $mode • $strengthPercent% strength"
                    progressBar.visibility = View.GONE
                    enhanceButton.isEnabled = true
                    saveButton.isEnabled = true
                }
            } catch (e: OutOfMemoryError) {
                runOnUiThread {
                    status.text = "Not enough memory for this output. Try Original size or 2×."
                    progressBar.visibility = View.GONE
                    enhanceButton.isEnabled = true
                    saveButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Could not enhance image: ${e.message ?: "unknown error"}"
                    progressBar.visibility = View.GONE
                    enhanceButton.isEnabled = true
                    saveButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdown()
        super.onDestroy()
    }

    private fun fastEnhance(src: Bitmap, strength: Float = 1f): Bitmap {
        val w = src.width; val h = src.height
        val input = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        val output = IntArray(input.size)
        for (i in input.indices) {
            val c = input[i]
            val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
            fun adjust(v: Int): Int = ((v - 128) * (1f + 0.16f * strength) + 128 + (v - 128) * 0.06f * strength).roundToInt().coerceIn(0, 255)
            output[i] = Color.argb(Color.alpha(c), adjust(r), adjust(g), adjust(b))
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(output, 0, w, 0, 0, w, h)
        return sharpen(out, 0.38f * strength)
    }

    // Offline heuristic mode: stronger contrast and sharpening, not a trained neural network.
    private fun smartEnhance(src: Bitmap, strength: Float): Bitmap {
        val filtered = fastEnhance(src, strength * 1.15f)
        return sharpen(filtered, 0.72f * strength)
    }

    private fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width; val h = src.height
        if (w < 3 || h < 3) return src
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = pixels.clone()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = pixels[i]
                val neighbors = intArrayOf(pixels[i - w], pixels[i + w], pixels[i - 1], pixels[i + 1])
                fun channel(extract: (Int) -> Int): Int {
                    val center = extract(c).toFloat()
                    val avg = neighbors.map { extract(it) }.average().toFloat()
                    return (center + (center - avg) * amount * 2.0f).roundToInt().coerceIn(0, 255)
                }
                out[i] = Color.argb(Color.alpha(c), channel(Color::red), channel(Color::green), channel(Color::blue))
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { it.setPixels(out, 0, w, 0, 0, w, h) }
    }

    private fun calculateSample(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width, height) / (sample * 2) >= maxEdge) sample *= 2
        return sample.coerceAtLeast(1)
    }

    private fun dimensions(w: Int, h: Int): Pair<Int, Int> {
        val maxDimension = 4096
        val factor = min(1f, maxDimension.toFloat() / max(w, h))
        return Pair(max(1, (w * factor).roundToInt()), max(1, (h * factor).roundToInt()))
    }

    private fun saveImage() {
        val bmp = result ?: run { toast("Pehle image enhance karein."); return }
        val format = formatSpinner.selectedItem.toString()
        val mime = when (format) { "PNG" -> "image/png"; "WebP" -> "image/webp"; else -> "image/jpeg" }
        val ext = when (format) { "PNG" -> "png"; "WebP" -> "webp"; else -> "jpg" }
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "SmartEnhancer_${System.currentTimeMillis()}.$ext")
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SmartEnhancer")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create output file")
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                    ?: throw IOException("Could not open output stream")
                outputStream.use { stream ->
                    val fmt = when (format) {
                        "PNG" -> Bitmap.CompressFormat.PNG
                        "WebP" -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                        else -> Bitmap.CompressFormat.JPEG
                    }
                    if (!bmp.compress(fmt, if (format == "PNG") 100 else 94, stream)) {
                        throw IOException("Bitmap compression failed")
                    }
                }
            } catch (e: Exception) {
                contentResolver.delete(uri, null, null)
                throw e
            }
            status.text = "Saved to Pictures/SmartEnhancer • $format"
        } catch (e: Exception) {
            status.text = "Save failed: ${e.message ?: "unknown error"}"
        }
    }

    private fun imagePanel(caption: String): ImageView {
        return ImageView(this).apply {
            setBackgroundColor(panel)
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = caption
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
    }

    private fun spinner(items: List<String>): Spinner {
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        s.setPadding(dp(8), dp(4), dp(8), dp(4))
        return s
    }

    private fun sectionTitle(text: String) = label(text, 12, muted, true).apply {
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun button(text: String) = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple))
        layoutParams = LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                contentResolver.openInputStream(uri).use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
                val sample = BitmapFactory.Options().apply {
                    inSampleSize = calculateSample(bounds.outWidth, bounds.outHeight, 4096)
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                contentResolver.openInputStream(uri).use { stream ->
                    original = BitmapFactory.decodeStream(stream, null, sample) ?: throw IllegalArgumentException("Unsupported image")
                }
                beforeView.setImageBitmap(original)
                afterView.setImageDrawable(null)
                result = null
                saveButton.isEnabled = false
                status.text = "Image loaded • ${original!!.width} × ${original!!.height} px"
            } catch (e: Exception) {
                status.text = "Image open failed: ${e.message ?: "unknown error"}"
            }
        }
    }
}
