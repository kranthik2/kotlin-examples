package com.mrkulli.demo

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.circuitbreaker.operator.CircuitBreakerOperator
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import org.springframework.boot.autoconfigure.SpringBootApplication
import java.time.Duration

@SpringBootApplication
class DemoApplication


fun main(args: Array<String>) {
    val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(75.0F)
            .waitDurationInOpenState(Duration.ofMinutes(10)).build();
    val circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig)
    val circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("test-circuit")
    //Force opening circuit for testing
    circuitBreaker.transitionToForcedOpenState()
    observable().compose(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResumeNext { throwable: Throwable -> fallbackObservable(throwable) }
            .subscribeBy(onNext = { next -> println(next) },
                    onError = { error -> println("Failed to process observable : ".plus(error)) })
}

private fun observable(): Observable<String> {
    return Observable.fromCallable { "calling primary observable" }.doOnComplete { println("primaryObservable completed") }
}

private fun fallbackObservable(throwable: Throwable): Observable<String> {
    println("Error occurred with primaryObservable : ".plus(throwable))
    return Observable.fromCallable { "fallbackObservable called" }.doOnComplete { println("fallbackObservable completed") }
}
