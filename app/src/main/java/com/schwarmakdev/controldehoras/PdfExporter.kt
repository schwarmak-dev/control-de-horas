package com.schwarmakdev.controldehoras

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.schwarmakdev.controldehoras.data.entity.Proyecto
import com.schwarmakdev.controldehoras.data.entity.SesionTiempo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_WIDTH  = 595  // A4 en puntos (72dpi)
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 40f
    private const val LINE_H      = 18f

    fun generateAndShare(
        context: Context,
        sessions: List<SesionTiempo>,
        projects: List<Proyecto>
    ) {
        val projMap = projects.associateBy { it.id }
        val doc     = PdfDocument()
        var pageNum = 1
        var info    = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page    = doc.startPage(info)
        var canvas  = page.canvas
        var y       = MARGIN

        // ── Paints ──────────────────────────────────────────────────────────────
        val titlePaint = Paint().apply {
            color     = Color.parseColor("#10B981")
            textSize  = 16f
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color     = Color.parseColor("#6B7280")
            textSize  = 9f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color     = Color.parseColor("#1F2937")
            textSize  = 9f
            typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color     = Color.parseColor("#111827")
            textSize  = 8f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color     = Color.parseColor("#E5E7EB")
            strokeWidth = 0.5f
        }

        // ── Helper: nueva página ─────────────────────────────────────────────────
        fun newPage(): Canvas {
            doc.finishPage(page)
            pageNum++
            info   = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page   = doc.startPage(info)
            y      = MARGIN
            return page.canvas
        }

        fun checkY(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) canvas = newPage()
        }

        // ── Encabezado ──────────────────────────────────────────────────────────
        canvas.drawText("HOJA DE ASISTENCIA Y REPORTES OFICIALES", MARGIN, y, titlePaint)
        y += LINE_H
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generado el: $nowStr    |    Total sesiones: ${sessions.size}", MARGIN, y, subPaint)
        y += LINE_H * 0.5f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += LINE_H

        // ── Resumen por proyecto ─────────────────────────────────────────────────
        canvas.drawText("RESUMEN POR PROYECTO", MARGIN, y, headerPaint)
        y += LINE_H

        val minutesByProj = sessions.groupBy { it.proyectoId }
            .mapValues { (_, s) -> s.sumOf { it.duracionMinutos } }

        for ((projId, mins) in minutesByProj) {
            checkY(LINE_H)
            val name = projMap[projId]?.nombre ?: "Desconocido"
            val meta = projMap[projId]?.horasMetaGlobal?.toInt() ?: 0
            val h    = mins / 60
            val m    = mins % 60
            canvas.drawText(
                "  $name:  ${h}h ${m}min  /  Meta: ${meta}h",
                MARGIN, y, bodyPaint
            )
            y += LINE_H
        }

        y += LINE_H * 0.5f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += LINE_H

        // ── Tabla de sesiones ───────────────────────────────────────────────────
        canvas.drawText("DETALLE DE SESIONES", MARGIN, y, headerPaint)
        y += LINE_H

        // Encabezado de tabla
        val cols = listOf(
            Pair(MARGIN,         "FECHA"),
            Pair(MARGIN + 68f,   "PROYECTO"),
            Pair(MARGIN + 185f,  "INICIO"),
            Pair(MARGIN + 225f,  "FIN"),
            Pair(MARGIN + 265f,  "MIN"),
            Pair(MARGIN + 295f,  "MÉTODO"),
            Pair(MARGIN + 355f,  "NOTAS")
        )
        for ((x, label) in cols) canvas.drawText(label, x, y, headerPaint)
        y += 4f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += LINE_H * 0.8f

        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        for (sess in sessions.sortedByDescending { it.horaInicio }) {
            checkY(LINE_H)
            val name    = (projMap[sess.proyectoId]?.nombre ?: "?").take(16)
            val inicio  = timeFmt.format(Date(sess.horaInicio))
            val fin     = timeFmt.format(Date(sess.horaFin))
            val metodo  = if (sess.metodoRegistro == "temporizador") "Timer" else "Manual"
            val notas   = sess.notas.take(28)

            canvas.drawText(sess.fecha,             cols[0].first, y, bodyPaint)
            canvas.drawText(name,                   cols[1].first, y, bodyPaint)
            canvas.drawText(inicio,                 cols[2].first, y, bodyPaint)
            canvas.drawText(fin,                    cols[3].first, y, bodyPaint)
            canvas.drawText("${sess.duracionMinutos}", cols[4].first, y, bodyPaint)
            canvas.drawText(metodo,                 cols[5].first, y, bodyPaint)
            canvas.drawText(notas,                  cols[6].first, y, bodyPaint)
            y += LINE_H
        }

        // ── Pie de firma ────────────────────────────────────────────────────────
        checkY(LINE_H * 5)
        y += LINE_H
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += LINE_H
        canvas.drawText("Firma del Estudiante / Consultor: _______________________________", MARGIN, y, bodyPaint)
        y += LINE_H * 2
        canvas.drawText("Firma del Supervisor / Líder de Proyecto: _______________________", MARGIN, y, bodyPaint)

        doc.finishPage(page)

        // ── Guardar y compartir ─────────────────────────────────────────────────
        val file = File(context.cacheDir, "reporte_horas_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type     = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte_Horas_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Exportar PDF"))
    }
}
