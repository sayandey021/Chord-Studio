package com.gbros.tabslite.utilities

import com.gbros.tabslite.data.Tab
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.Color
import java.io.File
import javax.imageio.ImageIO

object TabPdfExporter {

    fun exportToPdf(tab: Tab, transpose: Int, targetFile: File) {
        val document = PDDocument()
        try {
            val pageSize = PDRectangle.A4
            val pageWidth = pageSize.width
            val pageHeight = pageSize.height
            val marginLeft = 42f
            val marginRight = 42f
            val marginTop = 42f
            val marginBottom = 48f
            val usableWidth = pageWidth - marginLeft - marginRight

            val fontTitle = PDType1Font.HELVETICA_BOLD
            val fontSubtitle = PDType1Font.HELVETICA
            val fontMeta = PDType1Font.HELVETICA
            val fontSection = PDType1Font.HELVETICA_BOLD
            val fontBody = PDType1Font.COURIER
            val fontBodyBold = PDType1Font.COURIER_BOLD

            val colorPrimary = Color(15, 23, 42) // Dark Slate
            val colorSecondary = Color(71, 85, 105) // Slate
            val colorMuted = Color(148, 163, 184) // Light Slate
            val colorBorder = Color(226, 232, 240) // Subtle Line
            val colorAccent = Color(2, 132, 199) // Clean Blue for Chords/Sections

            // Load app logo for embedding in PDF
            val logoImage: PDImageXObject? = try {
                val stream = TabPdfExporter::class.java.getResourceAsStream("/app_icon_128.png")
                    ?: TabPdfExporter::class.java.getResourceAsStream("/app_icon.png")
                if (stream != null) {
                    val bImg = ImageIO.read(stream)
                    if (bImg != null) {
                        LosslessFactory.createFromImage(document, bImg)
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }

            var currentPage = PDPage(pageSize)
            document.addPage(currentPage)
            var contentStream = PDPageContentStream(document, currentPage)

            var y = pageHeight - marginTop

            // Helper to start new page
            fun startNewPage() {
                contentStream.close()
                currentPage = PDPage(pageSize)
                document.addPage(currentPage)
                contentStream = PDPageContentStream(document, currentPage)
                y = pageHeight - marginTop
            }

            // 1. Draw Brand Header & Title on First Page
            val title = tab.songName.ifBlank { "Untitled Tab" }
            val artist = tab.artistName.ifBlank { "Unknown Artist" }

            // Draw Top-Right Chord Studio App Logo & Brand Text
            if (logoImage != null) {
                val logoSize = 32f
                val logoX = pageWidth - marginRight - logoSize
                val logoY = y - 24f
                contentStream.drawImage(logoImage, logoX, logoY, logoSize, logoSize)

                val brandText = "CHORD STUDIO"
                val brandWidth = fontTitle.getStringWidth(brandText) / 1000f * 9.5f
                contentStream.beginText()
                contentStream.setFont(fontTitle, 9.5f)
                contentStream.setNonStrokingColor(colorAccent)
                contentStream.newLineAtOffset(logoX - brandWidth - 8f, logoY + 18f)
                contentStream.showText(brandText)
                contentStream.endText()

                val brandSub = "Precision Tablature"
                val brandSubWidth = fontMeta.getStringWidth(brandSub) / 1000f * 7.5f
                contentStream.beginText()
                contentStream.setFont(fontMeta, 7.5f)
                contentStream.setNonStrokingColor(colorMuted)
                contentStream.newLineAtOffset(logoX - brandSubWidth - 8f, logoY + 7f)
                contentStream.showText(brandSub)
                contentStream.endText()
            }

            // Song Title (Left aligned)
            contentStream.beginText()
            contentStream.setFont(fontTitle, 17f)
            contentStream.setNonStrokingColor(colorPrimary)
            contentStream.newLineAtOffset(marginLeft, y)
            contentStream.showText(title)
            contentStream.endText()
            y -= 19f

            // Artist & Version
            contentStream.beginText()
            contentStream.setFont(fontSubtitle, 11f)
            contentStream.setNonStrokingColor(colorSecondary)
            contentStream.newLineAtOffset(marginLeft, y)
            contentStream.showText("by $artist  •  ${tab.type} (Ver ${tab.version})")
            contentStream.endText()
            y -= 15f

            // Metadata Row (Key, Capo, Transpose, Difficulty)
            val metaParts = mutableListOf<String>()
            if (!tab.tonalityName.isNullOrBlank()) metaParts.add("Key: ${tab.tonalityName}")
            if (transpose != 0) metaParts.add("Transpose: ${if (transpose > 0) "+$transpose" else "$transpose"}")
            if (tab.capo > 0) metaParts.add("Capo: ${tab.capo}th fret")
            metaParts.add("Difficulty: ${tab.difficulty.ifBlank { "All Levels" }}")

            contentStream.beginText()
            contentStream.setFont(fontMeta, 8.5f)
            contentStream.setNonStrokingColor(colorSecondary)
            contentStream.newLineAtOffset(marginLeft, y)
            contentStream.showText(metaParts.joinToString("   |   "))
            contentStream.endText()
            y -= 10f

            // Top Separator Divider Line
            contentStream.setStrokingColor(colorBorder)
            contentStream.setLineWidth(0.8f)
            contentStream.moveTo(marginLeft, y)
            contentStream.lineTo(pageWidth - marginRight, y)
            contentStream.stroke()
            y -= 16f

            // 2. Prepare Transposed Tab Lines
            val transposedContent = TabTransposer.transposeContent(tab.content, transpose)
            val rawLines = transposedContent.lines()

            val fontSize = 9.5f
            val lineHeight = 12.5f

            for (rawLine in rawLines) {
                // Check if page break is needed
                if (y < marginBottom + lineHeight * 2) {
                    startNewPage()
                }

                val trimmed = rawLine.trim()

                // Check for Section Headers like [Verse 1], [Chorus], [Intro], etc.
                if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.contains("[ch]")) {
                    y -= 4f
                    if (y < marginBottom + lineHeight) {
                        startNewPage()
                    }
                    contentStream.beginText()
                    contentStream.setFont(fontSection, 10f)
                    contentStream.setNonStrokingColor(colorAccent)
                    contentStream.newLineAtOffset(marginLeft, y)
                    contentStream.showText(trimmed)
                    contentStream.endText()
                    y -= (lineHeight + 2f)
                    continue
                }

                // Parse chords markup [ch]...[/ch] and [tab]...[/tab]
                // Strip tags for clean monospace alignment
                val cleanLine = rawLine
                    .replace("[ch]", "")
                    .replace("[/ch]", "")
                    .replace("[tab]", "")
                    .replace("[/tab]", "")

                // Render in monospace font
                val isChordLine = rawLine.contains("[ch]")

                contentStream.beginText()
                if (isChordLine) {
                    contentStream.setFont(fontBodyBold, fontSize)
                    contentStream.setNonStrokingColor(colorAccent)
                } else {
                    contentStream.setFont(fontBody, fontSize)
                    contentStream.setNonStrokingColor(colorPrimary)
                }
                contentStream.newLineAtOffset(marginLeft, y)

                // Sanitize string to remove characters not representable in standard PDF Type 1 encoding
                val sanitized = cleanLine.map { ch ->
                    if (ch.code in 32..126) ch else ' '
                }.joinToString("")

                // Truncate or fit line if too long
                val maxCharsPerLine = (usableWidth / (fontSize * 0.6f)).toInt()
                val lineToPrint = if (sanitized.length > maxCharsPerLine) {
                    sanitized.take(maxCharsPerLine)
                } else {
                    sanitized
                }

                contentStream.showText(lineToPrint)
                contentStream.endText()

                y -= lineHeight
            }

            contentStream.close()

            // 3. Draw Footers on all pages
            val totalPages = document.numberOfPages
            for (i in 0 until totalPages) {
                val page = document.getPage(i)
                val footerStream = PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)

                // Divider line above footer
                footerStream.setStrokingColor(colorBorder)
                footerStream.setLineWidth(0.6f)
                footerStream.moveTo(marginLeft, 34f)
                footerStream.lineTo(pageWidth - marginRight, 34f)
                footerStream.stroke()

                // Left: Logo + "Chord Studio"
                var textStartX = marginLeft
                if (logoImage != null) {
                    footerStream.drawImage(logoImage, marginLeft, 18f, 11f, 11f)
                    textStartX += 15f
                }

                footerStream.beginText()
                footerStream.setFont(fontTitle, 8f)
                footerStream.setNonStrokingColor(colorSecondary)
                footerStream.newLineAtOffset(textStartX, 20f)
                footerStream.showText("Chord Studio")
                footerStream.endText()

                // Right: "Page X of Y"
                val footerText = "Page ${i + 1} of $totalPages"
                val textWidth = fontMeta.getStringWidth(footerText) / 1000f * 8f
                footerStream.beginText()
                footerStream.setFont(fontMeta, 8f)
                footerStream.setNonStrokingColor(colorMuted)
                footerStream.newLineAtOffset(pageWidth - marginRight - textWidth, 20f)
                footerStream.showText(footerText)
                footerStream.endText()

                footerStream.close()
            }

            document.save(targetFile)
        } finally {
            document.close()
        }
    }
}
