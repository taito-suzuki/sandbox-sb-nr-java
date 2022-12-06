package com.example.sandboxsbnrjava;

import com.newrelic.api.agent.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

@SpringBootApplication
public class SandboxSbNrJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SandboxSbNrJavaApplication.class, args);
    }
}

@Controller
class HelloController {
    private Logger logger;
    private ExecutorService thraedPoolForP009 = Executors.newFixedThreadPool(2);

    HelloController() {
        logger = logger.getLogger(getClass().getName());
    }

    @GetMapping("/p001")
    @Trace(dispatcher = true)
        // ここからトランザクションを開始する
    String getP001() {
        Usecase.f001();
        return "index";
    }

    @GetMapping("/p002")
    @Trace(dispatcher = true)
    String getP002() {
        Usecase.f002();
        return "index";
    }

    @GetMapping("/p003")
    @Trace(dispatcher = true)
    String getP003() {
        // ロガーを適切に設定することで、
        // ログを出力すると、Transactionと紐付けられる。
        // 詳細 https://docs.newrelic.com/docs/logs/logs-context/java-configure-logs-context-all/
        logger.info("Hello world!");
        return "index";
    }

    @GetMapping("/p004")
    @Trace(dispatcher = true)
    String getP004() {
        return "index";
    }

    @GetMapping("/p005")
    @Trace(dispatcher = true)
    String getP005() throws InterruptedException {
        // NewRelicのTransactionを使用することで、
        // どのメソッドが遅いのか？がわかる。
        Usecase.f002();
        Usecase.f003();
        return "index";
    }

    @GetMapping("/p006")
    @Trace(dispatcher = true)
    String getP006() throws InterruptedException {
        // 並列処理の例。
        // 素朴なマルチスレッドプログラム。

        // Jobクラスのrunメソッドが子スレッド上で実行される。
        // Jobクラスのrunメソッドをトレースすることはできない。

        // トレースできない理由は、Transactionに紐づいてないから。
        // 参考 https://docs.newrelic.com/docs/apm/agents/java-agent/async-instrumentation/introduction-java-async-instrumentation/
        // とのことらしいが、正直何を言ってるのかよく分からん。

        List<Thread> threads = List.of(
                new Thread(new Job(1000L), "job1"),
                new Thread(new Job(2000L), "job2"),
                new Thread(new Job(1000L), "job3")
        );
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        return "index";
    }

    @GetMapping("/p007")
    @Trace(dispatcher = true)
    String getP007() throws InterruptedException {
        Agent agent = NewRelic.getAgent();
        Token token = agent.getTransaction().getToken();
        // 並列処理の例。
        // 素朴なマルチスレッドプログラム。

        // Jobクラスのrunメソッドが子スレッド上で実行される。
        // Jobクラスのrunメソッドをトレースすることはできない。

        // 子スレッドの処理をTransactionへ紐づけるためには
        // tokenを渡してやり、link -> expire しろとのこと。
        // とりあえずやってみる。

        // 以下はダメ。
        // Job2のrun関数に付与されているアノテーションが
        // Traceだから。

        List<Thread> threads = List.of(
                new Thread(new Job2(1000L, token), "job1"),
                new Thread(new Job2(2000L, token), "job2"),
                new Thread(new Job2(1000L, token), "job3")
        );
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        token.expire();
        return "index";
    }

    @GetMapping("/p008")
    @Trace(dispatcher = true)
    String getP008() throws InterruptedException {
        Agent agent = NewRelic.getAgent();
        Token token = agent.getTransaction().getToken();
        // 並列処理の例。
        // 素朴なマルチスレッドプログラム。

        // Jobクラスのrunメソッドが子スレッド上で実行される。
        // Jobクラスのrunメソッドをトレースすることはできた！

        // 子スレッドの処理をTransactionへ紐づけるためには
        // tokenを渡してやり、link -> expire しろとのこと。
        // とりあえずやってみる。

        // Job3のrun関数に付与されているアノテーションを
        // Trace(async = true)にしなければならない。
        List<Thread> threads = List.of(
                new Thread(new Job3(1000L, token), "job1-thread"),
                new Thread(new Job3(2000L, token), "job2-thread"),
                new Thread(new Job3(1000L, token), "job3-thread")
        );
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        // expire関数を呼ぶと、トークンが無効化される。
        // 無効化された後のトークンを使うことはできない。
        token.expire();
        return "index";
    }

