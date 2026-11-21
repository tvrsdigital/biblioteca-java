package br.com.biblioteca.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilitário de senhas: gera salt, calcula hash PBKDF2 e verifica senhas.
 * Usa apenas classes que já vêm no Java (sem dependência externa).
 */
public final class SenhaUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 600_000;   // repetições do cálculo: lento de propósito
    private static final int TAMANHO_HASH = 256;    // em bits
    private static final int TAMANHO_SALT = 16;     // em bytes

    // SecureRandom: gerador de números aleatórios próprio para segurança
    // (o Random comum é previsível e não serve para isso)
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SenhaUtil() {
    }

    /** Gera um salt aleatório, em texto (Base64), para guardar no banco. */
    public static String gerarSalt() {
        byte[] salt = new byte[TAMANHO_SALT];
        ALEATORIO.nextBytes(salt);
        // Base64 transforma bytes em texto, para caber numa coluna TEXT
        return Base64.getEncoder().encodeToString(salt);
    }

    /** Calcula o hash da senha com o salt informado. Mesma senha + mesmo salt = mesmo hash. */
    public static String gerarHash(String senha, String salt) {
        try {
            PBEKeySpec especificacao = new PBEKeySpec(
                senha.toCharArray(),
                Base64.getDecoder().decode(salt),
                ITERACOES,
                TAMANHO_HASH
            );
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance(ALGORITMO);
            byte[] hash = fabrica.generateSecret(especificacao).getEncoded();
            especificacao.clearPassword();   // apaga a senha da memória assim que possível
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Multi-catch: um catch para dois tipos de exceção.
            // Esses erros só aconteceriam se o Java não tivesse o algoritmo,
            // o que não ocorre: é um erro de configuração, não do usuário.
            // Por isso viram uma exceção unchecked, sem "poluir" quem chama.
            throw new IllegalStateException("Falha ao calcular o hash da senha.", e);
        }
    }

    /**
     * Verifica se a senha digitada corresponde ao hash guardado.
     * Não "descriptografa" nada: recalcula o hash e compara.
     */
    public static boolean verificar(String senhaDigitada, String salt, String hashGuardado) {
        String hashCalculado = gerarHash(senhaDigitada, salt);
        // MessageDigest.isEqual compara em tempo constante:
        // não "denuncia", pelo tempo de resposta, quantos caracteres bateram
        return MessageDigest.isEqual(
            hashCalculado.getBytes(StandardCharsets.UTF_8),
            hashGuardado.getBytes(StandardCharsets.UTF_8)
        );
    }
}