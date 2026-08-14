package com.young.studyproject.example.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.young.studyproject.example.async.dto.AsyncExecutionResponse;
import com.young.studyproject.example.async.dto.ChainResponse;
import com.young.studyproject.example.async.dto.FailureHandlingResponse;
import com.young.studyproject.example.async.dto.TaskResult;
import com.young.studyproject.example.async.dto.TimeoutResponse;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AsyncExampleServiceTest {

    private static final long DELAY_MS = 200;

    @Autowired
    private AsyncExampleService asyncExampleService;

    @Test
    @DisplayName("순차 실행은 요청 스레드에서 작업 시간을 모두 더한 만큼 걸린다")
    void runSequentially() {
        AsyncExecutionResponse response = asyncExampleService.runSequentially(3, DELAY_MS);

        assertThat(response.tasks()).hasSize(3);
        assertThat(response.totalElapsedMs()).isGreaterThanOrEqualTo(DELAY_MS * 3);
        assertThat(response.tasks())
                .extracting(TaskResult::threadName)
                .containsOnly(response.callerThread());
    }

    @Test
    @DisplayName("병렬 실행은 다른 스레드에서 돌고 가장 느린 작업 하나 정도만 걸린다")
    void runInParallel() {
        AsyncExecutionResponse response = asyncExampleService.runInParallel(3, DELAY_MS);

        assertThat(response.tasks()).hasSize(3);
        assertThat(response.totalElapsedMs()).isLessThan(DELAY_MS * 3);
        assertThat(response.tasks())
                .extracting(TaskResult::threadName)
                .allSatisfy(threadName -> assertThat(threadName).startsWith("example-async-"));
    }

    @Test
    @DisplayName("가상 스레드는 풀 크기보다 많은 작업도 동시에 실행한다")
    void runOnVirtualThreads() {
        AsyncExecutionResponse response = asyncExampleService.runOnVirtualThreads(50, DELAY_MS);

        assertThat(response.tasks()).hasSize(50);
        assertThat(response.totalElapsedMs()).isLessThan(DELAY_MS * 5);
        assertThat(response.tasks()).allSatisfy(task -> assertThat(task.virtualThread()).isTrue());
    }

    @Test
    @DisplayName("가상 스레드 작업 중 하나가 실패해도 exceptionally가 감싸 실패 결과로 남고 다른 결과는 영향받지 않는다")
    void runOnVirtualThreadsHandlesIndividualFailure() {
        TaskResult failed = asyncExampleService.failedTaskResult(
                "task-1", System.nanoTime(), new CompletionException(new IllegalStateException("작업이 중단되었습니다.")));

        assertThat(failed.taskName()).isEqualTo("task-1");
        assertThat(failed.value()).isEqualTo("실패: 작업이 중단되었습니다.");
    }

    @Test
    @DisplayName("@Async는 다른 빈을 통한 호출에만 적용되고 self-invocation에는 적용되지 않는다")
    void runSpringAsync() {
        AsyncExecutionResponse response = asyncExampleService.runSpringAsync(DELAY_MS);

        TaskResult proxyCall = response.tasks().getFirst();
        TaskResult selfInvocation = response.tasks().getLast();

        assertThat(proxyCall.threadName()).startsWith("example-async-");
        assertThat(selfInvocation.threadName()).isEqualTo(response.callerThread());
    }

    @Test
    @DisplayName("thenCompose는 중첩된 CompletableFuture를 평탄화한다")
    void runChain() {
        ChainResponse response = asyncExampleService.runChain("young", DELAY_MS);

        assertThat(response.thenApplyResult()).isEqualTo("FETCHUSERNAME 완료:YOUNG");
        assertThat(response.thenComposeResult()).isEqualTo("안녕하세요, fetchUserName 완료:young님");
        assertThat(response.nestedTypeWithThenApply()).contains("CompletableFuture<CompletableFuture<String>>");
        assertThat(response.thenCombineResult()).endsWith("님의 최신 글: 비동기 처리 정리");
    }

    @Test
    @DisplayName("비동기 작업의 예외는 join에서 CompletionException으로 감싸져 다시 던져진다")
    void runWithFailure() {
        FailureHandlingResponse response = asyncExampleService.runWithFailure(true, 0);

        assertThat(response.exceptionallyResult()).startsWith("exceptionally 대체값");
        assertThat(response.handleResult()).startsWith("handle 실패 처리");
        assertThat(response.joinThrownType()).isEqualTo("CompletionException");
        assertThat(response.joinCauseType()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("성공하면 exceptionally는 건너뛰고 handle만 결과를 받는다")
    void runWithoutFailure() {
        FailureHandlingResponse response = asyncExampleService.runWithFailure(false, 0);

        assertThat(response.exceptionallyResult()).isEqualTo("정상 결과");
        assertThat(response.handleResult()).isEqualTo("handle 정상 처리: 정상 결과");
        assertThat(response.joinThrownType()).isEqualTo("던져지지 않음");
    }

    @Test
    @DisplayName("orTimeout은 예외로 실패하고 completeOnTimeout은 대체값으로 성공한다")
    void runWithTimeout() {
        TimeoutResponse response = asyncExampleService.runWithTimeout(1_000, 200);

        assertThat(response.orTimeoutResult()).startsWith("TimeoutException 발생");
        assertThat(response.completeOnTimeoutResult()).startsWith("기본값");
    }

    @Test
    @DisplayName("허용 범위를 벗어난 파라미터는 IllegalArgumentException으로 거부한다")
    void rejectInvalidParameters() {
        assertThatThrownBy(() -> asyncExampleService.runInParallel(0, DELAY_MS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asyncExampleService.runInParallel(3, 10_000))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asyncExampleService.runWithTimeout(100, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
