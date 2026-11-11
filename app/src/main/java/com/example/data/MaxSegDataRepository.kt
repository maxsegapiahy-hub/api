package com.example.data

import com.example.R
import com.example.model.*

object MaxSegDataRepository {

    val pinsGraduations = listOf(
        PinGraduation(
            id = "afiliado",
            rankName = "AFILIADO",
            rankTitle = "O Guerreiro da Linha de Frente",
            imageRes = R.drawable.img_pin_afiliado,
            metalFinish = "Metal Zamac com Pintura Soft Enamel",
            enamelColor = "Bordô Imperial & Prata Fosco",
            accentDetail = "Tarraxa Borboleta metálica reforçada",
            requiredPoints = 1,
            estimatedActivePlans = "1 Cliente Ativo",
            directCommission = "15% Recorrente (R$ 30/mês no Max Total)",
            indirectLevel2 = "-",
            indirectLevel3 = "-",
            specialBonus = "10% de desconto nos serviços Max Seg",
            meaning = "Representa o início da jornada na proteção de Apiaí.",
            metalColorHex = 0xFFCCCCCC
        ),
        PinGraduation(
            id = "bronze",
            rankName = "BRONZE",
            rankTitle = "Líder de Vizinhança",
            imageRes = R.drawable.img_pin_bronze,
            metalFinish = "Banho de Cobre Envelhecido com Pátina e Resina",
            enamelColor = "Bronze Escuro & Bordô Metálico",
            accentDetail = "Inscrição 'BRONZE' em relevo na base",
            requiredPoints = 3000,
            estimatedActivePlans = "15 a 20 Planos Max Total na Rede",
            directCommission = "15% Recorrente",
            indirectLevel2 = "5% sobre o Nível 2 (R$ 10/plano)",
            indirectLevel3 = "-",
            specialBonus = "Isenção total da sua mensalidade Max Seg/Saúde",
            meaning = "O primeiro grande reconhecimento de liderança comunitária.",
            metalColorHex = 0xFFCD7F32
        ),
        PinGraduation(
            id = "prata",
            rankName = "PRATA",
            rankTitle = "Guardião de Elite",
            imageRes = R.drawable.img_pin_afiliado, // fallback high quality asset
            metalFinish = "Banho de Cromo / Níquel de Alto Brilho (Prata 925)",
            enamelColor = "Vermelho Rubí Metálico Espelhado",
            accentDetail = "Inscrição 'PRATA' em baixo-relevo polido",
            requiredPoints = 10000,
            estimatedActivePlans = "50 a 60 Planos Max Total na Rede",
            directCommission = "15% Recorrente",
            indirectLevel2 = "5% Recorrente (Nível 2)",
            indirectLevel3 = "2% Recorrente (Nível 3 - R$ 4/plano)",
            specialBonus = "Auxílio Combustível fixo de R$ 300,00/mês",
            meaning = "Líder consolidado com rede multiplicadora em toda a cidade.",
            metalColorHex = 0xFFE0E0E0
        ),
        PinGraduation(
            id = "ouro",
            rankName = "OURO",
            rankTitle = "Comandante de Expansão",
            imageRes = R.drawable.img_pin_ouro,
            metalFinish = "Banhado a Ouro 24k com Polimento Joalheiro",
            enamelColor = "Bordô Imperial com Crista Dourada",
            accentDetail = "Gravação a laser 'OURO' e espada dourada",
            requiredPoints = 30000,
            estimatedActivePlans = "150 Planos Max Total na Rede",
            directCommission = "15% Recorrente",
            indirectLevel2 = "5% Recorrente",
            indirectLevel3 = "2% Recorrente + 1% Bônus Global",
            specialBonus = "Smartphone / Tablet topo de linha + R$ 4.000 a R$ 5.500/mês",
            meaning = "Comandante de equipe com autoridade e alta rentabilidade.",
            metalColorHex = 0xFFFFD700
        ),
        PinGraduation(
            id = "diamante",
            rankName = "DIAMANTE",
            rankTitle = "General Fundador",
            imageRes = R.drawable.img_pin_diamante,
            metalFinish = "Acabamento Black Piano (Black Nickel) com Titânio",
            enamelColor = "Preto Obsidiana com Detalhes Cromados",
            accentDetail = "1 Pedra Zircônia Diamantada incrustada no elmo",
            requiredPoints = 80000,
            estimatedActivePlans = "400+ Planos Max Total na Rede",
            directCommission = "15% + 5% + 2% + Bônus Diamante",
            indirectLevel2 = "Participação nos Lucros (1% Faturamento Líquido)",
            indirectLevel3 = "Acesso Direto à Diretoria Estratégica",
            specialBonus = "Viagem Anual com Acompanhante para Resort 5 Estrelas",
            meaning = "O ápice do sucesso no ecossistema Max Seg Apiahy.",
            metalColorHex = 0xFF80D8FF
        )
    )

