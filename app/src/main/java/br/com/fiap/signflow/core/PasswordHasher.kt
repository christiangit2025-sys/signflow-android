package br.com.fiap.signflow.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Senhas nunca são gravadas em texto puro: usamos PBKDF2 com salt aleatório por usuário.
 */
object PasswordHasher {
    private const val ITERACOES = 120_000
    private const val TAMANHO_CHAVE = 256

    data class Hash(val hash: String, val salt: String)

    fun gerar(senha: String): Hash {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Hash(derivar(senha, salt), Base64.getEncoder().encodeToString(salt))
    }

    fun confere(senha: String, hash: String, salt: String): Boolean {
        val calculado = derivar(senha, Base64.getDecoder().decode(salt))
        // comparação em tempo constante
        return MessageDigest.isEqual(calculado.toByteArray(), hash.toByteArray())
    }

    private fun derivar(senha: String, salt: ByteArray): String {
        val spec = PBEKeySpec(senha.toCharArray(), salt, ITERACOES, TAMANHO_CHAVE)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(bytes)
    }
}
