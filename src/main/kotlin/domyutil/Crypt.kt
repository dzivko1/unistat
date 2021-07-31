package domyutil

import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hides the specified string in a way that prevents it from being read in plain text. Note that this is not equivalent
 * to encryption and should not be used as such. The purpose of this is only to obfuscate potentially sensitive information.
 */
fun obfuscate(input: String): String {
    val key = KeyGenerator.getInstance("AES").generateKey()
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = cipher.parameters.getParameterSpec(IvParameterSpec::class.java).iv
    val cryptoText = cipher.doFinal(input.toByteArray())
    val stitched = base64Encode(key.encoded) + ':' + base64Encode(iv) + ':' + base64Encode(cryptoText)
    return base64Encode(stitched.toByteArray())
}

/**
 * Deobfuscates a string previously obfuscated by passing it through the [obfuscate] function. Note that this kind of
 * obfuscation is not equivalent to encryption and should not be used as such. The purpose of this is only to obfuscate
 * potentially sensitive information.
 */
fun deobfuscate(input: String): String {
    val parts = base64Decode(input).decodeToString().split(':')
    val keyBytes = base64Decode(parts[0])
    val iv = base64Decode(parts[1])
    val cryptoText = base64Decode(parts[2])

    val key = SecretKeySpec(keyBytes, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
    return cipher.doFinal(cryptoText).decodeToString()
}

private fun base64Encode(input: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(input)
private fun base64Decode(input: String): ByteArray = Base64.getUrlDecoder().decode(input)