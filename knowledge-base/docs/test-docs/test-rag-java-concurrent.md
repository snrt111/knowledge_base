# Java 并发编程实战

## 1. 并发基础

### 1.1 线程创建方式

```java
// 方式1：继承 Thread 类
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running: " + Thread.currentThread().getName());
    }
}

MyThread thread = new MyThread();
thread.start();

// 方式2：实现 Runnable 接口
Runnable runnable = () -> {
    System.out.println("Runnable running: " + Thread.currentThread().getName());
};
new Thread(runnable).start();

// 方式3：实现 Callable 接口（有返回值）
Callable<String> callable = () -> {
    return "Result from " + Thread.currentThread().getName();
};
FutureTask<String> futureTask = new FutureTask<>(callable);
new Thread(futureTask).start();
String result = futureTask.get(); // 阻塞等待结果
```

### 1.2 线程状态

```
NEW -> RUNNABLE -> (BLOCKED/WAITING/TIMED_WAITING) -> TERMINATED
```

```java
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
});

System.out.println(thread.getState()); // NEW
thread.start();
System.out.println(thread.getState()); // RUNNABLE
Thread.sleep(100);
System.out.println(thread.getState()); // TIMED_WAITING
thread.join();
System.out.println(thread.getState()); // TERMINATED
```

### 1.3 线程池

```java
// 创建线程池
ExecutorService executor = Executors.newFixedThreadPool(10);

// 提交任务
executor.execute(() -> System.out.println("Task 1"));
Future<Integer> future = executor.submit(() -> 42);

// 关闭线程池
executor.shutdown();
executor.awaitTermination(60, TimeUnit.SECONDS);

// 推荐使用 ThreadPoolExecutor 自定义线程池
ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(
    5,                      // 核心线程数
    10,                     // 最大线程数
    60L,                    // 空闲线程存活时间
    TimeUnit.SECONDS,       // 时间单位
    new LinkedBlockingQueue<>(100),  // 任务队列
    new ThreadFactory() {   // 线程工厂
        private final AtomicInteger count = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "custom-pool-" + count.incrementAndGet());
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
);
```

## 2. 线程同步

### 2.1 synchronized 关键字

```java
public class Counter {
    private int count = 0;
    private final Object lock = new Object();

    // 同步实例方法（锁对象）
    public synchronized void increment() {
        count++;
    }

    // 同步代码块
    public void decrement() {
        synchronized (lock) {
            count--;
        }
    }

    // 同步静态方法（锁类）
    public static synchronized void staticMethod() {
        // ...
    }
}
```

### 2.2 ReentrantLock

```java
public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private int count = 0;

    public void increment() {
        lock.lock();
        try {
            count++;
            if (count > 0) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void awaitCondition() throws InterruptedException {
        lock.lock();
        try {
            while (count <= 0) {
                condition.await();
            }
            // 执行业务逻辑
        } finally {
            lock.unlock();
        }
    }

    // 尝试获取锁（非阻塞）
    public boolean tryIncrement() {
        if (lock.tryLock()) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    // 超时获取锁
    public boolean tryIncrementWithTimeout() throws InterruptedException {
        if (lock.tryLock(3, TimeUnit.SECONDS)) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

### 2.3 读写锁

```java
public class ReadWriteLockDemo {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private Map<String, String> data = new HashMap<>();

    // 读操作（多个线程可同时读）
    public String get(String key) {
        readLock.lock();
        try {
            return data.get(key);
        } finally {
            readLock.unlock();
        }
    }

