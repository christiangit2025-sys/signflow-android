package br.com.fiap.signflow.data.files

import android.graphics.BitmapFactory
import br.com.fiap.signflow.core.Formatters
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import java.io.File
import java.util.UUID

/**
 * Gera o PDF final com PDFBox-Android:
 *  1. desenha a imagem de cada assinatura no campo posicionado (coordenadas relativas → pontos do PDF);
 *  2. adiciona uma página de autenticidade com protocolo, hash SHA-256 do original e trilha de eventos.
 */
class PdfStamper(private val storage: PdfStorage) {

    data class AssinaturaParaCarimbo(
        val usuarioId: Long,
        val nome: String,
        val email: String,
        val papel: String,
        val assinadoEm: Long,
        val caminhoImagem: String
    )

    data class CampoParaCarimbo(
        val usuarioId: Long,
        val pagina: Int,
        val x: Float,
        val y: Float,
        val largura: Float,
        val altura: Float
    )

    data class DadosAutenticidade(
        val titulo: String,
        val protocolo: String,
        val versao: Int,
        val hashOriginal: String,
        val emitidoPor: String,
        val eventos: List<Pair<Long, String>>
    )

    fun gerarFinal(
        caminhoOriginal: String,
        assinaturas: List<AssinaturaParaCarimbo>,
        campos: List<CampoParaCarimbo>,
        dados: DadosAutenticidade
    ): String {
        val destino = File(storage.pastaFinais, "${UUID.randomUUID()}.pdf")
        PDDocument.load(File(caminhoOriginal)).use { doc ->
            val porUsuario = assinaturas.associateBy { it.usuarioId }

            campos.forEach { campo ->
                val assinatura = porUsuario[campo.usuarioId] ?: return@forEach
                if (campo.pagina !in 0 until doc.numberOfPages) return@forEach
                val page = doc.getPage(campo.pagina)
                carimbar(doc, page, campo, assinatura)
            }

            adicionarPaginaDeAutenticidade(doc, assinaturas, dados)
            doc.save(destino)
        }
        return destino.absolutePath
    }

