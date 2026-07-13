package com.ruby.stream.feature.profiles.repository

import com.ruby.stream.data.database.dao.ProfileDao
import com.ruby.stream.data.database.entity.ProfileEntity
import com.ruby.stream.data.database.entity.ProfileType
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 6 (Session 25, AD-025) — the single façade over ALL profile
 * persistence: observing/creating/updating/deleting profiles, switching
 * which one is active, and (unchanged from before this revision) PIN
 * and recovery-phrase management. Supersedes, not merely extends, the
 * narrower AD-010 scope this repository originally had (PIN/recovery-
 * phrase only) -- confirmed against the actual file before expanding it
 * that it had zero CRUD of any kind prior to this revision.
 *
 * Every profile-related ViewModel has exactly ONE dependency:
 * ProfileRepository. ProfileDao remains data-access only, same as
 * always -- this expansion does not change PASS 0B, only what sits on
 * top of it.
 *
 * ACTIVE-PROFILE OWNERSHIP (resolved this session, a real decision, not
 * just wiring): "which profile is currently active" is persisted
 * DataStore-backed state, but it is NOT exposed through
 * SettingsRepository (AD-024). The deciding test is ownership of the
 * CONCEPT, not device-wide-vs-per-profile: once ProfileRepository is
 * the single façade for the whole profile domain (CRUD, PIN, owner
 * rules, switching), the active profile is part of THAT domain, and
 * splitting it into SettingsRepository would force ProfileRepository to
 * depend on SettingsRepository just to implement switchActiveProfile(),
 * and would force every future active-profile consumer to potentially
 * need both repositories. Persistence mechanism (DataStore) and domain
 * owner (ProfileRepository) are kept separate on purpose: if the
 * mechanism ever changes, nothing outside this repository needs to
 * know. SettingsRepository stays focused on user-configurable
 * application settings (AD-024's own charter), not general device
 * state that happens to also use DataStore.
 *
 * Exposes ProfileEntity directly rather than introducing a separate
 * domain-layer "Profile" type -- consistent with how AddonRepository
 * already exposes InstalledAddonEntity directly, and how PASS 5 only
 * introduced narrower purpose-built shapes (e.g.
 * SettingsAddonsUiState.InstalledAddonItem) at the specific screen that
 * needed a trimmed view, not as a repository-wide wrapper nothing else
 * asked for.
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
 *
 * OWNER-DELETION DEFENSE IN DEPTH (AD-023): deleteProfile() is the
 * final authority regardless of caller -- ProfileDao.delete()
 * deliberately does not enforce this itself (see its own doc comment),
 * so this repository is where "cannot delete the last Owner" actually
 * lives. Returns DeleteProfileResult rather than throwing or returning
 * a bare Boolean, mirroring this repository's own existing
 * verify-only-answers-one-question contract. The UI additionally
 * disabling the delete action when it can already tell this would fail
 * is a courtesy layer on top of this, not a substitute for it.
 */
sealed interface DeleteProfileResult {
    data object Success : DeleteProfileResult
    data object CannotDeleteOwner : DeleteProfileResult
    data object ProfileNotFound : DeleteProfileResult
}

/**
 * Returned by createProfile()/updateProfile() -- mirrors the
 * DuplicateName error state already locked in CreateProfileUiState/
 * ProfileEditorUiState (PASS 5), so the ViewModel can map this directly
 * to that existing UI state rather than needing its own translation
 * layer.
 */
sealed interface SaveProfileResult {
    data class Success(val profileId: Long) : SaveProfileResult
    data object DuplicateName : SaveProfileResult
    data object ProfileNotFound : SaveProfileResult
}

interface ProfileRepository {

    /** All profiles, Owner first then by creation order (see ProfileDao). */
    fun observeProfiles(): Flow<List<ProfileEntity>>

    /**
     * The currently active profile, or null if none has been set yet
     * (e.g. first launch, before any profile has been selected) or the
     * previously-active profile has since been deleted.
     */
    fun observeActiveProfile(): Flow<ProfileEntity?>

    /** Persists which profile is currently active. */
    suspend fun setActiveProfile(profileId: Long)

    /**
     * Creates a new profile. Fails with DuplicateName if another
     * profile already has this name (see the unique index on
     * profiles.name, AD-013/Session 11) -- checked explicitly via
     * ProfileDao.existsByName() first so this returns a clean result
     * rather than surfacing a raw SQLiteConstraintException from
     * insert() to the caller.
     */
    suspend fun createProfile(
        name: String,
        profileType: ProfileType,
        avatarUrl: String?,
    ): SaveProfileResult

    /**
     * Updates an existing profile's name/type/avatar/contentRatingLevel.
     * Fails with DuplicateName if another profile (any OTHER id) already
     * has this name -- the AD-013-locked exclusion pattern
     * (WHERE name = :name AND id != :profileId) so a no-op save of an
     * unchanged name never self-collides. Does not touch isOwner, PIN,
     * or recovery-phrase fields -- those remain the responsibility of
     * their own dedicated methods below.
     */
    suspend fun updateProfile(
        profileId: Long,
        name: String,
        profileType: ProfileType,
        avatarUrl: String?,
        contentRatingLevel: String?,
    ): SaveProfileResult

    /**
     * Deletes a profile. Refuses with CannotDeleteOwner if this is the
     * last remaining Owner profile (AD-023) -- this is the actual
     * enforcement point; ProfileDao.delete() deliberately does not
     * check this itself (see its own doc comment). If the deleted
     * profile was the active one, the caller must separately call
     * setActiveProfile() with a new choice -- this method does not
     * silently pick one, since which profile becomes active next is a
     * ViewModel/UI decision, not a repository one.
     */
    suspend fun deleteProfile(profileId: Long): DeleteProfileResult

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

private val Context.activeProfileDataStore by preferencesDataStore(name = "ruby_active_profile")

@Singleton
class DefaultProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    @ApplicationContext private val context: Context,
) : ProfileRepository {

    private object Keys {
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
    }

    override fun observeProfiles(): Flow<List<ProfileEntity>> = profileDao.observeAll()

    override fun observeActiveProfile(): Flow<ProfileEntity?> {
        return context.activeProfileDataStore.data
            .map { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] }
            .distinctUntilChanged()
            .flatMapLatest { activeId ->
                if (activeId == null) {
                    flowOf<ProfileEntity?>(null)
                } else {
                    profileDao.observeAll().map { profiles -> profiles.find { it.id == activeId } }
                }
            }
    }

    override suspend fun setActiveProfile(profileId: Long) {
        context.activeProfileDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    override suspend fun createProfile(
        name: String,
        profileType: ProfileType,
        avatarUrl: String?,
    ): SaveProfileResult {
        if (profileDao.existsByName(name, excludingId = 0L)) return SaveProfileResult.DuplicateName
        val id = profileDao.insert(
            ProfileEntity(
                name = name,
                createdAt = System.currentTimeMillis(),
                avatarUrl = avatarUrl,
                profileType = profileType,
            )
        )
        return SaveProfileResult.Success(id)
    }

    override suspend fun updateProfile(
        profileId: Long,
        name: String,
        profileType: ProfileType,
        avatarUrl: String?,
        contentRatingLevel: String?,
    ): SaveProfileResult {
        val profile = profileDao.findById(profileId)
            ?: return SaveProfileResult.ProfileNotFound
        if (profileDao.existsByName(name, excludingId = profileId)) {
            return SaveProfileResult.DuplicateName
        }
        profileDao.update(
            profile.copy(
                name = name,
                profileType = profileType,
                avatarUrl = avatarUrl,
                contentRatingLevel = contentRatingLevel,
            )
        )
        return SaveProfileResult.Success(profileId)
    }

    override suspend fun deleteProfile(profileId: Long): DeleteProfileResult {
        val profile = profileDao.findById(profileId)
            ?: return DeleteProfileResult.ProfileNotFound
        if (profile.isOwner) {
            val owner = profileDao.findOwner()
            // Only one Owner row can exist at a time in practice, but
            // this checks the actual invariant (would deleting THIS
            // profile leave zero Owners) rather than assuming isOwner
            // alone is sufficient -- defensive against any future path
            // that might create a second Owner row.
            if (owner?.id == profileId) return DeleteProfileResult.CannotDeleteOwner
        }
        profileDao.delete(profile)
        return DeleteProfileResult.Success
    }

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
