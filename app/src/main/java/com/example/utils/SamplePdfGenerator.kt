package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

object SamplePdfGenerator {
    fun generateWelcomePdf(context: Context): File {
        val file = File(context.filesDir, "Welcome_to_Pure_PDF.pdf")
        if (file.exists()) return file

        val pdfDocument = PdfDocument()

        // Page 1: Preface
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in standard postscript points
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fill background with elegant soft white
            val paintBg = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, 595f, 842f, paintBg)

            // Primary typography paint
            val paintText = Paint().apply {
                color = Color.parseColor("#1B1B1F")
                isAntiAlias = true
            }

            // Title
            paintText.textSize = 28f
            paintText.isFakeBoldText = true
            canvas.drawText("Pure PDF Reader", 50f, 100f, paintText)

            // Subtitle
            paintText.textSize = 14f
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#44474E")
            canvas.drawText("Elegance, Privacy, and Speed — Without Ads", 50f, 130f, paintText)

            // Divider line
            val paintDivider = Paint().apply {
                color = Color.parseColor("#E1E2E9")
                strokeWidth = 2f
            }
            canvas.drawLine(50f, 160f, 545f, 160f, paintDivider)

            // Greeting & Body Elements
            paintText.color = Color.parseColor("#1B1B1F")
            paintText.textSize = 12f
            var y = 200f

            val paragraphs = listOf(
                "Welcome to Pure PDF!",
                "Unlike standard file readers cluttered with banners, analytics trackers,",
                "and paywalls, Pure PDF is built as a clean utility workspace.",
                "",
                "✨ ADVANCED KEY VIRTUES & CHARACTERISTICS:",
                " • Completely Ad-Free: Peaceful interface to absorb content.",
                " • Maximum Native Privacy: Fully self-contained local sandboxing.",
                " • Infinite Smooth Scrolling: Memory-cached background rendering layers.",
                " • Clean Minimalism Style: Styled with soft backgrounds and high contrast.",
                " • Compact & Lightweight: Designed entirely in Jetpack Compose.",
                "",
                "🛠️ GETTING STARTED GUIDE:",
                " 1. Pick a File: Click the Open (+) icon from the Dashboard screen.",
                " 2. Dynamic History: Your last-opened location is stored securely.",
                " 3. Bookmarks: Save particular pages to return to them instantly.",
                " 4. Eye-Safety Mode: Toggle dark contrast filters for custom lighting."
            )

            for (line in paragraphs) {
                if (line.startsWith("✨") || line.startsWith("🛠️")) {
                    paintText.isFakeBoldText = true
                    paintText.textSize = 14f
                    y += 12f
                } else if (line.startsWith("Welcome")) {
                    paintText.isFakeBoldText = true
                    paintText.textSize = 13f
                } else {
                    paintText.isFakeBoldText = false
                    paintText.textSize = 12f
                }
                canvas.drawText(line, 50f, y, paintText)
                y += 24f
            }

            // Footer
            paintText.textSize = 10f
            paintText.color = Color.parseColor("#A8ABB4")
            paintText.isFakeBoldText = true
            canvas.drawText("PAGE 1 OF 3  •  PURE PDF USER MANUAL", 190f, 800f, paintText)

