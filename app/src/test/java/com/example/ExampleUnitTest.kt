package com.example

import com.example.api.ApiPartnerDto
import com.example.api.toDomain
import com.example.api.toDto
import com.example.data.MaxSegDataRepository
import com.example.model.PartnerStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Testes unitários das regras de negócio, comissões e planos do Max Seg Apiahy.
 */
class ExampleUnitTest {

    @Test
    fun test_addition_sanity() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun test_career_commission_calculations() {
        // Regras: Max Total (N1 = R$ 30, N2 = R$ 10, N3 = R$ 4)
        val totalDirect = 10
        val totalLevel2 = 30
        val totalLevel3 = 60

        val incomeDirect = totalDirect * 30.0
        val incomeLevel2 = totalLevel2 * 10.0
        val incomeLevel3 = totalLevel3 * 4.0
        val totalMonthlyIncome = incomeDirect + incomeLevel2 + incomeLevel3

        assertEquals(300.0, incomeDirect, 0.01)
        assertEquals(300.0, incomeLevel2, 0.01)
        assertEquals(240.0, incomeLevel3, 0.01)
        assertEquals(840.0, totalMonthlyIncome, 0.01)

        val totalPoints = (totalDirect + totalLevel2 + totalLevel3) * 200
        assertEquals(20000, totalPoints)

        // Rank determination
        val calculatedRank = when {
            totalPoints >= 80000 -> "DIAMANTE"
            totalPoints >= 30000 -> "OURO"
            totalPoints >= 10000 -> "PRATA"
            totalPoints >= 3000 -> "BRONZE"
            totalDirect >= 1 -> "AFILIADO"
            else -> "INICIANTE"
        }
        assertEquals("PRATA", calculatedRank)
    }

    @Test
    fun test_career_ranks_thresholds() {
        fun calculateRank(direct: Int, totalPoints: Int): String {
            return when {
                totalPoints >= 80000 -> "DIAMANTE"
                totalPoints >= 30000 -> "OURO"
                totalPoints >= 10000 -> "PRATA"
                totalPoints >= 3000 -> "BRONZE"
                direct >= 1 -> "AFILIADO"
                else -> "INICIANTE"
            }
        }

        assertEquals("INICIANTE", calculateRank(0, 0))
        assertEquals("AFILIADO", calculateRank(1, 200))
        assertEquals("BRONZE", calculateRank(5, 3000))
        assertEquals("PRATA", calculateRank(10, 10000))
        assertEquals("OURO", calculateRank(20, 30000))
        assertEquals("DIAMANTE", calculateRank(50, 80000))
    }

    @Test
    fun test_data_repository_plans_offers() {
        val plans = MaxSegDataRepository.plansOffers
        assertTrue("Deve haver pelo menos 3 planos oficiais", plans.size >= 3)

        val totalPlan = plans.find { it.id == "total" }
        assertNotNull("Plano Total Spartan deve existir", totalPlan)
        assertEquals(30.0, totalPlan!!.commissionDirect, 0.01)
        assertEquals(10.0, totalPlan.commissionLevel2, 0.01)
        assertEquals(4.0, totalPlan.commissionLevel3, 0.01)
    }

    @Test
    fun test_data_repository_pins_graduations() {
        val pins = MaxSegDataRepository.pinsGraduations
        assertTrue("Deve haver pelo menos 4 pins de graduação", pins.size >= 4)

        val ranks = pins.map { it.rankName }
        assertTrue(ranks.contains("AFILIADO"))
        assertTrue(ranks.contains("BRONZE"))
        assertTrue(ranks.contains("PRATA"))
        assertTrue(ranks.contains("OURO"))
    }

    @Test
    fun test_partners_business_data_integrity() {
        val partners = MaxSegDataRepository.initialPartners
        assertTrue("Deve haver parceiros comerciais cadastrados em Apiaí", partners.isNotEmpty())

        partners.forEach { partner ->
            assertTrue("Nome do estabelecimento não pode ser vazio", partner.businessName.isNotBlank())
            assertTrue("Ramo do parceiro não pode ser vazio", partner.segment.isNotBlank())
            assertTrue("Benefício/Desconto deve estar especificado", partner.benefitDiscount.isNotBlank())
            assertTrue("Telefone deve estar preenchido", partner.phone.isNotBlank())
            assertTrue("Endereço deve conter referência a Apiaí", partner.address.contains("Apiaí") || partner.neighborhood.isNotBlank())
            assertTrue("Latitude deve estar na faixa de Apiaí-SP", partner.lat in -24.60..-24.40)
            assertTrue("Longitude deve estar na faixa de Apiaí-SP", partner.lng in -48.95..-48.75)
        }
    }

    @Test
    fun test_filter_partners_by_service_type_pharmacy_and_clinic() {
        val partners = MaxSegDataRepository.initialPartners
        val pharmacies = partners.filter {
            it.segment.contains("Farmácia", ignoreCase = true) || it.businessName.contains("Drogaria", ignoreCase = true)
        }
        val clinics = partners.filter {
            it.segment.contains("Clínica", ignoreCase = true) || it.segment.contains("Saúde", ignoreCase = true) || it.businessName.contains("Clínica", ignoreCase = true)
        }

        assertTrue("Deve encontrar pelo menos 2 farmácias em Apiaí", pharmacies.size >= 2)
        assertTrue("Deve encontrar pelo menos 2 clínicas/saúde em Apiaí", clinics.size >= 2)

        assertTrue(pharmacies.any { it.businessName.contains("Drogaria Central") })
        assertTrue(pharmacies.any { it.businessName.contains("São Paulo") })

        assertTrue(clinics.any { it.businessName.contains("Santa Helena") })
        assertTrue(clinics.any { it.businessName.contains("Fisioterapia") })
    }

    @Test
    fun test_search_partners_accent_insensitive() {
        fun normalize(str: String): String {
            return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
                .lowercase()
                .trim()
        }

        val partners = MaxSegDataRepository.initialPartners
        val query = "clinica"
        val queryNorm = normalize(query)

        val results = partners.filter {
            normalize(it.businessName).contains(queryNorm) || normalize(it.segment).contains(queryNorm)
        }

        assertTrue("Busca por 'clinica' sem acento deve retornar as clínicas acentuadas", results.isNotEmpty())
        assertTrue(results.all { normalize(it.businessName).contains("clinica") || normalize(it.segment).contains("clinica") })
    }

    @Test
    fun test_api_partner_dto_mapping() {
        val dto = com.example.api.ApiPartnerDto(
            id = "test_1",
            segment = "Farmácia",
            businessName = "Drogaria Teste Apiaí",
            benefitDiscount = "20% de desconto",
            contactPerson = "Farmacêutico Teste",
            phone = "(15) 3552-9999",
            address = "Rua Central, 100",
            neighborhood = "Centro",
            status = "ATIVO",
            argumentPitch = "Pitch de teste",
            lat = -24.5086,
            lng = -48.8465
        )

        val domain = dto.toDomain()
        assertEquals("test_1", domain.id)
        assertEquals("Farmácia", domain.segment)
        assertEquals("Drogaria Teste Apiaí", domain.businessName)
        assertEquals("20% de desconto", domain.benefitDiscount)
        assertEquals(PartnerStatus.ATIVO, domain.status)

        val backToDto = domain.toDto()
        assertEquals(dto.id, backToDto.id)
        assertEquals(dto.businessName, backToDto.businessName)
        assertEquals(dto.status, backToDto.status)
    }
}
