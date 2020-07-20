package no.nav.syfo.testutil

import io.mockk.every
import java.time.Duration
import no.nav.syfo.application.ApplicationState
import org.apache.kafka.clients.consumer.KafkaConsumer

fun stopApplicationNårTopicErLest(
    kafkaConsumer: KafkaConsumer<String, String>,
    applicationState: ApplicationState
) {
    every { kafkaConsumer.poll(any<Duration>()) } answers {
        val cr = callOriginal()
        if (!cr.isEmpty) {
            applicationState.ready = false
        }
        cr
    }
}
