package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** M04 - Concurrent borrows. See MidTasks.md. */
class M04_ConcurrentBorrowTest extends ApiTest {

  @Test
  void onlyOneOfManyParallelBorrowsOfTheSameDeviceSucceeds() throws Exception {
    long laptop = insertDevice("Dell XPS 13");
    List<Callable<Response>> requests = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      long employee = insertEmployee("Employee " + i);
      requests.add(() -> borrow(laptop, employee));
    }

    List<Response> responses = runAtTheSameTime(requests);

    assertThat(responses).filteredOn(r -> r.status() == 201).hasSize(1);
    assertThat(responses)
        .filteredOn(r -> r.status() != 201)
        .hasSize(9)
        .allSatisfy(r -> r.expectProblem(409, "DEVICE_ALREADY_LOANED"));
    assertThat(activeLoansOfDevice(laptop)).isEqualTo(1);
  }

  @Test
  void parallelBorrowsByOneEmployeeNeverExceedTheLimit() throws Exception {
    long asta = insertEmployee("Asta Demo");
    List<Callable<Response>> requests = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      long device = insertDevice("Laptop " + i);
      requests.add(() -> borrow(device, asta));
    }

    List<Response> responses = runAtTheSameTime(requests);

    assertThat(responses).filteredOn(r -> r.status() == 201).hasSize(3);
    assertThat(responses)
        .filteredOn(r -> r.status() != 201)
        .hasSize(3)
        .allSatisfy(r -> r.expectProblem(409, "LOAN_LIMIT_REACHED"));
    assertThat(activeLoansOfEmployee(asta)).isEqualTo(3);
  }

  private static List<Response> runAtTheSameTime(List<Callable<Response>> requests)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(requests.size());
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Response>> futures = new ArrayList<>();
      for (Callable<Response> request : requests) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  return request.call();
                }));
      }
      start.countDown();
      List<Response> responses = new ArrayList<>();
      for (Future<Response> future : futures) {
        responses.add(future.get(30, TimeUnit.SECONDS));
      }
      return responses;
    } finally {
      pool.shutdownNow();
    }
  }
}