    val plansOffers = listOf(
        PlanOffer(
            id = "ronda",
            name = "Max Ronda Noturna",
            subtitle = "Segurança Patrimonial Ostensiva",
            priceMonthly = 79.90,
            priceFormatted = "R$ 79,90",
            isPopular = false,
            categoryBadge = "MAX SEG",
            features = listOf(
                "Ronda Noturna Monitorada na rua da sua residência",
                "Viatura e moto de pronta resposta",
                "Selo e placa oficial 'Área Protegida Max Seg'",
                "Check-up preventivo de portões e trancas",
                "Suporte 24h via Central Operacional"
            ),
            commissionDirect = 12.00,
            commissionLevel2 = 4.00,
            commissionLevel3 = 1.60,
            strategicValue = "Garante margem saudável para operação de viaturas e combustível em Apiaí."
        ),
        PlanOffer(
            id = "sos",
            name = "Max Saúde SOS 24h",
            subtitle = "Salvaguarda de Vidas & Emergências",
            priceMonthly = 119.90,
            priceFormatted = "R$ 119,90",
            isPopular = false,
            categoryBadge = "MAX SAÚDE",
            features = listOf(
                "Dispositivo Botão de Pânico 4G/GPS (Pingente ou Pulseira)",
                "Central de Emergência Médica 24 Horas",
                "Envio imediato de viatura dedicada de socorro rápido",
                "Agentes treinados em APH (Atendimento Pré-Hospitalar)",
                "Acionamento simultâneo de familiares e SAMU/Resgate"
            ),
            commissionDirect = 18.00,
            commissionLevel2 = 6.00,
            commissionLevel3 = 2.40,
            strategicValue = "Alto valor percebido para idosos e famílias, ticket premium com excelente retenção."
        ),
        PlanOffer(
            id = "clube",
            name = "Clube Max Vantagens",
            subtitle = "Telemedicina & Economia Local",
            priceMonthly = 39.90,
            priceFormatted = "+ R$ 39,90",
            isPopular = false,
            categoryBadge = "UPGRADE",
            features = listOf(
                "Telemedicina 24h ilimitada (Clínica Geral e Pediatria)",
                "Descontos de até 70% em Farmácias credenciadas",
                "Rede de descontos em Mercados, Lojas de Roupas e Eletro",
                "Preço social em Laboratórios de Análises e Clínicas",
                "Cartão de Membro Virtual VIP no Celular"
            ),
            commissionDirect = 6.00,
            commissionLevel2 = 2.00,
            commissionLevel3 = 0.80,
            strategicValue = "Custo atacado de Telemedicina ~R$ 10,00/vida. Margem de quase 300% para o clube."
        ),
        PlanOffer(
            id = "total",
            name = "Max Proteção Total",
            subtitle = "Combo Completo: Ronda + SOS + Clube",
            priceMonthly = 199.90,
            priceFormatted = "R$ 199,90",
            isPopular = true,
            categoryBadge = "COMBO VIP (ECONOMIZE R$ 39,70)",
            features = listOf(
                "TUDO do Max Ronda (Ronda Noturna Dedicada)",
                "TUDO do Max SOS 24h (Botão de Pânico + Viatura APH)",
                "TUDO do Clube Max Vantagens (Telemedicina 24h + Descontos)",
                "Cartão Black VIP com QR Code e Prioridade Total",
                "Economia de R$ 39,70 na assinatura unificada"
            ),
            commissionDirect = 30.00,
            commissionLevel2 = 10.00,
            commissionLevel3 = 4.00,
            strategicValue = "O carro-chefe para os afiliados: R$ 30/mês de recorrência limpa por cliente direto."
        )
    )

