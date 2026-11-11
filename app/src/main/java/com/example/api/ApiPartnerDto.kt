package com.example.api

import com.example.model.PartnerBusiness
import com.example.model.PartnerStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Object (DTO) para intercâmbio de dados com os endpoints da API maxsegapiahy-hub/api.
 */
@JsonClass(generateAdapter = true)
data class ApiPartnerDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "segment") val segment: String = "Comércio",
    @Json(name = "businessName") val businessName: String = "",
    @Json(name = "benefitDiscount") val benefitDiscount: String = "",
    @Json(name = "contactPerson") val contactPerson: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "address") val address: String = "Apiaí - SP",
    @Json(name = "neighborhood") val neighborhood: String = "Centro",
    @Json(name = "status") val status: String = "ATIVO",
    @Json(name = "argumentPitch") val argumentPitch: String = "",
    @Json(name = "lat") val lat: Double = -24.5086,
    @Json(name = "lng") val lng: Double = -48.8465
)

fun ApiPartnerDto.toDomain(): PartnerBusiness {
    val statusEnum = try {
        PartnerStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
        PartnerStatus.ATIVO
    }
    return PartnerBusiness(
        id = id.ifBlank { "partner_${System.currentTimeMillis()}" },
        segment = segment,
        businessName = businessName,
        benefitDiscount = benefitDiscount,
        contactPerson = contactPerson,
        phone = phone,
        address = address,
        neighborhood = neighborhood,
        status = statusEnum,
        argumentPitch = argumentPitch,
        lat = lat,
        lng = lng
    )
}

fun PartnerBusiness.toDto(): ApiPartnerDto {
    return ApiPartnerDto(
        id = id,
        segment = segment,
        businessName = businessName,
        benefitDiscount = benefitDiscount,
        contactPerson = contactPerson,
        phone = phone,
        address = address,
        neighborhood = neighborhood,
        status = status.name,
        argumentPitch = argumentPitch,
        lat = lat,
        lng = lng
    )
}
