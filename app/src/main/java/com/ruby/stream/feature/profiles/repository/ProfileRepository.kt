package com.ruby.stream.feature.profiles.repository

import com.ruby.stream.data.database.dao.ProfileDao
import com.ruby.stream.data.database.entity.ProfileEntity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns PIN and recovery-phrase salt generation, hashing, and
 * verification for profiles. ProfileDao remains data access only
 * (read/write ProfileEntity) -- this repository is where the actual
 * credential policy lives, per the same layering already established
 * for AddonRepository/PlayerController: a lower layer never carries
 * business logic that belongs one level up.
 *
 * Locked threat model (see SOT "Profile/PIN"): casual/accidental access
 * by household members sharing one device, NOT offline database theft
 * or a determined attacker. This justifies a plain salted SHA-256 hash
 * rather than a slow KDF (BCrypt/Argon2/PBKDF2) -- the added complexity
 * of a slow KDF defends against a threat model Ruby does not have.
 * Same reasoning applies to the recovery phrase below.
 *
 * verifyPin() / verifyRecoveryPhrase() contract (locked): each returns
 * true only when the corresponding credential exists AND matches.
 * false for both "not configured" and "wrong value" -- this repository
 * answers exactly one question per credential ("does this verify?") and
 * does not decide whether it should have been requested in the first
 * place. That policy (when to prompt, retry handling, navigation after
 * success) belongs to the ViewModel that calls this repository,
 * mirroring the PASS 4 / PASS 6 boundary (AD-00T): PlayerController
 * owns the engine fact, the ViewModel owns what to do with it.
 *
 * Recovery phrase (locked): Owner-only concept, used solely to let the
 * Owner recover from a forgotten PIN without a nuclear (clear app data)
 * reset. A single phrase, not a bank of security questions -- see SOT
 * reasoning: multiple questions add real UX/normalization complexity
 * and, in a shared-household context, can be answerable by the same
 * people the PIN is meant to keep out, without actually adding
 * protection this threat model needs. Repository does not enforce
 * "Owner only" at the DB level; setRecoveryPhrase/clearRecoveryPhrase
 * are no-ops on a non-owner profileId, same no-op-over-throw pattern
 * used elsewhere in this repository. Normalized (trimmed, case-folded)
 * before hashing so verification isn't brittle to incidental
 * capitalization/whitespace differences between when it was set and
 * when it's recalled.
 *
 * Lockout/rate-limiting is explicitly DEFERRED, not implemented here --
 * see SOT Deferred Decisions. No retry counter, no cooldown, no
 * exponential backoff, no permanent lock in v1. Revisit only if Ruby's
 * threat model changes (e.g. cloud accounts, multi-user sync).
 */
interface ProfileRepository {

    /** Establishes or replaces this profile's PIN. */
    suspend fun setPin(profileId: Long, pin: String)

    /**
     * true only if profileId currently has a PIN configured AND
     * enteredPin matches it. false for both "no PIN configured" and
     * "wrong PIN" -- callers distinguish those cases themselves via
     * ProfileEntity.pinHash, not via this method's return value.
     */
    suspend fun verifyPin(profileId: Long, enteredPin: String): Boolean

    /** Removes PIN protection from this profile entirely. */
    suspend fun clearPin(profileId: Long)

    /**
     * Establishes or replaces the Owner's recovery phrase. No-op if
     * profileId does not refer to the Owner profile.
     */
    suspend fun setRecoveryPhrase(profileId: Long, phrase: String)

    /**
     * true only if profileId is the Owner, a recovery phrase is
     * configured, AND enteredPhrase matches it (after normalization).
     * false otherwise -- same "verify only, no policy" contract as
     * verifyPin().
     */
    suspend fun verifyRecoveryPhrase(profileId: Long, enteredPhrase: String): Boolean

    /** Removes the Owner's recovery phrase. No-op on a non-owner profileId. */
    suspend fun clearRecoveryPhrase(profileId: Long)
}

@Singleton
class DefaultProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) : ProfileRepository {

    override suspend fun setPin(profileId: Long, pin: String) {
        val profile = profileDao.findById(profileId) ?: return
        val salt = generateSalt()
        val hash = hash(salt, pin)
        profileDao.update(profile.copy(pinHash = hash, pinSalt = salt))
    }

    override suspend fun verifyPin(profileId: Long, enteredPin: String): Boolean {
        val profile = profileDao.findById(profileId) ?: return false
        val storedHash = profile.pinHash ?: return false
        val storedSalt = profile.pinSalt ?: return false
        val candidateHash = hash(storedSalt, enteredPin)
        return constantTimeEquals(storedHash, candidateHash)
    }

    override suspend fun clearPin(profileId: Long) {
        val profile = profileDao.findById(profileId) ?: return
        profileDao.update(profile.copy(pinHash = null, pinSalt = null))
    }

    override suspend fun setRecoveryPhrase(profileId: Long, phrase: String) {
        val profile = profileDao.findById(profileId) ?: return
        if (!profile.isOwner) return
        val salt = generateSalt()
        val hash = hash(salt, normalize(phrase))
        profileDao.update(
            profile.copy(recoveryPhraseHash = hash, recoveryPhraseSalt = salt)
        )
    }

    override suspend fun verifyRecoveryPhrase(
        profileId: Long,
        enteredPhrase: String
    ): Boolean {
        val profile = profileDao.findById(profileId) ?: return false
        if (!profile.isOwner) return false
        val storedHash = profile.recoveryPhraseHash ?: return false
        val storedSalt = profile.recoveryPhraseSalt ?: return false
        val candidateHash = hash(storedSalt, normalize(enteredPhrase))
        return constantTimeEquals(storedHash, candidateHash)
    }

    override suspend fun clearRecoveryPhrase(profileId: Long) {
        val profile = profileDao.findById(profileId) ?: return
        if (!profile.isOwner) return
        profileDao.update(
            profile.copy(recoveryPhraseHash = null, recoveryPhraseSalt = null)
        )
    }

    /**
     * Trims surrounding whitespace and case-folds so "Blue Whale " and
     * "blue whale" verify identically. Only applied to the recovery
     * phrase, not the PIN -- a PIN is digits-only by convention and has
     * no equivalent capitalization/whitespace ambiguity to normalize.
     */
    private fun normalize(phrase: String): String = phrase.trim().lowercase()

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(salt: String, value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + value).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Avoids a timing side-channel on hash comparison. Low-value against
     * this threat model specifically, but free to do correctly, so it's
     * done correctly rather than with a plain String == comparison.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
