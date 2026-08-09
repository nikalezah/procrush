package jobs.procrush.personality.bootstrap

import jobs.procrush.bootstrap.config.WorkerAppConfig
import jobs.procrush.personality.amqp.PersonalityAmqpModule
import jobs.procrush.personality.messaging.PersonalityCommandConsumer
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun personalityKoinModules(config: WorkerAppConfig): List<Module> =
    listOf(
        module {
            single { config }
            single { PersonalityAmqpModule.create(get<WorkerAppConfig>().rabbitMq) }
            single { PersonalityWorkerModule.create(get(), get()) }
            single<PersonalityCommandConsumer> { get<PersonalityWorkerModule>().consumer }
            singleOf(::PersonalityWorkerRuntime)
        },
    )

internal class PersonalityWorkerRuntime(
    private val rabbitMqModule: PersonalityAmqpModule,
    private val workerModule: PersonalityWorkerModule,
) {
    fun start() {
        workerModule.start()
    }

    fun close() {
        workerModule.stop()
        rabbitMqModule.close()
    }
}