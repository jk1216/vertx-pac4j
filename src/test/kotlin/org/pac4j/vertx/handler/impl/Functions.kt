package org.pac4j.vertx.handler.impl

import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import io.vertx.rxjava.core.Vertx
import io.vertx.rxjava.core.http.HttpClientRequest
import io.vertx.rxjava.core.http.HttpClientResponse
import io.vertx.rxjava.core.http.HttpServer
import io.vertx.rxjava.ext.auth.AuthProvider
import io.vertx.rxjava.ext.web.Router
import io.vertx.rxjava.ext.web.RoutingContext
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore
import org.hamcrest.MatcherAssert.assertThat
import org.pac4j.core.config.Config
import org.pac4j.vertx.VertxProfileManager
import org.pac4j.vertx.VertxWebContext
import org.pac4j.vertx.auth.Pac4jAuthProvider
import rx.Observable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Supplier
import org.hamcrest.core.Is.`is` as isEqualTo

val LOG: Logger = LoggerFactory.getLogger("org.pac4j.vertx.handler.impl")

/**
 * Common functions used by Kotlin integration tests.
 * @author Jeremy Prime
 * @since 2.0.0
 */
fun getProfileHandler(rc: RoutingContext) {

    LOG.info("Get profile endpoint called")
    LOG.info("Session id = " + rc.session().id())
    val sessionId = rc.session().id()
    val profileManager = getProfileManager(rc)

    val (userId, email) = profileManager.get(true)
            .map { p -> Pair(p.id, p.getAttribute(EMAIL_KEY)) }
            .orElseGet { Pair(null, null) }

    val json = JsonObject()
            .put(USER_ID_KEY, userId)
            .put(SESSION_ID_KEY, sessionId)
            .put(EMAIL_KEY, email)

    LOG.info("Json is\n" + json.encodePrettily())
    LOG.info("Get profile endpoint completing")

    rc.response().end(json.encodePrettily())
}

fun logoutHandler(vertx: io.vertx.core.Vertx): Handler<RoutingContext> {
    val applicationLogoutHandler = ApplicationLogoutHandler(vertx, ApplicationLogoutHandlerOptions(), Config())
    return Handler {
        val delegate = it.delegate
        if (delegate is io.vertx.ext.web.RoutingContext) {
            applicationLogoutHandler.handle(delegate)
        }
    }
}


fun getProfileManager(rc: RoutingContext): VertxProfileManager {
    val webContext = VertxWebContext(rc.delegate as io.vertx.ext.web.RoutingContext)
    return VertxProfileManager(webContext)
}

fun toResponseObservable(request: HttpClientRequest, requestConfigurer: Consumer<HttpClientRequest>):
        Observable<HttpClientResponse> =
        Observable.create<HttpClientResponse> { subscriber ->

            if (subscriber.isUnsubscribed) {
                Observable.empty<HttpClientResponse>()
            } else {
                val requestObservable = request.toObservable()
                requestConfigurer.accept(request)
                requestObservable.subscribe(subscriber)
                request.end()
            }

        }

fun addHeader(headerName: String,
              headerValueSupplier: Supplier<String?>): Consumer<HttpClientRequest> {

    return Consumer { req -> headerValueSupplier.get()?.let { req.headers().add(headerName, it )} }

}

fun startServer(rxVertx: Vertx, routeConfigurer: Consumer<Router>) {
    val rxRouter = Router.router(rxVertx)

    routeConfigurer.accept(rxRouter)

    // Can we switch this for returning observable?
    val serverFuture = CompletableFuture<HttpServer>()
    rxVertx.createHttpServer()
            .requestHandler({ rxRouter.accept(it) })
            .listenObservable(8080)
            .subscribe({ serverFuture.complete(it)
                LOG.info("Server started")},
                    { LOG.info("Server failed to start") })
    serverFuture.get(1, TimeUnit.SECONDS)
}

fun startServerWithSessionSupport(rxVertx: Vertx, routeConfigurer: Consumer<Router>) {

    val fullRouteConfigurer = Consumer<Router> { r ->

        val sessionStore = LocalSessionStore.create(rxVertx)
        val authProvider = AuthProvider.newInstance(Pac4jAuthProvider())
        // All routes first, for ease of setup
        with(r) {
            route().handler(io.vertx.rxjava.ext.web.handler.CookieHandler.create())
            route().handler(io.vertx.rxjava.ext.web.handler.SessionHandler.create(sessionStore))
            route().handler(io.vertx.rxjava.ext.web.handler.UserSessionHandler.create(authProvider))
        }

    }.andThen(routeConfigurer)

    startServer(rxVertx, fullRouteConfigurer)
}

fun extractCookie(resp: HttpClientResponse, cookiePersister: Consumer<String>): HttpClientResponse {
    val setCookie = resp.headers().get("set-cookie")
    cookiePersister.accept(setCookie)
    return resp
}

fun assertThatResponseCodeIs(resp:HttpClientResponse, expectedStatus: Int): HttpClientResponse {
    assertThat(resp.statusCode(), isEqualTo(expectedStatus))
    return resp
}