    val initialPartners = listOf(
        PartnerBusiness(
            id = "1",
            segment = "Farmácia",
            businessName = "Drogaria Central de Apiaí",
            benefitDiscount = "Até 25% em genéricos e 10% em perfumaria",
            contactPerson = "Dr. Carlos (Farmacêutico)",
            phone = "(15) 3552-1100",
            address = "Rua XV de Novembro, 120 - Centro",
            status = PartnerStatus.ATIVO,
            argumentPitch = "Venda o plano SOS para quem compra remédios de pressão/coração; comissão de R$ 50 paga a segurança da sua farmácia.",
            lat = -24.5088,
            lng = -48.8432,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "2",
            segment = "Loja de Eletrodomésticos",
            businessName = "Eletro Apiaí & Móveis",
            benefitDiscount = "7% à vista em eletros e 10% na linha de móveis",
            contactPerson = "Sr. Roberto Mendes",
            phone = "(15) 3552-2244",
            address = "Av. Leopoldo Leme, 450 - Centro",
            status = PartnerStatus.CONFIRMADO,
            argumentPitch = "Nossa ronda noturna foca na sua vitrine de alto valor (TVs e celulares) em troca do fluxo de membros do clube.",
            lat = -24.5110,
            lng = -48.8415,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "3",
            segment = "Loja de Roupas / Moda",
            businessName = "Boutique Elegância & Estilo",
            benefitDiscount = "15% de desconto às terças no 'Dia Max Seg'",
            contactPerson = "Juliana Sampaio",
            phone = "(15) 99811-3322",
            address = "Praça Alberto Dias, 88",
            status = PartnerStatus.ATIVO,
            argumentPitch = "Criação do 'Dia Max Seg' para atrair fluxo de clientes nos dias de menor movimento da semana.",
            lat = -24.5095,
            lng = -48.8440,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "4",
            segment = "Supermercado",
            businessName = "Supermercado São João Apiahy",
            benefitDiscount = "R$ 10,00 de cashback nas compras acima de R$ 300",
            contactPerson = "Marcos Vinícius",
            phone = "(15) 3552-3300",
            address = "Rua do Comércio, 500",
            status = PartnerStatus.PROSPECCAO,
            argumentPitch = "Fidelização do ticket médio familiar de Apiaí e segurança ostensiva no fechamento do caixa à noite.",
            lat = -24.5072,
            lng = -48.8410,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "5",
            segment = "Posto de Combustíveis",
            businessName = "Posto Portal do Alto Ribeira",
            benefitDiscount = "R$ 0,15 de desconto por litro na gasolina e etanol",
            contactPerson = "Sr. Osvaldo",
            phone = "(15) 3552-4411",
            address = "Rodovia SP-250, km 2",
            status = PartnerStatus.CONFIRMADO,
            argumentPitch = "Direcionamento de toda a frota de viaturas Max Seg e centenas de afiliados para abastecer no posto.",
            lat = -24.5220,
            lng = -48.8350,
            neighborhood = "Alto Ribeira / SP-250"
        ),
        PartnerBusiness(
            id = "6",
            segment = "Pet Shop / Casa de Ração",
            businessName = "AgroPet Apiaí",
            benefitDiscount = "10% em sacos de ração acima de 10kg + Banho e Tosa",
            contactPerson = "Fernanda Veterinária",
            phone = "(15) 99655-7788",
            address = "Rua José Loureiro, 45",
            status = PartnerStatus.PROSPECCAO,
            argumentPitch = "Público tutor de animais que valoriza proteção do lar e compra ração todo mês com recorrência.",
            lat = -24.5125,
            lng = -48.8455,
            neighborhood = "Jardim Paraíso"
        ),
        PartnerBusiness(
            id = "7",
            segment = "Laboratório de Exames",
            businessName = "Laboratório Bioclínico Apiaí",
            benefitDiscount = "Tabela social com até 30% em hemograma e rotina",
            contactPerson = "Dra. Renata Biomédica",
            phone = "(15) 3552-6677",
            address = "Rua Cel. Manoel Gomes, 210",
            status = PartnerStatus.CONFIRMADO,
            argumentPitch = "Convênio direto integrado ao Clube Max Saúde para encaminhamento de exames preventivos.",
            lat = -24.5065,
            lng = -48.8460,
            neighborhood = "Pinheiros"
        ),
        PartnerBusiness(
            id = "8",
            segment = "Autopeças & Oficina",
            businessName = "Auto Peças & Mecânica Central",
            benefitDiscount = "Check-up preventivo grátis + 10% nas peças",
            contactPerson = "Cláudio Mecânico",
            phone = "(15) 99722-1133",
            address = "Av. das Nações, 780",
            status = PartnerStatus.PROSPECCAO,
            argumentPitch = "Segurança noturna para os veículos deixados no pátio da oficina em troca de descontos para a rede.",
            lat = -24.5150,
            lng = -48.8380,
            neighborhood = "Vila Santa Bárbara"
        ),
        PartnerBusiness(
            id = "9",
            segment = "Clínica & Saúde",
            businessName = "Clínica Médica & Odontológica Santa Helena",
            benefitDiscount = "25% em consultas médicas e avaliação odontológica grátis",
            contactPerson = "Dr. Marcelo Paes (Clínico Geral)",
            phone = "(15) 3552-1980",
            address = "Rua Primeiro de Maio, 310 - Centro",
            status = PartnerStatus.ATIVO,
            argumentPitch = "Convênio prioritário integrado ao Clube Max Saúde para consultas presenciais com desconto.",
            lat = -24.5078,
            lng = -48.8445,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "10",
            segment = "Farmácia",
            businessName = "Farmácia & Drogaria São Paulo Apiaí",
            benefitDiscount = "Até 30% em genéricos e tele-entrega grátis no município",
            contactPerson = "Dra. Luciana Farmacêutica",
            phone = "(15) 3552-2550",
            address = "Rua XV de Novembro, 280 - Centro",
            status = PartnerStatus.ATIVO,
            argumentPitch = "Desconto express em medicamentos para idosos e famílias do Clube Max Seg.",
            lat = -24.5090,
            lng = -48.8420,
            neighborhood = "Centro"
        ),
        PartnerBusiness(
            id = "11",
            segment = "Clínica & Saúde",
            businessName = "Clínica de Fisioterapia & Reabilitação Apiahy",
            benefitDiscount = "20% em fisioterapia motora, respiratória e pilates",
            contactPerson = "Dra. Camila Fisioterapeuta",
            phone = "(15) 99788-4455",
            address = "Av. Leopoldo Leme, 620 - Centro",
            status = PartnerStatus.CONFIRMADO,
            argumentPitch = "Atendimento a pacientes do plano SOS e idosos com mobilidade reduzida em Apiaí.",
            lat = -24.5115,
            lng = -48.8405,
            neighborhood = "Centro"
        )
    )