    @GetMapping("/p009")
    @Trace(dispatcher = true)
    String getP009() throws InterruptedException, ExecutionException {
        // 平行処理（≠並列処理）の例。
        Token token = NewRelic.getAgent().getTransaction().getToken();
        // スレッドプール上でJob3を実行する。
        // スレッドプール上には、2つのスレッドが存在している。
        List<Future> futures = List.of(
                thraedPoolForP009.submit(new Job3(1000L, token)),
                thraedPoolForP009.submit(new Job3(2000L, token)),
                thraedPoolForP009.submit(new Job3(1000L, token))
        );
        for (Future f : futures) {
            f.get();
        }
        token.expire();
        return "index";
    }

    @GetMapping("/p011")
    @Trace(dispatcher = true)
    String getP011() throws InterruptedException, ExecutionException {
        // 平行処理（≠並列処理）の例。
        Token token = NewRelic.getAgent().getTransaction().getToken();
        // スレッドプール上でJob3を実行する。
        // スレッドプール上には、2つのスレッドが存在している。
        List<Future> futures = List.of(
                thraedPoolForP009.submit(new Job4(1000L, token, NewRelic.getAgent().getTransaction().startSegment("hoge1"))),
                thraedPoolForP009.submit(new Job4(2000L, token, NewRelic.getAgent().getTransaction().startSegment("hoge2"))),
                thraedPoolForP009.submit(new Job4(1000L, token, NewRelic.getAgent().getTransaction().startSegment("hoge3")))
        );
        for (Future f : futures) {
            f.get();
        }
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("foo");
        segment.end();
        token.expire();
        return "index";
    }
}

class Usecase {
    static void f001() {
    }

    // Traceしたい関数には
    // Traceアノテーションをつける
    @Trace
    static void f002() {
    }

    @Trace
    static void f003() throws InterruptedException {
        Thread.sleep(1000L);
    }
}

class Job implements Runnable {
    final private Long waitSeconds;

    Job(Long waitSeconds) {
        this.waitSeconds = waitSeconds;
    }

    @Trace
    @Override
    public void run() {
        try {
            Thread.sleep(this.waitSeconds);
        } catch (InterruptedException e) {
        }
    }
}


class Job2 implements Runnable {
    final private Long waitSeconds;
    final private Token token;

    Job2(
            Long waitSeconds,
            Token token
    ) {
        this.waitSeconds = waitSeconds;
        this.token = token;
    }

    @Trace
    @Override
    public void run() {
        try {
            Thread.sleep(this.waitSeconds);
        } catch (InterruptedException e) {
        } finally {
            token.link();
        }
    }
}


class Job3 implements Runnable {
    final private Long waitSeconds;
    final private Token token;

    Job3(
            Long waitSeconds,
            Token token
    ) {
        this.waitSeconds = waitSeconds;
        this.token = token;
    }

    // 親とは異なるスレッドの中で実行される可能性のある関数には
    // @Trace(async = true)
    // を付与する。
    @Trace(async = true)
    @Override
    public void run() {
        try {
            Thread.sleep(this.waitSeconds);
        } catch (InterruptedException e) {
        } finally {
            token.link();
        }
    }
}


class Job4 implements Runnable {
    final private Long wait;
    final private Token token;
    final private Segment segment;

    Job4(
            Long wait,
            Token token,
            Segment segment
    ) {
        this.wait = wait;
        this.token = token;
        this.segment = segment;
    }

    // 親とは異なるスレッドの中で実行される可能性のある関数には
    // @Trace(async = true)
    // を付与する。
    @Trace(async = true)
    @Override
    public void run() {
        // ここでSegment生成してもダメ。なぜなら、親のTransactionとは違うから。
        Segment segment2 = NewRelic.getAgent().getTransaction().startSegment("bar");
        try {
            Thread.sleep(this.wait);
        } catch (InterruptedException e) {
        } finally {
            segment.end();
            segment2.end();
            token.link();
        }
    }
}