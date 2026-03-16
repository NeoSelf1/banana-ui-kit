package com.neon.core.ui.infrastructure

import java.util.concurrent.atomic.AtomicInteger

/**
 * Recomposition 횟수를 추적하는 싱글톤 카운터
 *
 * AtomicInteger를 사용하여 메인 스레드(Compose SideEffect)와
 * instrumentation 스레드(테스트 코드) 간 thread-safe 접근을 보장합니다.
 *
 * instrumented test는 앱과 동일 프로세스에서 실행되므로
 * 테스트 코드에서 직접 get()/reset() 호출이 가능합니다.
 */
object RecompositionTracker {
    private val counter = AtomicInteger(0)

    fun increment() {
        counter.incrementAndGet()
    }

    fun get(): Int = counter.get()

    fun reset() {
        counter.set(0)
    }
}
