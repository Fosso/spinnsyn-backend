package no.nav.helse.flex.client.syfotilgangskontroll

import no.nav.helse.flex.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
class SyfoTilgangskontrollOboClient(
    @Value("\${syfotilgangskontroll.url}") private val url: String,
    private val syfotilgangskontrollRestTemplate: RestTemplate
) {
    companion object {
        const val ACCESS_TO_USER_WITH_AZURE_V2_PATH = "/syfo-tilgangskontroll/api/tilgang/navident/person"
        const val NAV_PERSONIDENT_HEADER = "nav-personident"
    }

    val log = logger()

    @Retryable
    fun sjekkTilgangVeileder(fnr: String): Boolean {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        headers[NAV_PERSONIDENT_HEADER] = fnr

        return try {
            val response = syfotilgangskontrollRestTemplate.exchange(
                accessToUserV2Url(),
                GET,
                HttpEntity<Any>(headers),
                String::class.java
            )
            response.statusCode.is2xxSuccessful
        } catch (e: HttpClientErrorException) {
            if (e.statusCode != HttpStatusCode.valueOf(403)) {
                log.error("Kall til SyfoTilgangskontroll feilet med HttpStatusCode ${e.statusCode.value()}", e)
            }
            false
        }
    }

    fun accessToUserV2Url(): String {
        return "$url$ACCESS_TO_USER_WITH_AZURE_V2_PATH"
    }
}