            pdfDocument.finishPage(page)
        }

        // Page 2: Navigation Features
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintBg = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, 595f, 842f, paintBg)

            val paintText = Paint().apply {
                color = Color.parseColor("#1B1B1F")
                isAntiAlias = true
                textSize = 20f
                isFakeBoldText = true
            }

            canvas.drawText("Navigation & Page Manipulation", 50f, 100f, paintText)

            paintText.textSize = 12f
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#44474E")
            canvas.drawText("Custom tools that let you handle dense materials with precision.", 50f, 125f, paintText)

            val paintDivider = Paint().apply {
                color = Color.parseColor("#E1E2E9")
                strokeWidth = 2f
            }
            canvas.drawLine(50f, 150f, 545f, 150f, paintDivider)

            // Content paragraphs
            paintText.color = Color.parseColor("#1B1B1F")
            paintText.textSize = 12f
            var y = 190f

            val layoutList = listOf(
                "🔎 Layout Scaling and Zoom Controls:",
                " When reading sheets or fine technical maps, pinch-to-zoom is supported.",
                " You can also tap the dynamic scaling pill (+ / - buttons at the bottom)",
                " to scale pages seamlessly between 50% and 300%.",
                "",
                "📈 Smart Jump & Seek Slider:",
                " Scroll horizontally or vertically, or drag the bottom seek bar to cycle",
                " through pages without any delay. For massive books, tap the page",
                " indicator bubble directly to enter a target page number to jump instantly.",
                "",
                "📌 Persistent Local Landmarks:",
                " Need to flag a section for reference? In the reader view, tap the upper",
                " right bookmark button. Give the landmark a distinct name (e.g. 'Thesis Hypothesis',",
                " 'Chapter 2 Appendix') and save it. It will instantly pin in Room and show up",
                " under your Bookmarks sub-tabs.",
                "",
                "🧹 File History Cleanup:",
                " Long press any document in history to reveal removal confirmation options.",
                " Removing file metadata from Pure PDF does not delete the original physical",
                " document from your storage space — keeping your file system secure."
            )

            for (line in layoutList) {
                if (line.endsWith(":") || line.startsWith("🔎") || line.startsWith("📈") || line.startsWith("📌") || line.startsWith("🧹")) {
                    paintText.isFakeBoldText = true
                    paintText.textSize = 14f
                    y += 12f
                } else {
                    paintText.isFakeBoldText = false
                    paintText.textSize = 12f
                }
                canvas.drawText(line, 50f, y, paintText)
                y += 24f
            }

            paintText.textSize = 10f
            paintText.color = Color.parseColor("#A8ABB4")
            paintText.isFakeBoldText = true
            canvas.drawText("PAGE 2 OF 3  •  PURE PDF USER MANUAL", 190f, 800f, paintText)

            pdfDocument.finishPage(page)
        }

        // Page 3: Security & Support
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 3).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paintBg = Paint().apply { color = Color.WHITE }
            canvas.drawRect(0f, 0f, 595f, 842f, paintBg)

            val paintText = Paint().apply {
                color = Color.parseColor("#1B1B1F")
                isAntiAlias = true
                textSize = 20f
                isFakeBoldText = true
            }

            canvas.drawText("Privacy & File Sandboxing Tech", 50f, 100f, paintText)

            paintText.textSize = 12f
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#44474E")
            canvas.drawText("Understanding how your permissions and local storage are guarded.", 50f, 125f, paintText)

            val paintDivider = Paint().apply {
                color = Color.parseColor("#E1E2E9")
                strokeWidth = 2f
            }
            canvas.drawLine(50f, 150f, 545f, 150f, paintDivider)

            paintText.color = Color.parseColor("#1B1B1F")
            paintText.textSize = 12f
            var y = 190f

            val textGroup = listOf(
                "🔒 Modern Permissionless Access:",
                " Pure PDF leverages Android's Storage Access Framework (SAF). Only documents",
                " chosen explicitly by you are granted access. This completely avoids requesting",
                " broad 'MANAGE_EXTERNAL_STORAGE' or 'READ_EXTERNAL_STORAGE' scan rights,",
                " making Pure PDF highly privacy-first and secure. No hidden threads, no data outbound.",
                "",
                "💾 File State Retention & Resuming:",
                " When you exit standard system activities, your last viewed page position is",
                " archived into Room database persistence. When reopening the file later",
                " from the home screen, Pure PDF automatically scrolls back perfectly to let you",
                " continue where you paused.",
                "",
                "🌟 Open-Source Spirit & Transparency:",
                " Pure PDF is built with pure developer appreciation, zero paywall restrictions,",
                " zero advertising platforms, and zero user-telemetry aggregators. Just robust,",
                " clean, premium reading space. Feel free to export your documents cleanly!"
            )

            for (line in textGroup) {
                if (line.endsWith(":") || line.startsWith("🔒") || line.startsWith("💾") || line.startsWith("🌟")) {
                    paintText.isFakeBoldText = true
                    paintText.textSize = 14f
                    y += 12f
                } else {
                    paintText.isFakeBoldText = false
                    paintText.textSize = 12f
                }
                canvas.drawText(line, 50f, y, paintText)
                y += 24f
            }

            paintText.textSize = 10f
            paintText.color = Color.parseColor("#A8ABB4")
            paintText.isFakeBoldText = true
            canvas.drawText("PAGE 3 OF 3  •  PURE PDF USER MANUAL", 190f, 800f, paintText)

            pdfDocument.finishPage(page)
        }

        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }

        return file
    }
}
