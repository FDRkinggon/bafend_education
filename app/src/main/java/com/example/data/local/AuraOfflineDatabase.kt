package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraOfflineDao {

    // 1. User Profiles SQLite Cache
    @Query("SELECT * FROM cached_user_profiles WHERE userId = :userId LIMIT 1")
    fun observeUserProfile(userId: String): Flow<CachedUserProfileEntity?>

    @Query("SELECT * FROM cached_user_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getUserProfileById(userId: String): CachedUserProfileEntity?

    @Query("SELECT * FROM cached_user_profiles ORDER BY updatedAtEpochMs DESC")
    fun observeAllUserProfiles(): Flow<List<CachedUserProfileEntity>>

    @Query("SELECT * FROM cached_user_profiles ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllUserProfiles(): List<CachedUserProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(entity: CachedUserProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfiles(entities: List<CachedUserProfileEntity>)

    @Query("DELETE FROM cached_user_profiles WHERE userId = :userId")
    suspend fun deleteUserProfileById(userId: String)

    // 2. Activities SQLite Cache
    @Query("SELECT * FROM cached_activities ORDER BY createdAtEpochMs DESC")
    fun observeAllActivities(): Flow<List<CachedActivityEntity>>

    @Query("SELECT * FROM cached_activities WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    fun observeActivitiesForUser(userId: String): Flow<List<CachedActivityEntity>>

    @Query("SELECT * FROM cached_activities ORDER BY createdAtEpochMs DESC")
    suspend fun getAllActivities(): List<CachedActivityEntity>

    @Query("SELECT * FROM cached_activities WHERE userId = :userId ORDER BY createdAtEpochMs DESC")
    suspend fun getActivitiesForUser(userId: String): List<CachedActivityEntity>

    @Query("SELECT * FROM cached_activities WHERE activityId = :activityId LIMIT 1")
    suspend fun getActivityById(activityId: String): CachedActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(entity: CachedActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(entities: List<CachedActivityEntity>)

    @Query("UPDATE cached_activities SET reviewStatus = :reviewStatus, updatedAtEpochMs = :updatedAtMs, isPendingOfflineSync = :isPending WHERE activityId = :activityId")
    suspend fun updateActivityReviewStatus(
        activityId: String,
        reviewStatus: String,
        updatedAtMs: Long = System.currentTimeMillis(),
        isPending: Boolean = false
    )

    @Query("DELETE FROM cached_activities WHERE activityId = :activityId")
    suspend fun deleteActivityById(activityId: String)

    @Query("DELETE FROM cached_activities WHERE userId = :userId")
    suspend fun deleteActivitiesForUser(userId: String)

    // 3. Admin Custom Subjects SQLite Cache
    @Query("SELECT * FROM cached_custom_subjects ORDER BY createdAtEpochMs DESC")
    fun observeCustomSubjects(): Flow<List<CachedCustomSubjectEntity>>

    @Query("SELECT * FROM cached_custom_subjects ORDER BY createdAtEpochMs DESC")
    suspend fun getAllCustomSubjects(): List<CachedCustomSubjectEntity>

    @Query("SELECT * FROM cached_custom_subjects WHERE subjectId = :subjectId LIMIT 1")
    suspend fun getCustomSubjectById(subjectId: String): CachedCustomSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomSubject(entity: CachedCustomSubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomSubjects(entities: List<CachedCustomSubjectEntity>)

    @Query("DELETE FROM cached_custom_subjects WHERE subjectId = :subjectId")
    suspend fun deleteCustomSubjectById(subjectId: String)

    // 4. Revision .bafend Documents SQLite Cache
    @Query("SELECT * FROM cached_revision_docs ORDER BY createdAtEpochMs DESC")
    fun observeRevisionDocs(): Flow<List<CachedRevisionDocEntity>>

    @Query("SELECT * FROM cached_revision_docs ORDER BY createdAtEpochMs DESC")
    suspend fun getAllRevisionDocs(): List<CachedRevisionDocEntity>

    @Query("SELECT * FROM cached_revision_docs WHERE docId = :docId LIMIT 1")
    suspend fun getRevisionDocById(docId: String): CachedRevisionDocEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevisionDoc(entity: CachedRevisionDocEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRevisionDocs(entities: List<CachedRevisionDocEntity>)

    @Query("DELETE FROM cached_revision_docs WHERE docId = :docId")
    suspend fun deleteRevisionDocById(docId: String)

    @Query("DELETE FROM cached_revision_docs WHERE docId LIKE 'seed_%'")
    suspend fun deleteMockSeededRevisionDocs()

    @Query("UPDATE cached_revision_docs SET isDownloadedLocally = :questionDownloaded, isSolutionDownloadedLocally = :solutionDownloaded WHERE docId = :docId")
    suspend fun updateRevisionDocLocalDownloadStatus(
        docId: String,
        questionDownloaded: Boolean,
        solutionDownloaded: Boolean
    )

    // 5. Offline Outbox Sync Queue (Actions performed offline waiting to sync to Firebase)
    @Query("SELECT * FROM offline_sync_outbox ORDER BY createdAtEpochMs ASC")
    fun observePendingSyncActions(): Flow<List<OfflineSyncActionEntity>>

    @Query("SELECT * FROM offline_sync_outbox ORDER BY createdAtEpochMs ASC")
    suspend fun getPendingSyncActions(): List<OfflineSyncActionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncAction(action: OfflineSyncActionEntity): Long

    @Query("DELETE FROM offline_sync_outbox WHERE queueId = :queueId")
    suspend fun deleteSyncActionById(queueId: Long)

    @Query("DELETE FROM offline_sync_outbox WHERE actionType = :actionType AND targetDocumentId = :targetDocId")
    suspend fun deleteMatchingSyncActions(actionType: String, targetDocId: String)

    @Query("UPDATE offline_sync_outbox SET retryCount = retryCount + 1, lastError = :error WHERE queueId = :queueId")
    suspend fun recordSyncActionError(queueId: Long, error: String)

    // 6. SQLite Phone Memory Telemetry Counts
    @Query("SELECT COUNT(*) FROM cached_user_profiles")
    fun observeCachedProfilesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_activities")
    fun observeCachedActivitiesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_custom_subjects")
    fun observeCachedSubjectsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_revision_docs")
    fun observeCachedRevisionDocsCount(): Flow<Int>
}

@Database(
    entities = [
        CachedUserProfileEntity::class,
        CachedActivityEntity::class,
        CachedCustomSubjectEntity::class,
        CachedRevisionDocEntity::class,
        OfflineSyncActionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AuraOfflineDatabase : RoomDatabase() {

    abstract fun offlineDao(): AuraOfflineDao

    companion object {
        const val DATABASE_NAME = "aura_phone_offline_cache.db"

        @Volatile
        private var instance: AuraOfflineDatabase? = null

        fun getInstance(context: Context): AuraOfflineDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraOfflineDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { instance = it }
            }
        }

        fun setTestInstance(db: AuraOfflineDatabase?) {
            synchronized(this) {
                instance = db
            }
        }
    }
}
