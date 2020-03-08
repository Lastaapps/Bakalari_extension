package cz.lastaapps.bakalariextension.tools

import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.charset.Charset
import java.security.Key
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal


/**
 * @source https://medium.com/@ericfu/securely-storing-secrets-in-an-android-application-501f030ae5a3
 * Encrypts/decrypts users data(password)
 * Failed...
 * */
class Crypto {

    companion object {
        private val TAG = Crypto::class.java.simpleName


        fun encrypt(mess: String): String {
            return mess
            return if (checkV23()) V23after.encrypt(
                mess
            ) else V23before.encrypt(
                mess
            )
        }

        fun decrypt(mess: String): String {
            return mess
            return if (checkV23()) V23after.decrypt(
                mess
            ) else V23before.decrypt(
                mess
            )
        }

        private fun checkV23(): Boolean {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        }

        //NOT WORKING
        @RequiresApi(Build.VERSION_CODES.M)
        private class V23after {
            companion object {
                private const val AndroidKeyStore = "AndroidKeyStore"
                private const val AES_MODE = "AES/GCM/NoPadding"
                private const val KEY_ALIAS = "NOBODY_EXPECTED_SPANISH_INQUISITION"
                private const val XEALTH_KEY_ALIAS = "SPAM"
                private const val FIXED_IV = "HOW_DOES_THIS_WORK???"
                private var keyStore: KeyStore? = null

                private fun generate() {
                    keyStore = KeyStore.getInstance(
                        AndroidKeyStore
                    )
                    keyStore?.load(null)

                    if (!keyStore?.containsAlias(
                            KEY_ALIAS
                        )!!) {
                        val keyGenerator: KeyGenerator = KeyGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_AES,
                            AndroidKeyStore
                        )
                        keyGenerator.init(
                            KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                            )
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(
                                    KeyProperties.ENCRYPTION_PADDING_NONE
                                )
                                .setRandomizedEncryptionRequired(false)
                                .build()
                        )
                        keyGenerator.generateKey()
                    }
                }

                @Throws(Exception::class)
                private fun getSecretKey(): Key? {
                    if (keyStore == null) generate()
                    return keyStore?.getKey(
                        XEALTH_KEY_ALIAS, null)
                }

                internal fun encrypt(mess: String): String {
                    val input = mess.toByteArray(Charset.forName("UTF-8"))
                    val c: Cipher = Cipher.getInstance(AES_MODE)
                    c.init(
                        Cipher.ENCRYPT_MODE,
                        getSecretKey(),
                        GCMParameterSpec(128, FIXED_IV.toByteArray())
                    )
                    val encodedBytes: ByteArray = c.doFinal(input)
                    return Base64.encodeToString(encodedBytes, Base64.DEFAULT)
                }

                internal fun decrypt(mess: String): String {
                    val input = mess.toByteArray(Charset.forName("UTF-8"))
                    val c = Cipher.getInstance(AES_MODE)
                    c.init(
                        Cipher.DECRYPT_MODE,
                        getSecretKey(),
                        GCMParameterSpec(128, FIXED_IV.toByteArray())
                    )
                    return String(c.doFinal(input))
                }
            }
        }

        //NOT WORKING
        private class V23before {
            companion object {
                private const val AndroidKeyStore = "AndroidKeyStore"
                private const val KEY_ALIAS = "EUROPEAN_SHALLOW"
                private const val RSA_MODE = "RSA/ECB/PKCS1Padding"
                private var keyStore: KeyStore? = null

                private fun generate() {
                    keyStore = KeyStore.getInstance(
                        AndroidKeyStore
                    )
                    keyStore?.load(null)

                    // Generate the RSA key pairs
                    // Generate the RSA key pairs
                    if (!keyStore?.containsAlias(
                            KEY_ALIAS
                        )!!) { // Generate a key pair for encryption
                        val start: Calendar = Calendar.getInstance()
                        val end: Calendar = Calendar.getInstance()
                        end.add(Calendar.YEAR, 30)
                        val spec = KeyPairGeneratorSpec.Builder(App.appContext())
                            .setAlias(KEY_ALIAS)
                            .setSubject(X500Principal("CN=$KEY_ALIAS"))
                            .setSerialNumber(BigInteger.TEN)
                            .setStartDate(start.time)
                            .setEndDate(end.time)
                            .build()
                        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_RSA,
                            AndroidKeyStore
                        )
                        kpg.initialize(spec)
                        kpg.generateKeyPair()
                    }
                }

                @Throws(java.lang.Exception::class)
                internal fun encrypt(mess: String): String {
                    generate()
                    val secret = mess.toByteArray()
                    val privateKeyEntry =
                        keyStore!!.getEntry(
                            KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
                    // Encrypt the text
                    val inputCipher =
                        Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
                    inputCipher.init(
                        Cipher.ENCRYPT_MODE,
                        privateKeyEntry.certificate.publicKey
                    )
                    val outputStream = ByteArrayOutputStream()
                    val cipherOutputStream =
                        CipherOutputStream(outputStream, inputCipher)
                    cipherOutputStream.write(secret)
                    cipherOutputStream.close()
                    return String(outputStream.toByteArray())
                }

                @Throws(java.lang.Exception::class)
                internal fun decrypt(mess: String): String {
                    generate()
                    val encrypted = mess.toByteArray()
                    val privateKeyEntry =
                        keyStore!!.getEntry(
                            KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
                    val output =
                        Cipher.getInstance(RSA_MODE, "AndroidOpenSSL")
                    output.init(Cipher.DECRYPT_MODE, privateKeyEntry.privateKey)
                    val cipherInputStream = CipherInputStream(
                        ByteArrayInputStream(encrypted), output
                    )
                    val values: ArrayList<Byte> = ArrayList()
                    var nextByte: Int
                    while (cipherInputStream.read().also { nextByte = it } != -1) {
                        values.add(nextByte.toByte())
                    }
                    val bytes = ByteArray(values.size)
                    for (i in bytes.indices) {
                        bytes[i] = values[i]
                    }
                    return String(bytes)
                }
            }
        }

    }

}