    private fun carimbar(doc: PDDocument, page: PDPage, campo: CampoParaCarimbo, assinatura: AssinaturaParaCarimbo) {
        val bitmap = BitmapFactory.decodeFile(assinatura.caminhoImagem) ?: return
        val imagem = LosslessFactory.createFromImage(doc, bitmap)
        val caixa = page.cropBox

        // (x, y) relativos ao canto superior esquerdo → PDF usa origem no canto inferior esquerdo
        val larguraCampo = campo.largura * caixa.width
        val alturaCampo = campo.altura * caixa.height
        val esquerda = caixa.lowerLeftX + campo.x * caixa.width
        val topo = caixa.upperRightY - campo.y * caixa.height
        val base = topo - alturaCampo

        val alturaLegenda = (alturaCampo * 0.22f).coerceIn(4f, 9f)
        val alturaImagem = alturaCampo - alturaLegenda
        val escala = minOf(larguraCampo / bitmap.width, alturaImagem / bitmap.height)
        val w = bitmap.width * escala
        val h = bitmap.height * escala

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
            cs.drawImage(imagem, esquerda + (larguraCampo - w) / 2f, base + alturaLegenda + (alturaImagem - h) / 2f, w, h)

            // linha e legenda abaixo da assinatura
            cs.setStrokingColor(0.42f, 0.21f, 0.85f)
            cs.setLineWidth(0.6f)
            cs.moveTo(esquerda, base + alturaLegenda)
            cs.lineTo(esquerda + larguraCampo, base + alturaLegenda)
            cs.stroke()

            val tamanhoFonte = (alturaLegenda * 0.75f).coerceIn(3.5f, 7f)
            val legenda = ajustar(
                "${assinatura.nome} - ${Formatters.dataHora(assinatura.assinadoEm)}",
                PDType1Font.HELVETICA, tamanhoFonte, larguraCampo
            )
            cs.beginText()
            cs.setNonStrokingColor(0.2f, 0.2f, 0.2f)
            cs.setFont(PDType1Font.HELVETICA, tamanhoFonte)
            cs.newLineAtOffset(esquerda, base + 1f)
            cs.showText(legenda)
            cs.endText()
        }
        bitmap.recycle()
    }

    private fun adicionarPaginaDeAutenticidade(
        doc: PDDocument,
        assinaturas: List<AssinaturaParaCarimbo>,
        dados: DadosAutenticidade
    ) {
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        val margem = 50f
        val larguraUtil = PDRectangle.A4.width - margem * 2
        var y = PDRectangle.A4.height - margem

        PDPageContentStream(doc, page).use { cs ->
            // faixa superior
            cs.setNonStrokingColor(0.36f, 0.17f, 0.82f)
            cs.addRect(0f, PDRectangle.A4.height - 8f, PDRectangle.A4.width, 8f)
            cs.fill()

            fun texto(t: String, fonte: PDFont, tamanho: Float, espaco: Float = tamanho + 6f) {
                quebrar(t, fonte, tamanho, larguraUtil).forEach { linha ->
                    cs.beginText()
                    cs.setNonStrokingColor(0.1f, 0.1f, 0.15f)
                    cs.setFont(fonte, tamanho)
                    cs.newLineAtOffset(margem, y)
                    cs.showText(linha)
                    cs.endText()
                    y -= espaco
                }
            }

            texto("SignFlow - Página de autenticidade", PDType1Font.HELVETICA_BOLD, 18f, 28f)
            texto("Documento: ${dados.titulo} (versão ${dados.versao})", PDType1Font.HELVETICA, 11f)
            texto("Protocolo: ${dados.protocolo}", PDType1Font.HELVETICA, 11f)
            texto("Emitido por: ${dados.emitidoPor}", PDType1Font.HELVETICA, 11f)
            texto("Hash SHA-256 do arquivo original:", PDType1Font.HELVETICA, 11f)
            texto(dados.hashOriginal, PDType1Font.COURIER, 8.5f, 18f)

            texto("Signatários", PDType1Font.HELVETICA_BOLD, 13f, 20f)
            assinaturas.forEach { a ->
                texto("${a.nome} <${a.email}> - ${a.papel}", PDType1Font.HELVETICA_BOLD, 10f, 14f)
                texto("Assinou em ${Formatters.dataHoraSegundos(a.assinadoEm)}", PDType1Font.HELVETICA, 10f, 20f)
            }

            y -= 4f
            texto("Histórico", PDType1Font.HELVETICA_BOLD, 13f, 20f)
            dados.eventos.forEach { (quando, descricao) ->
                if (y > margem + 40f) {
                    texto("${Formatters.dataHoraSegundos(quando)}  ${descricao}", PDType1Font.HELVETICA, 9.5f, 14f)
                }
            }

            y = margem
            texto(
                "Assinatura eletrônica coletada no aplicativo SignFlow (Challenge FIAP x LetsSign). " +
                    "Qualquer alteração no conteúdo original muda o hash acima.",
                PDType1Font.HELVETICA_OBLIQUE, 8f, 11f
            )
        }
    }

    /** As fontes padrão do PDF (WinAnsi) aceitam acentos do português, mas não emojis e afins. */
    private fun seguro(texto: String) = texto.map { c -> if (c.code in 32..255) c else '?' }.joinToString("")

    private fun ajustar(texto: String, fonte: PDFont, tamanho: Float, larguraMax: Float): String {
        var t = seguro(texto)
        while (t.length > 3 && fonte.getStringWidth(t) / 1000f * tamanho > larguraMax) {
            t = t.dropLast(2) + "."
        }
        return t
    }

    private fun quebrar(texto: String, fonte: PDFont, tamanho: Float, larguraMax: Float): List<String> {
        val linhas = mutableListOf<String>()
        var atual = StringBuilder()
        seguro(texto).split(" ").forEach { palavra ->
            val tentativa = if (atual.isEmpty()) palavra else "$atual $palavra"
            if (fonte.getStringWidth(tentativa) / 1000f * tamanho <= larguraMax || atual.isEmpty()) {
                atual = StringBuilder(tentativa)
            } else {
                linhas += atual.toString()
                atual = StringBuilder(palavra)
            }
        }
        if (atual.isNotEmpty()) linhas += atual.toString()
        return linhas
    }
}
