package com.example.data

import android.content.Context
import com.example.R
import com.example.model.EmergencyContact
import com.example.model.MemberCardData
import com.example.model.PartnerBusiness
import com.example.model.PartnerStatus
import com.example.model.SosAlert
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.example.api.ApiClient
import com.example.api.toDomain
import kotlinx.coroutines.tasks.await

class MaxSegCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário precisa estar autenticado na Conta Max.")
    }

    // 1. Perfil do Associado
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        val docRef = db.collection("users").document(userId)
        
        docRef.snapshots()
            .map { snapshot -> snapshot.toObject(UserProfile::class.java) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)

        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "fullName" to profile.fullName,
                "email" to (auth.currentUser?.email ?: profile.email),
                "documentCpf" to profile.documentCpf,
                "phone" to profile.phone,
                "planId" to profile.planId,
                "tier" to profile.tier,
                "cardCode" to profile.cardCode,
                "bloodType" to profile.bloodType,
                "city" to profile.city,
                "status" to profile.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            // Se for novo, inicializa createdAt
            val existing = docRef.get().await()
            if (!existing.exists() || !existing.contains("createdAt")) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // Sincronização do perfil local com a conta Max
    suspend fun syncLocalProfile(localData: MemberCardData, email: String?): Result<UserProfile> {
        val uid = requireUserId()
        val cloudProfile = UserProfile(
            userId = uid,
            fullName = localData.memberName,
            email = email ?: auth.currentUser?.email ?: "",
            phone = localData.emergencyPhone,
            tier = localData.tier.name,
            cardCode = localData.memberId,
            bloodType = localData.bloodType,
            city = localData.city,
            status = if (localData.isPaid) "ATIVO" else "PENDENTE"
        )
        val saveResult = saveUserProfile(cloudProfile)
        return if (saveResult.isSuccess) {
            Result.success(cloudProfile)
        } else {
            Result.failure(saveResult.exceptionOrNull() ?: Exception("Falha ao sincronizar"))
        }
    }

    // 2. Contatos de Emergência
    fun observeEmergencyContacts(userId: String): Flow<List<EmergencyContact>> = flow {
        val path = "users/$userId/emergencyContacts"
        db.collection("users").document(userId).collection("emergencyContacts")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(EmergencyContact::class.java) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun addEmergencyContact(contact: EmergencyContact): Result<Unit> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("emergencyContacts")
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "name" to contact.name,
                "relationship" to contact.relationship,
                "phone" to contact.phone,
                "isPrimary" to contact.isPrimary,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            colRef.add(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteEmergencyContact(contactId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("emergencyContacts").document(contactId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // 3. Botão SOS e Despacho de Socorro com GPS
    suspend fun dispatchSosAlert(
        latitude: Double,
        longitude: Double,
        address: String = "Apiaí - SP",
        notes: String = "Acionamento de Emergência via App Max Seg"
    ): Result<SosAlert> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("sosAlerts")
        return try {
            val payload = hashMapOf<String, Any>(
                "userId" to uid,
                "latitude" to latitude,
                "longitude" to longitude,
                "address" to address,
                "status" to "DISPATCHED",
                "dispatchedUnit" to "V-02 Resgate APH SOS",
                "estimatedMinutes" to 4L,
                "notes" to notes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = colRef.add(payload).await()
            val alert = SosAlert(
                id = doc.id,
                userId = uid,
                latitude = latitude,
                longitude = longitude,
                address = address,
                status = "DISPATCHED",
                dispatchedUnit = "V-02 Resgate APH SOS",
                estimatedMinutes = 4L,
                notes = notes
            )
            Result.success(alert)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    fun observeSosAlerts(userId: String): Flow<List<SosAlert>> = flow {
        val path = "users/$userId/sosAlerts"
        db.collection("users").document(userId).collection("sosAlerts")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(SosAlert::class.java) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    // 4. Rede de Parceiros Comerciais de Apiaí (Firestore)
    fun observePartners(): Flow<List<PartnerBusiness>> = flow {
        val path = "partners"
        db.collection("partners")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val statusStr = doc.getString("status") ?: "ATIVO"
                        val statusEnum = try {
                            PartnerStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            PartnerStatus.ATIVO
                        }
                        PartnerBusiness(
                            id = doc.id,
                            businessName = doc.getString("businessName") ?: "",
                            segment = doc.getString("segment") ?: "Comércio",
                            benefitDiscount = doc.getString("benefitDiscount") ?: "Desconto Especial Clube Max",
                            contactPerson = doc.getString("contactPerson") ?: "",
                            phone = doc.getString("phone") ?: "",
                            address = doc.getString("address") ?: "Apiaí - SP",
                            neighborhood = doc.getString("neighborhood") ?: "Centro",
                            status = statusEnum,
                            argumentPitch = doc.getString("argumentPitch") ?: "",
                            lat = doc.getDouble("lat") ?: -24.5086,
                            lng = doc.getDouble("lng") ?: -48.8465,
                            createdAt = doc.getTimestamp("createdAt"),
                            updatedAt = doc.getTimestamp("updatedAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun seedInitialPartnersIfEmpty(): Result<Unit> {
        val colRef = db.collection("partners")
        return try {
            val snapshot = colRef.get().await()
            val existingIds = snapshot.documents.map { it.id }.toSet()
            val initialList = MaxSegDataRepository.initialPartners
            val missing = initialList.filter { it.id !in existingIds }
            if (missing.isNotEmpty()) {
                val batch = db.batch()
                missing.forEach { partner ->
                    val docRef = colRef.document(partner.id)
                    val payload = hashMapOf<String, Any>(
                        "businessName" to partner.businessName,
                        "segment" to partner.segment,
                        "benefitDiscount" to partner.benefitDiscount,
                        "contactPerson" to partner.contactPerson,
                        "phone" to partner.phone,
                        "address" to partner.address,
                        "neighborhood" to partner.neighborhood,
                        "status" to partner.status.name,
                        "argumentPitch" to partner.argumentPitch,
                        "lat" to partner.lat,
                        "lng" to partner.lng,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    batch.set(docRef, payload)
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun addPartner(partner: PartnerBusiness): Result<String> {
        val colRef = db.collection("partners")
        return try {
            val payload = hashMapOf<String, Any>(
                "businessName" to partner.businessName,
                "segment" to partner.segment,
                "benefitDiscount" to partner.benefitDiscount,
                "contactPerson" to partner.contactPerson,
                "phone" to partner.phone,
                "address" to partner.address,
                "neighborhood" to partner.neighborhood,
                "status" to partner.status.name,
                "argumentPitch" to partner.argumentPitch,
                "lat" to partner.lat,
                "lng" to partner.lng,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val doc = if (partner.id.isNotBlank()) {
                val ref = colRef.document(partner.id)
                ref.set(payload).await()
                ref
            } else {
                colRef.add(payload).await()
            }
            Result.success(doc.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePartnerStatus(partnerId: String, newStatus: PartnerStatus): Result<Unit> {
        val docRef = db.collection("partners").document(partnerId)
        return try {
            docRef.update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePartner(partnerId: String): Result<Unit> {
        val docRef = db.collection("partners").document(partnerId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // 5. Integração com a API maxsegapiahy-hub/api
    suspend fun syncPartnersFromApi(): Result<List<PartnerBusiness>> {
        return try {
            val response = ApiClient.service.getPartners()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val domainPartners = dtos.map { it.toDomain() }
                if (domainPartners.isNotEmpty()) {
                    val colRef = db.collection("partners")
                    val batch = db.batch()
                    domainPartners.forEach { partner ->
                        val docRef = colRef.document(partner.id)
                        val payload = hashMapOf<String, Any>(
                            "businessName" to partner.businessName,
                            "segment" to partner.segment,
                            "benefitDiscount" to partner.benefitDiscount,
                            "contactPerson" to partner.contactPerson,
                            "phone" to partner.phone,
                            "address" to partner.address,
                            "neighborhood" to partner.neighborhood,
                            "status" to partner.status.name,
                            "argumentPitch" to partner.argumentPitch,
                            "lat" to partner.lat,
                            "lng" to partner.lng,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        batch.set(docRef, payload)
                    }
                    batch.commit().await()
                }
                Result.success(domainPartners)
            } else {
                Result.failure(Exception("API returned code: ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
