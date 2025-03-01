package no.nav.helse.flex.domene

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

data class RSVedtakWrapper(
    val id: String,
    val lest: Boolean,
    val lestDato: OffsetDateTime? = null,
    val vedtak: RSVedtak,
    val opprettetTimestamp: Instant,
    val orgnavn: String,
    val annullert: Boolean = false,
    val revurdert: Boolean = false,
    val dagerArbeidsgiver: List<RSDag> = emptyList(),
    val dagerPerson: List<RSDag> = emptyList(),
    val sykepengebelopArbeidsgiver: Int = 0,
    val sykepengebelopPerson: Int = 0,
    val andreArbeidsgivere: Map<String, Double>?
)

data class RSVedtak(
    val organisasjonsnummer: String,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val dokumenter: List<Dokument>,
    val inntekt: Double?,
    val sykepengegrunnlag: Double?,
    val utbetaling: RSUtbetalingUtbetalt,
    val grunnlagForSykepengegrunnlag: Double?,
    val grunnlagForSykepengegrunnlagPerArbeidsgiver: Map<String, Double>?,
    val begrensning: String?, // ER_6G_BEGRENSET, ER_IKKE_6G_BEGRENSET, VURDERT_I_INFOTRYGD og VET_IKKE
    val vedtakFattetTidspunkt: LocalDate?,
    val sykepengegrunnlagsfakta: JsonNode?,
    val begrunnelser: List<Begrunnelse>?
) : Periode

data class RSUtbetalingUtbetalt(
    val organisasjonsnummer: String?,
    val utbetalingId: String?,
    val forbrukteSykedager: Int,
    val gjenståendeSykedager: Int,
    val automatiskBehandling: Boolean,
    @JsonIgnore
    val arbeidsgiverOppdrag: RSOppdrag?,
    @JsonIgnore
    val personOppdrag: RSOppdrag?,
    @JsonIgnore
    val utbetalingsdager: List<RSUtbetalingdag>?,
    val foreløpigBeregnetSluttPåSykepenger: LocalDate?,
    val utbetalingType: String
)

data class RSOppdrag(
    val mottaker: String,
    val nettoBeløp: Int,
    val utbetalingslinjer: List<RSUtbetalingslinje>
)

data class RSUtbetalingslinje(
    val fom: LocalDate,
    val tom: LocalDate,
    val dagsats: Int,
    val dagsatsTransformasjonHjelper: Int,
    val totalbeløp: Int,
    val grad: Double,
    val stønadsdager: Int
) {
    fun overlapperMed(dato: LocalDate) = dato in fom..tom
}

data class RSUtbetalingdag(
    val dato: LocalDate,
    val type: String,
    val begrunnelser: List<String>
)

data class RSDag(
    val dato: LocalDate,
    val belop: Int,
    val grad: Double,
    val dagtype: String,
    val begrunnelser: List<String>
)
