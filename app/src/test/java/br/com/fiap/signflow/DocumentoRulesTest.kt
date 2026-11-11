package br.com.fiap.signflow

import br.com.fiap.signflow.domain.model.CampoAssinatura
import br.com.fiap.signflow.domain.model.StatusDocumento
import br.com.fiap.signflow.domain.model.StatusSignatario.ASSINADO
import br.com.fiap.signflow.domain.model.StatusSignatario.PENDENTE
import br.com.fiap.signflow.domain.rules.DocumentoRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentoRulesTest {

    private fun campo(usuarioId: Long, x: Float = 0.1f, y: Float = 0.1f, pagina: Int = 0) =
        CampoAssinatura("c$usuarioId$x", usuarioId, pagina, x, y, 0.3f, 0.06f)

    @Test
    fun `status enviado quando ninguem assinou`() {
        assertEquals(StatusDocumento.ENVIADO, DocumentoRules.calcularStatus(listOf(PENDENTE, PENDENTE)))
    }

    @Test
    fun `status parcial quando parte assinou`() {
        assertEquals(StatusDocumento.PARCIAL, DocumentoRules.calcularStatus(listOf(ASSINADO, PENDENTE)))
    }

    @Test
    fun `status concluido somente quando todos assinaram`() {
        assertEquals(StatusDocumento.CONCLUIDO, DocumentoRules.calcularStatus(listOf(ASSINADO, ASSINADO)))
    }

    @Test
    fun `campo arrastado para fora volta para dentro da pagina`() {
        val limitado = DocumentoRules.limitar(campo(1, x = 0.95f, y = -0.2f))
        assertTrue(DocumentoRules.dentroDaPagina(limitado))
        assertEquals(0.7f, limitado.x, 0.0001f)
        assertEquals(0f, limitado.y, 0.0001f)
    }

    @Test
    fun `envio sem signatario e invalido`() {
        val erros = DocumentoRules.validarEnvio("Contrato", true, 1, emptyList(), emptyList())
        assertTrue(erros.any { it.contains("pelo menos um signatário") })
    }

    @Test
    fun `signatario sem campo de assinatura e invalido`() {
        val erros = DocumentoRules.validarEnvio("Contrato", true, 1, listOf(1L, 2L), listOf(campo(1)))
        assertTrue(erros.any { it.contains("pelo menos um campo") })
    }

    @Test
    fun `rascunho completo e valido`() {
        val erros = DocumentoRules.validarEnvio("Contrato", true, 2, listOf(1L, 2L), listOf(campo(1), campo(2, pagina = 1)))
        assertTrue(erros.isEmpty())
    }

    @Test
    fun `valida email e telefone`() {
        assertTrue(DocumentoRules.emailValido("ana@empresa.com.br"))
        assertFalse(DocumentoRules.emailValido("ana@"))
        assertTrue(DocumentoRules.telefoneValido("(11) 98765-4321"))
        assertFalse(DocumentoRules.telefoneValido("1234"))
    }
}
