package com.monpro.api.design.pattern.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.*;

@RestController
@RequestMapping("/async")

public class CompletableFutureDemo {
  private static final Logger logger = LoggerFactory.getLogger(CompletableFutureDemo.class);
  private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
  @PostMapping("/CompletableFutureSupplyAsync")
  public String saveSalesData(@RequestParam("param") String param,
                                @RequestParam("sleepTime") Integer sleepTime) throws ExecutionException, InterruptedException {

    // other business logic
    var future = CompletableFuture.supplyAsync(() -> save(param, sleepTime))
        .exceptionally(throwable -> {
          logger.error("error: {}", Arrays.toString(throwable.getStackTrace()));
          return "error";
        });

    logger.info("running while I/O waiting");

    return future.get();
  }

  @PostMapping("/CompletableFutureSupplyAsyncAcceptException")
  public String saveSalesDataThenAggregate(@RequestParam("param") String param,
                              @RequestParam("sleepTime") Integer sleepTime) throws ExecutionException, InterruptedException {

    // other business logic
    var saveSalesDataFuture = CompletableFuture.supplyAsync(() -> save(param, sleepTime));
    saveSalesDataFuture.thenApply((result) -> {
      logger.info("result: {}", result);
      return String.format("200: %s", result);
    });

    saveSalesDataFuture.exceptionally((throwable -> {
      logger.error("error: {}", Arrays.toString(throwable.getStackTrace()));
      return null;
    }));
    logger.info("running while I/O waiting then apply and exceptionally");

    return saveSalesDataFuture.get();
  }

  public String save(String param, Integer sleepTime){
    logger.info(Thread.currentThread().getName());
    if ("error".equals(param)) {
      throw new RuntimeException("Error when saving sales records into databases");
    }
//    final Runnable runnable = () -> {
//      logger.info("sleeping");
//    };
//    executor.schedule(runnable, sleepTime, TimeUnit.SECONDS);

    try {
      Thread.sleep(sleepTime * 1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "okay";
  }

  @PostMapping("/QueryCodeThenPrice")
  public String queryCodeThenPrice(@RequestParam("companyName") String companyName,
                                   @RequestParam("sleepTime") Integer sleepTime) throws ExecutionException, InterruptedException {
    final CompletableFuture<String> codeFuture = CompletableFuture.supplyAsync(() -> queryCode(companyName, sleepTime));

    final CompletableFuture<String> priceFuture = codeFuture.thenApplyAsync((code) -> queryPrice(code, sleepTime));

    priceFuture.thenAccept((result) -> logger.info("result: {}", result));

    logger.info("running other logic while waiting I/O");
    return priceFuture.get();
  }


  static String queryCode(final String companyName, final Integer sleepTime) {
    try {
      logger.info("querying code");
      Thread.sleep(sleepTime * 1000);
    } catch (InterruptedException ignored) {
    }
    logger.info("querying code finished");

    return companyName + "123456";
  }

  static String queryPrice(final String code, final Integer sleepTime) {
    try {
      logger.info("querying price");
      Thread.sleep(sleepTime * 1000);
    } catch (InterruptedException ignored) {
    }
    logger.info("querying price finished");
    if (code.startsWith("test")) {
      return "9999";
    } else {
      return "1111";
    }
  }

}
