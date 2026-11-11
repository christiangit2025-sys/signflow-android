package br.com.fiap.signflow.domain.model

enum class Perfil { ADMIN, SIGNATARIO }

/** Classificação do signatário no documento. */
enum class Papel(val rotulo: String) {
    CONTRATANTE("Contratante"),
    CONTRATADO("Contratado"),
    TESTEMUNHA("Testemunha")
}

/** Situação geral do documento. É sempre calculada a partir das assinaturas. */
enum class StatusDocumento(val rotulo: String) {
    ENVIADO("Enviado"),
    PARCIAL("Parcialmente assinado"),
    CONCLUIDO("Concluído")
}

/** Situação individual de cada signatário no documento. */
enum class StatusSignatario(val rotulo: String) {
    PENDENTE("Pendente"),
    ASSINADO("Assinado")
}

enum class TipoEvento(val rotulo: String) {
    CRIADO("Documento criado"),
    ENVIADO("Documento enviado"),
    CONVITE("Convite enviado"),
    VISUALIZADO("Documento visualizado"),
    ASSINADO("Documento assinado"),
    CONCLUIDO("Documento concluído"),
    NOVA_VERSAO("Nova versão criada")
}
