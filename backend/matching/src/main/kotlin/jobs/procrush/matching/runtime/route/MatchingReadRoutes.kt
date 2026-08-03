package jobs.procrush.matching.runtime.route

import io.ktor.server.routing.Route

/** HTTP recommendation read model removed — API consumes Kafka scores and joins locally. */
fun Route.matchingReadRoutes() {
    // Intentionally empty: matching exposes /health only.
}
