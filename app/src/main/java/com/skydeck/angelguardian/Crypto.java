/*
 * Secure Password Storage – Don’ts, dos
 *
 * The Don’ts:
 * - Don’t store authentication data unless you really have to.
 * - If you must store authentication data, for Gosling’s sake don’t store the passwords in clear text.
 * - Don’t use two-way encryption unless you really need to retrieve the clear-text password.
 *   You do not need to use the user’s original clear-text password to perform authentication in your application.
 * - Don’t use outdated hashing algorithms like MD5.
 *   Go to http://www.md5hacker.com/ and you can decrypt an MD5-hashed password in seconds.
 * - Don’t come up with your own encryption scheme, unless you’ve invented the next great successor
 *   to PBKDF2 or bcrypt.
 *
 * The Dos:
 * - Choose a one-way encryption algorithm. As I mentioned above, once you’ve encrypted and stored
 *   a user’s password, you never need to know the real value again. When a user attempts to
 *   authenticate, you’ll just apply the same algorithm to the password they entered, and compare
 *   that to the encrypted password that you stored.
 * - Make the encryption as slow as your application can tolerate.
 * - Pick a well-known algorithm. The National Institute of Standards and Technology (NIST)
 *   recommends PBKDF2 for passwords. bcrypt is a popular and established alternative, and scrypt
 *   is a relatively new algorithm that has been well-received.
 */

package com.skydeck.angelguardian;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Crypto library for encryption and decryption.
 * encrypt/decrypt using PBKDF2.
 *
 * PBKDF2:
 * - Recommended by the NIST.
 * - Adjustable key stretching to defeat brute force attacks. The basic idea of key stretching is
 *   that after you apply your hashing algorithm to the password, you then continue to apply the
 *   same algorithm to the result many times (the iteration count).
 * - A required salt to defeat rainbow table attacks and prevent collisions with other users.
 *   A salt is a randomly generated sequence of bits that is unique to each user and is added to
 *   the user’s password as part of the hashing.
 * - Part of Java SE 6.
 *
 * The call flow goes as below:
 * 1. When adding a new user, call generateSalt(), then getEncryptedPassword(), and store both the
 *    encrypted password and the salt. Do not store the clear-text password. Don’t worry about
 *    keeping the salt in a separate table or location from the encrypted password;
 *    as discussed above, the salt is non-secret.
 * 2. When authenticating a user, retrieve the previously encrypted password and salt from the
 *    database, then send those and the clear-text password they entered to authenticate().
 *    If it returns true, authentication succeeded.
 * 3. When a user changes their password, it’s safe to reuse their old salt; you can just call
 *    getEncryptedPassword() with the old salt.
 */

public class Crypto {
    public boolean authenticate(String attemptedPassword, byte[] encryptedPassword, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Encrypt the clear-text password using the same salt that was used to
        // encrypt the original password
        byte[] encryptedAttemptedPassword = getEncryptedPassword(attemptedPassword, salt);

        // Authentication succeeds if encrypted password that the user entered
        // is equal to the stored hash
        return Arrays.equals(encryptedPassword, encryptedAttemptedPassword);
    }

    public byte[] getEncryptedPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        // PBKDF2 with SHA-1 as the hashing algorithm. Note that the NIST specifically names
        // SHA-1 as an acceptable hashing algorithm for PBKDF2
        String algorithm = "PBKDF2WithHmacSHA1";
        // SHA-1 generates 160 bit hashes, so that's what makes sense here
        int derivedKeyLength = 160;
        // Pick an iteration count that works for you. The NIST recommends at least 1,000 iterations:
        // http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf
        // iOS 4.x reportedly uses 10,000:
        // http://blog.crackpassword.com/2010/09/smartphone-forensics-cracking-blackberry-backup-passwords/
        int iterations = 20000;

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength);

        SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);

        return f.generateSecret(spec).getEncoded();
    }

    public byte[] generateSalt() throws NoSuchAlgorithmException {
        // VERY important to use SecureRandom instead of just Random
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        // Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
        byte[] salt = new byte[8];
        random.nextBytes(salt);

        return salt;
    }
}
