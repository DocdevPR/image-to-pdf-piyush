package com.piyush.imagetopdf

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.IOException
import kotlin.math.min

class MainActivity : Activity() {
    private data class ImageItem(val uri: Uri, val name: String)
    private val images = mutableListOf<ImageItem>()
    private lateinit var listBox: LinearLayout
    private lateinit var status: TextView
    private lateinit var pageSize: Spinner
    private lateinit var orientation: Spinner
    private lateinit var margin: Spinner
    private lateinit var createButton: Button

    private val PICK_IMAGES_REQUEST = 1001
    private val SAVE_PDF_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(245, 247, 252)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 252))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
        }
        val title = TextView(this).apply {
            text = "Image to PDF"
            textSize = 28f
            setTextColor(Color.rgb(25, 35, 65))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        content.addView(title)
        content.addView(text("by Piyush  •  Private, on-device conversion", 14, Color.DKGRAY), marginParams(top = 4, bottom = 18))

        val pickButton = button("＋  Select images") { openImagePicker() }
        content.addView(pickButton, matchParams())
        content.addView(text("Choose multiple images, then use the arrows to change their order.", 13, Color.DKGRAY), marginParams(top = 8, bottom = 12))

        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listBox)

        content.addView(sectionTitle("PDF settings"), marginParams(top = 18, bottom = 8))
        pageSize = spinner(listOf("A4", "Letter", "Fit image (A4 page)"))
        orientation = spinner(listOf("Portrait", "Landscape"))
        margin = spinner(listOf("No margin", "Small margin (5 mm)", "Normal margin (10 mm)", "Large margin (20 mm)"))
        content.addView(label("Page size")); content.addView(pageSize, marginParams(bottom = 10))
        content.addView(label("Orientation")); content.addView(orientation, marginParams(bottom = 10))
        content.addView(label("Margins")); content.addView(margin, marginParams(bottom = 14))

        createButton = button("Create PDF") { startPdfSave() }
        content.addView(createButton, matchParams())
        status = text("Your images stay on your device.", 13, Color.rgb(65, 75, 95))
        content.addView(status, marginParams(top = 12))
        content.addView(text("Tip: very large images may take longer to process. Output is saved through Android's file picker.", 12, Color.GRAY), marginParams(top = 16))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderImages()
    }

    @Deprecated("Deprecated by Android, retained for broad compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            PICK_IMAGES_REQUEST -> {
                val clip = data.clipData
                if (clip != null) {
                    for (i in 0 until clip.itemCount) addUri(clip.getItemAt(i).uri)
                } else data.data?.let { addUri(it) }
                renderImages()
            }
            SAVE_PDF_REQUEST -> data.data?.let { createPdf(it) }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, PICK_IMAGES_REQUEST)
    }

    private fun addUri(uri: Uri) {
        if (images.any { it.uri == uri }) return
        val name = try {
            contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            } ?: "Image ${images.size + 1}"
        } catch (_: Exception) { "Image ${images.size + 1}" }
        images.add(ImageItem(uri, name))
    }

    private fun renderImages() {
        if (!::listBox.isInitialized) return
        listBox.removeAllViews()
        if (images.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No images selected yet"
                setTextColor(Color.GRAY); textSize = 14f
                setPadding(dp(14), dp(18), dp(14), dp(18))
                setBackgroundColor(Color.WHITE)
            }
            listBox.addView(empty)
        } else {
            images.forEachIndexed { index, item ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(8), dp(8))
                    setBackgroundColor(Color.WHITE)
                }
                val label = TextView(this).apply {
                    text = "${index + 1}. ${item.name}"
                    textSize = 14f; setTextColor(Color.rgb(35, 45, 70))
                    maxLines = 2; ellipsize = android.text.TextUtils.TruncateAt.END
                }
                row.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
                val up = smallButton("↑") { if (index > 0) { val x = images.removeAt(index); images.add(index - 1, x); renderImages() } }
                up.isEnabled = index > 0
                row.addView(up)
                val down = smallButton("↓") { if (index < images.lastIndex) { val x = images.removeAt(index); images.add(index + 1, x); renderImages() } }
                down.isEnabled = index < images.lastIndex
                row.addView(down)
                val remove = smallButton("×") { images.removeAt(index); renderImages() }
                row.addView(remove)
                listBox.addView(row, marginParams(bottom = 5))
            }
        }
        if (::createButton.isInitialized) createButton.isEnabled = images.isNotEmpty()
    }

    private fun startPdfSave() {
        if (images.isEmpty()) { toast("Select at least one image first."); return }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "Image_to_PDF_${System.currentTimeMillis()}.pdf")
        }
        startActivityForResult(intent, SAVE_PDF_REQUEST)
    }

    private fun createPdf(destination: Uri) {
        createButton.isEnabled = false
        status.text = "Creating PDF…"
        Thread {
            var document: PdfDocument? = null
            try {
                document = PdfDocument()
                val dimensions = pageDimensions()
                val marginPx = when (margin.selectedItemPosition) { 0 -> 0f; 1 -> 14f; 2 -> 28f; else -> 57f }
                images.forEachIndexed { index, item ->
                    val bitmap = decodeScaled(item.uri, 2400, 2400)
                    val page = document.startPage(PdfDocument.PageInfo.Builder(dimensions.first, dimensions.second, index + 1).create())
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    val available = RectF(marginPx, marginPx, dimensions.first - marginPx, dimensions.second - marginPx)
                    val scale = min(available.width() / bitmap.width, available.height() / bitmap.height)
                    val w = bitmap.width * scale
                    val h = bitmap.height * scale
                    val left = available.centerX() - w / 2f
                    val top = available.centerY() - h / 2f
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    canvas.drawBitmap(bitmap, null, RectF(left, top, left + w, top + h), paint)
                    document.finishPage(page)
                    bitmap.recycle()
                }
                contentResolver.openOutputStream(destination)?.use { document.writeTo(it) }
                    ?: throw IOException("Could not open the selected destination.")
                runOnUiThread { status.text = "PDF created successfully."; toast("PDF saved successfully.") }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Could not create PDF: ${e.message ?: "Unknown error"}"; toast("PDF creation failed.") }
            } finally {
                document?.close()
                runOnUiThread { createButton.isEnabled = images.isNotEmpty() }
            }
        }.start()
    }

    private fun decodeScaled(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Could not read image.")
        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IOException("Unsupported or unreadable image format.")
    }

    private fun pageDimensions(): Pair<Int, Int> {
        val base = when (pageSize.selectedItemPosition) {
            1 -> 612 to 792 // Letter, PDF points
            else -> 595 to 842 // A4, PDF points
        }
        return if (orientation.selectedItemPosition == 1) base.second to base.first else base
    }

    private fun spinner(items: List<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
        setPadding(dp(8), dp(4), dp(8), dp(4))
        setBackgroundColor(Color.WHITE)
    }
    private fun label(s: String) = text(s, 13, Color.rgb(50, 60, 80))
    private fun sectionTitle(s: String) = TextView(this).apply { text = s; textSize = 18f; setTextColor(Color.rgb(25, 35, 65)); typeface = android.graphics.Typeface.DEFAULT_BOLD }
    private fun text(s: String, size: Int, color: Int) = TextView(this).apply { text = s; textSize = size.toFloat(); setTextColor(color) }
    private fun button(s: String, action: () -> Unit) = Button(this).apply { text = s; isAllCaps = false; setOnClickListener { action() } }
    private fun smallButton(s: String, action: () -> Unit) = Button(this).apply { text = s; isAllCaps = false; minWidth = dp(38); minimumWidth = dp(38); setPadding(dp(2), 0, dp(2), 0); setOnClickListener { action() } }
    private fun matchParams() = LinearLayout.LayoutParams(-1, -2)
    private fun marginParams(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
