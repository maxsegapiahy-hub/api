package com.example.model

import androidx.annotation.DrawableRes
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CardTier(
    val title: String,
    val subtitle: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long,
    val priceLabel: String
) {
    AFILIADO("Plano Max Afiliado", "Guerreiro Inicial", 0xFF5A0000, 0xFF1E1E24, 0xFFE0E0E0, "Kit R$ 150,00"),
    RONDA("Max Ronda Noturna", "Segurança Patrimonial", 0xFF1A237E, 0xFF121214, 0xFF90CAF9, "R$ 79,90/mês"),
    SOS("Max Saúde SOS 24h", "Salvaguarda de Vidas", 0xFFB71C1C, 0xFF121214, 0xFFFF5252, "R$ 119,90/mês"),
    CLUBE("Clube Max Vantagens", "Descontos & Telemedicina", 0xFF2E7D32, 0xFF121214, 0xFF81C784, "+ R$ 39,90/mês"),
    TOTAL("Max Proteção Total", "Combo VIP: Ronda + SOS + Clube", 0xFF8B0000, 0xFF0D0D10, 0xFFFFD700, "R$ 199,90/mês")
}

data class MemberCardData(
    val memberName: String = "MARCOS OLIVEIRA",
    val memberId: String = "00.584.2024-A",
    val city: String = "Apiaí - SP",
    val tier: CardTier = CardTier.TOTAL,
    val validThru: String = "12/2026",
    val emergencyContact: String = "Central Max Seg 24h",
    val emergencyPhone: String = "(15) 99700-0000",
    val bloodType: String = "O+",
    val isPaid: Boolean = true
)

data class PinGraduation(
    val id: String,
    val rankName: String,
    val rankTitle: String,
    @DrawableRes val imageRes: Int,
    val metalFinish: String,
    val enamelColor: String,
    val accentDetail: String,
    val requiredPoints: Int,
    val estimatedActivePlans: String,
    val directCommission: String,
    val indirectLevel2: String,
    val indirectLevel3: String,
    val specialBonus: String,
    val meaning: String,
    val metalColorHex: Long
)

data class PlanOffer(
    val id: String,
    val name: String,
    val subtitle: String,
    val priceMonthly: Double,
    val priceFormatted: String,
    val isPopular: Boolean,
    val categoryBadge: String,
    val features: List<String>,
    val commissionDirect: Double,
    val commissionLevel2: Double,
    val commissionLevel3: Double,
    val strategicValue: String
)

enum class PartnerStatus(val label: String, val colorHex: Long) {
    ATIVO("Ativo no Clube", 0xFF00E676),
    CONFIRMADO("Contrato Fechado", 0xFF29B6F6),
    PROSPECCAO("Em Prospecção", 0xFFFFD700)
}

@IgnoreExtraProperties
data class PartnerBusiness(
    @DocumentId val id: String = "",
    val segment: String = "",
    val businessName: String = "",
    val benefitDiscount: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val address: String = "",
    val status: PartnerStatus = PartnerStatus.ATIVO,
    val argumentPitch: String = "",
    val lat: Double = -24.5094,
    val lng: Double = -48.8427,
    val neighborhood: String = "Centro",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CommercialPitch(
    val id: String,
    val title: String,
    val targetAudience: String,
    val keyHooks: List<String>,
    val pitchBody: String,
    val whatsappMessage: String
)