    // 写操作（独占锁）
    public void put(String key, String value) {
        writeLock.lock();
        try {
            data.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 2.4  stampedLock

```java
public class StampedLockDemo {
    private final StampedLock lock = new StampedLock();
    private double x, y;

    // 乐观读
    public double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead();
        double currentX = x;
        double currentY = y;

        // 验证读期间是否有写操作
        if (!lock.validate(stamp)) {
            // 升级为悲观读锁
            stamp = lock.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // 写操作
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

## 3. 原子类

### 3.1 基本原子类

```java
// AtomicInteger
AtomicInteger atomicInt = new AtomicInteger(0);
atomicInt.incrementAndGet();  // ++i
atomicInt.getAndIncrement();  // i++
atomicInt.addAndGet(5);       // i += 5
atomicInt.compareAndSet(0, 1); // CAS 操作

// AtomicLong
AtomicLong atomicLong = new AtomicLong(0);

// AtomicBoolean
AtomicBoolean atomicBool = new AtomicBoolean(false);
atomicBool.compareAndSet(false, true);
```

### 3.2 原子引用

```java
// AtomicReference
AtomicReference<User> userRef = new AtomicReference<>(new User("John"));

// 使用 CAS 更新
User oldUser = userRef.get();
User newUser = new User("Jane");
userRef.compareAndSet(oldUser, newUser);

// AtomicStampedReference（解决 ABA 问题）
AtomicStampedReference<String> stampedRef = 
    new AtomicStampedReference<>("A", 0);

int[] stampHolder = new int[1];
String value = stampedRef.get(stampHolder);
stampedRef.compareAndSet(value, "B", stampHolder[0], stampHolder[0] + 1);

// AtomicMarkableReference
AtomicMarkableReference<String> markedRef = 
    new AtomicMarkableReference<>("A", false);
```

### 3.3 LongAdder（高并发计数器）

```java
// LongAdder 比 AtomicLong 在高并发下性能更好
LongAdder adder = new LongAdder();

// 多线程累加
for (int i = 0; i < 100; i++) {
    new Thread(() -> {
        for (int j = 0; j < 10000; j++) {
            adder.increment();
        }
    }).start();
}

// 获取总和
long sum = adder.sum();
```

## 4. 并发集合

### 4.1 ConcurrentHashMap

```java
// 创建 ConcurrentHashMap
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// 基本操作
map.put("key", 1);
map.putIfAbsent("key", 2);  // key 不存在时才 put
map.computeIfAbsent("key2", k -> k.length());
map.computeIfPresent("key", (k, v) -> v + 1);

// 批量操作
map.forEach(3, (k, v) -> System.out.println(k + "=" + v));

// 搜索
String result = map.search(3, (k, v) -> v > 5 ? k : null);

// 归约
long sum = map.reduceValuesToLong(3, v -> v, 0, Long::sum);
```

### 4.2 CopyOnWriteArrayList

```java
// 读多写少场景使用
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// 读操作（无锁）
for (String item : list) {
    System.out.println(item);
}

// 写操作（加锁并复制数组）
list.add("new item");
list.remove("old item");
```

### 4.3 BlockingQueue

```java
// ArrayBlockingQueue - 有界队列
BlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(100);

// LinkedBlockingQueue - 可选有界
BlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>();

// PriorityBlockingQueue - 优先队列
BlockingQueue<Task> priorityQueue = new PriorityBlockingQueue<>();

// DelayQueue - 延迟队列
DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();

// SynchronousQueue - 同步队列
BlockingQueue<String> syncQueue = new SynchronousQueue<>();

// 生产者-消费者示例
public class ProducerConsumer {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);

    public void produce() throws InterruptedException {
        while (true) {
            String item = produceItem();
            queue.put(item);  // 队列满时阻塞
        }
    }

    public void consume() throws InterruptedException {
        while (true) {
            String item = queue.take();  // 队列空时阻塞
            consumeItem(item);
        }
    }
}
```

## 5. 并发工具类

### 5.1 CountDownLatch

```java
public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        int workerCount = 3;
        CountDownLatch latch = new CountDownLatch(workerCount);

        for (int i = 0; i < workerCount; i++) {
            final int workerId = i;
            new Thread(() -> {
                try {
                    System.out.println("Worker " + workerId + " is working...");
                    Thread.sleep(1000);
                    System.out.println("Worker " + workerId + " finished.");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        System.out.println("Waiting for all workers...");
        latch.await();  // 等待所有 worker 完成
        System.out.println("All workers finished!");
    }
}
```

### 5.2 CyclicBarrier

```java
public class CyclicBarrierDemo {
    public static void main(String[] args) {
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            System.out.println("All parties arrived at barrier!");
        });

        for (int i = 0; i < parties; i++) {
            final int partyId = i;
            new Thread(() -> {
                try {
                    System.out.println("Party " + partyId + " is waiting...");
                    barrier.await();
                    System.out.println("Party " + partyId + " continues...");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // CyclicBarrier 可重复使用
        barrier.reset();
    }
}
```

### 5.3 Semaphore

```java
public class SemaphoreDemo {
    private final Semaphore semaphore = new Semaphore(5); // 5 个许可

    public void accessResource() {
        try {
            semaphore.acquire();  // 获取许可
            try {
                // 访问受限资源
                System.out.println("Accessing resource...");
                Thread.sleep(1000);
            } finally {
                semaphore.release();  // 释放许可
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 尝试获取（非阻塞）
    public boolean tryAccessResource() {
        if (semaphore.tryAcquire()) {
            try {
                // 访问资源
                return true;
            } finally {
                semaphore.release();
            }
        }
        return false;
    }
}
```

### 5.4 CompletableFuture

```java
public class CompletableFutureDemo {
    public static void main(String[] args) {
        // 创建异步任务
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Hello";
        });

        // 链式操作
        future.thenApply(String::toUpperCase)
              .thenCombine(
                  CompletableFuture.supplyAsync(() -> " World"),
                  (s1, s2) -> s1 + s2
              )
              .thenAccept(System.out::println);

        // 异常处理
        CompletableFuture<String> futureWithError = CompletableFuture
            .supplyAsync(() -> {
                if (true) throw new RuntimeException("Error!");
                return "Success";
            })
            .exceptionally(ex -> "Recovered from: " + ex.getMessage());

        // 并行执行多个任务
        CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Task 1");
        CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "Task 2");
        CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "Task 3");

        CompletableFuture.allOf(f1, f2, f3)
            .thenApply(v -> f1.join() + ", " + f2.join() + ", " + f3.join())
            .thenAccept(System.out::println);

        // 等待完成
        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
            .execute(() -> System.out.println("Delayed task"));
    }
}
```

## 6. Fork/Join 框架

```java
public class ForkJoinDemo {
    // 计算数组求和
    static class SumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 10000;
        private final long[] array;
        private final int start;
        private final int end;

        SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= THRESHOLD) {
                // 直接计算
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            }

            // 拆分任务
            int mid = (start + end) / 2;
            SumTask left = new SumTask(array, start, mid);
            SumTask right = new SumTask(array, mid, end);

            left.fork();
            long rightResult = right.compute();
            long leftResult = left.join();

            return leftResult + rightResult;
        }
    }

    public static void main(String[] args) {
        long[] array = new long[1000000];
        Arrays.fill(array, 1);

        ForkJoinPool pool = new ForkJoinPool();
        long result = pool.invoke(new SumTask(array, 0, array.length));
        System.out.println("Sum: " + result);
    }
}
```

## 7. 线程安全最佳实践

1. **优先使用不可变对象**
   - 使用 `final` 修饰类和字段
   - 使用不可变集合（`Collections.unmodifiableList`）

2. **最小化同步范围**
   - 只在必要时同步
   - 使用细粒度锁

3. **使用并发集合替代同步集合**
   - `ConcurrentHashMap` 替代 `Collections.synchronizedMap`
   - `CopyOnWriteArrayList` 替代 `Vector`

4. **避免死锁**
   - 按固定顺序获取锁
   - 使用 `tryLock` 超时获取

5. **使用线程池管理线程**
   - 避免直接创建线程
   - 合理配置线程池参数

---

本文档详细介绍了 Java 并发编程的核心概念、同步机制、原子类、并发集合、工具类以及最佳实践，是 Java 并发编程的完整参考指南。
