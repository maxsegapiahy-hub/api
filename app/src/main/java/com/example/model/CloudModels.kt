package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val fullName: String = "MARCOS OLIVEIRA",
    val email: String = "",
    val documentCpf: String = "123.456.789-00",
    val phone: String = "(15) 99700-0000",
    val planId: String = "total",
    val tier: String = "TOTAL",
    val cardCode: String = "00.584.2024-A",
    val bloodType: String = "O+",
    val city: String = "Apiaí - SP",
    val status: String = "ATIVO",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EmergencyContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val relationship: String = "",
    val phone: String = "",
    val isPrimary: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SosAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val latitude: Double = -24.5086,
    val longitude: Double = -48.8465,
    val address: String = "Apiaí - SP",
    val status: String = "DISPATCHED",
    val dispatchedUnit: String = "V-02 Resgate APH SOS",
    val estimatedMinutes: Long = 4L,
    val notes: String = "Acionamento SOS via App Max Seg",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
