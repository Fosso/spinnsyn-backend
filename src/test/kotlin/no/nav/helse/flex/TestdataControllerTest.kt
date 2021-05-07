package no.nav.helse.flex

import no.nav.helse.flex.vedtak.db.VedtakDAO
import no.nav.helse.flex.vedtak.domene.AnnulleringDto
import no.nav.helse.flex.vedtak.domene.UtbetalingUtbetalt
import no.nav.helse.flex.vedtak.domene.VedtakDto
import no.nav.helse.flex.vedtak.domene.VedtakFattetForEksternDto
import org.amshove.kluent.`should be`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import java.time.LocalDateTime

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestdataControllerTest : AbstractContainerBaseTest() {

    @Autowired
    lateinit var vedtakDAO: VedtakDAO

    @Autowired
    lateinit var restTemplate: RestTemplate

    @Value("\${on-prem-kafka.username}")
    lateinit var systembruker: String

    val fnr = "234232323"
    val fom = LocalDate.now().minusDays(7)
    val tom = LocalDate.now()
    val orgnummer = "123"
    val utbetalingId = "239fj20j3f"
    val automatiskBehandletVedtak = VedtakDto(
        fom = fom,
        tom = tom,
        forbrukteSykedager = 1,
        gjenståendeSykedager = 2,
        organisasjonsnummer = orgnummer,
        utbetalinger = listOf(
            VedtakDto.UtbetalingDto(
                mottaker = orgnummer,
                fagområde = "idk",
                totalbeløp = 1400,
                utbetalingslinjer = listOf(
                    VedtakDto.UtbetalingDto.UtbetalingslinjeDto(
                        fom = fom,
                        tom = tom,
                        dagsats = 14,
                        beløp = 1400,
                        grad = 100.0,
                        sykedager = 100
                    )
                )
            )
        ),
        dokumenter = emptyList(),
        automatiskBehandling = true
    )
    val annulleringDto = AnnulleringDto(
        fødselsnummer = fnr,
        orgnummer = orgnummer,
        tidsstempel = LocalDateTime.now(),
        fom = fom,
        tom = tom
    )

    val vedtak = VedtakFattetForEksternDto(
        fødselsnummer = fnr,
        aktørId = fnr,
        organisasjonsnummer = orgnummer,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = fom,
        dokumenter = emptyList(),
        inntekt = 0.0,
        sykepengegrunnlag = 0.0,
        utbetalingId = utbetalingId
    )
    val utbetaling = UtbetalingUtbetalt(
        fødselsnummer = fnr,
        aktørId = fnr,
        organisasjonsnummer = orgnummer,
        fom = fom,
        tom = tom,
        utbetalingId = utbetalingId,
        event = "utbetaling_utbetalt",
        forbrukteSykedager = 42,
        gjenståendeSykedager = 3254,
        automatiskBehandling = true,
        arbeidsgiverOppdrag = UtbetalingUtbetalt.OppdragDto(
            mottaker = orgnummer,
            fagområde = "SPREF",
            fagsystemId = "1234",
            nettoBeløp = 123,
            utbetalingslinjer = emptyList()
        ),
        type = "UTBETALING",
        utbetalingsdager = emptyList()
    )

    @Test
    @Order(1)
    fun `Oppretter vedtak`() {
        mockMvc.perform(
            post("/api/v1/mock/vedtak/$fnr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(automatiskBehandletVedtak.serialisertTilString())
        ).andExpect(status().is2xxSuccessful).andReturn()

        val id = vedtakDAO.finnVedtak(fnr).first().id

        val oppgaver = oppgaveKafkaConsumer.ventPåRecords(antall = 1)
        doneKafkaConsumer.ventPåRecords(antall = 0)

        oppgaver.shouldHaveSize(1)

        val nokkel = oppgaver[0].key()
        nokkel.getSystembruker() shouldBeEqualTo systembruker

        val oppgave = oppgaver[0].value()

        oppgave.getFodselsnummer() shouldBeEqualTo fnr
        oppgave.getSikkerhetsnivaa() shouldBeEqualTo 4
        oppgave.getTekst() shouldBeEqualTo "Sykepengene dine er beregnet - se resultatet"
        oppgave.getLink() shouldBeEqualTo "blah/vedtak/$id"
        oppgave.getGrupperingsId() shouldBeEqualTo id
        oppgave.getEksternVarsling() shouldBeEqualTo true
    }

    @Test
    @Order(2)
    fun `vi henter vedtaket`() {
        val vedtak = hentV1Vedtak(fnr)

        vedtak shouldHaveSize 1
        vedtak.first().lest `should be` false
        vedtak.first().annullert `should be` false
    }

    @Test
    @Order(3)
    fun `Oppretter annullering`() {
        mockMvc.perform(
            post("/api/v1/mock/annullering/$fnr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(annulleringDto.serialisertTilString())
        ).andExpect(status().is2xxSuccessful).andReturn()
    }

    @Test
    @Order(4)
    fun `Vi henter vedtaket som nå er annullert`() {
        val vedtak = hentV1Vedtak(fnr)

        vedtak shouldHaveSize 1
        vedtak.first().annullert `should be` true
    }

    @Test
    @Order(5)
    fun `Oppretter nytt vedtak og utbetaling`() {
        data class VedtakV2(val vedtak: String, val utbetaling: String?)
        val body = VedtakV2(vedtak.serialisertTilString(), utbetaling.serialisertTilString())

        mockMvc.perform(
            post("/api/v2/testdata/vedtak/$fnr")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.serialisertTilString())
        ).andExpect(status().is2xxSuccessful).andReturn()
    }

    @Test
    @Order(6)
    fun `Sletter vedtaket`() {
        mockMvc.perform(
            delete("/api/v1/mock/vedtak/$fnr")
        ).andExpect(status().is2xxSuccessful).andReturn()

        doneKafkaConsumer.ventPåRecords(antall = 1)
    }

    @Test
    @Order(7)
    fun `Vi har nå ingen vedtak`() {
        hentV1Vedtak(fnr) shouldHaveSize 0
        hentVedtak(fnr) shouldHaveSize 0
    }
}