    val commercialPitches = listOf(
        CommercialPitch(
            id = "farmacia",
            title = "Abordagem para Farmácias de Apiaí",
            targetAudience = "Proprietários e Farmacêuticos",
            keyHooks = listOf(
                "Segurança para o estabelecimento com a Ronda Noturna dedicada",
                "Comissão de R$ 50,00 por plano SOS vendido no balcão",
                "Direcionamento prioritário de todos os membros para a farmácia",
                "Transformação da farmácia em um 'Centro de Proteção à Vida'"
            ),
            pitchBody = "Sr. Proprietário, imagine que uma cliente idosa sua compra remédios para pressão aqui todo mês. Se ela passar mal sozinha em casa, não terá tempo de discar. Com o nosso pingente Max SOS, ela aperta um botão e nossa viatura, que já está na rua de Apiaí, chega em minutos com socorrista treinado. A sua farmácia não está apenas vendendo o remédio, está vendendo a garantia da vida. E cada venda gera R$ 50,00 de comissão direta, quitando a sua própria segurança patrimonial.",
            whatsappMessage = """
*Proposta de Parceria Estratégica – Max Seg & Clube Max Vantagens Apiahy*

Prezado(a) proprietário(a),

A Max Seg Apiahy está implementando o projeto *Cinturão de Proteção e Saúde* em Apiaí. Gostaríamos de convidar sua farmácia para ser nossa *Parceira Âncora Oficial*.

*Vantagens exclusivas:*
1. Sua farmácia protegida por nossa Ronda Noturna dedicada.
2. Comissão direta de até *R$ 50,00 por adesão* no balcão.
3. Direcionamento dos nossos associados para comprarem medicamentos no seu estabelecimento.
4. Selo oficial de 'Farmácia Protetora de Apiaí'.

Podemos agendar uma breve demonstração de 10 minutos do dispositivo SOS amanhã?
            """.trimIndent()
        ),
        CommercialPitch(
            id = "eletro",
            title = "Abordagem para Lojas de Eletrodomésticos",
            targetAudience = "Gerentes e Donos de Lojas de Eletro/Móveis",
            keyHooks = listOf(
                "Redução de risco noturno para vitrines com produtos de alto valor (TVs, smartphones)",
                "Geração de novos compradores com renda economizada no clube",
                "Divulgação no app e carteira digital de todos os membros"
            ),
            pitchBody = "Ao ser parceiro Max Seg, nossa ronda noturna terá um olhar redobrado sobre sua vitrine e estoque de alta densidade de valor durante a madrugada. Em troca, você concede um benefício aos nossos membros cadastrados, atraindo a população de Apiaí que economiza na mensalidade e vem comprar móveis e eletrônicos na sua loja.",
            whatsappMessage = """
*Parceria Comercial – Max Seg Proteção & Lojas de Eletro Apiahy*

Olá! A Max Seg Apiahy convida sua loja a se credenciar no Clube Max Vantagens.

*O que sua empresa ganha:*
• Monitoramento preventivo e ronda na porta do seu estabelecimento.
• Canal direto com centenas de famílias de Apiaí portadoras do Cartão VIP.
• Aumento das vendas à vista com condições exclusivas.

Vamos agendar uma conversa para apresentar a tabela de benefícios?
            """.trimIndent()
        ),
        CommercialPitch(
            id = "roupas",
            title = "Abordagem para Lojas de Roupas e Confecções",
            targetAudience = "Lojistas do Comércio Local",
            keyHooks = listOf(
                "Criação do 'Dia Max Seg' para dias de menor movimento (ex: terça-feira)",
                "Divulgação visual com adesivo 'Estabelecimento Parceiro'",
                "Engajamento com líderes de opinião (portadores dos bottons de graduação)"
            ),
            pitchBody = "Podemos criar o 'Dia Max Seg' na sua loja, onde em dias de menor movimento, todo cliente que apresentar o Cartão Virtual Max ganha um desconto exclusivo. Além disso, quando nossos Afiliados com Bottons de Ouro e Prata entrarem na sua loja, eles agirão como verdadeiros promotores da sua marca em seus grupos e redes de contatos em Apiaí.",
            whatsappMessage = """
*Oportunidade de Fluxo de Clientes – Clube Max Apiahy*

Olá! Gostaria de apresentar como atrair mais clientes para sua boutique/loja de roupas com o Clube Max Vantagens Apiahy.

Criamos campanhas temáticas e direcionamos nossos membros com cartão ativo para comprar moda no seu comércio.

Podemos apresentar o material oficial hoje?
            """.trimIndent()
        ),
        CommercialPitch(
            id = "evento",
            title = "Script Completo para o Evento de Lançamento (Fundadores)",
            targetAudience = "Comerciantes, Líderes de Bairro e 20 Parceiros Fundadores",
            keyHooks = listOf(
                "Abertura com impacto social e cuidado com as famílias de Apiaí",
                "Demonstração ao vivo do clique no Botão SOS e viatura",
                "Apresentação do Marketing de Rede Sustentável (Social Selling)",
                "Chamada para os 20 primeiros contratos com selo de Fundador"
            ),
            pitchBody = """
1. ABERTURA (5 min): 'Boa noite a todos. Todos nós conhecemos a realidade de Apiaí. Sabemos o receio de fechar o comércio à noite ou a preocupação de ter um pai ou mãe idoso morando sozinho em uma rua afastada. A iniciativa privada precisa dar o próximo passo.'

2. O PRODUTO (10 min): 'A Max Seg e a Max Saúde se uniram em três pilares: Max Ronda por R$ 79,90, Max SOS por R$ 119,90 com viatura treinada em primeiros socorros, e Clube Max Vantagens com telemedicina 24h por +R$ 39,90.'

3. A OPORTUNIDADE (15 min): 'Adotamos o modelo de Marketing de Rede Sustentável como a Amway e Natura, mas com serviços essenciais. Indicou 1 Max Total (R$ 199,90) = Ganha R$ 30,00 TODO MÊS na sua conta. Com 10 clientes, sua renda passiva é de R$ 300/mês e sua segurança sai de graça!'

4. FECHAMENTO (5 min): 'Hoje estamos selecionando os 20 Parceiros Fundadores de Apiaí com exclusividade de território e o Botton de Liderança. Quem está pronto para proteger nossa cidade e lucrar com isso?'
            """.trimIndent(),
            whatsappMessage = """
*Convite Oficial – Lançamento do Clube Max Seg Apiahy*

Você é nosso convidado de honra para o evento exclusivo de lançamento do *Cinturão de Proteção & Clube de Afiliados Max Seg Apiahy*.

Venha conhecer como proteger seu patrimônio, salvar vidas com a tecnologia SOS e gerar uma renda recorrente sólida em nossa cidade.

Data: Próxima Quinta-feira às 19h30
Local: Centro Empresarial de Apiaí
Confirme sua presença respondendo esta mensagem!
            """.trimIndent()
        )
    )
}
