---
title: Android集成Aspectj
date: 2023-08-02 20:07:53
tags:
---

使用 AspectJ 在 Android 项目中实现面向切面编程（AOP）可以帮助您在代码中更好地处理横切关注点，如日志记录、权限控制、性能监测等。下面是在 Android 项目中使用 AspectJ 的详细流程：

## 1. 添加依赖和插件：
在您的 Android 项目中，首先需要添加 AspectJ 相关的依赖和插件。

+ 在项目级别的 `build.gradle` 文件中，添加 AspectJ 的 Gradle 插件依赖：

```gradle
dependencies {
    classpath 'org.aspectj:aspectjtools:1.9.8'
}
```
注意:`dependencies` 所在的`buildscript`节点一定要在`plugins`的前面

+ 在应用模块的 `build.gradle` 文件中添加以下内容：

在`dependencies`中添加

```agsl
dependencies {
    api 'org.aspectj:aspectjrt:1.9.8'
}
```

在`build.gradle`文件下面添加aspectj的编译配置：

```gradle
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
final def log = project.logger
final def variants = project.android.applicationVariants

variants.all { variant ->
    JavaCompile javaCompile = variant.javaCompileProvider.get()
    javaCompile.doLast {
        String[] args = ["-showWeaveInfo",
                         "-17",
                         "-inpath", javaCompile.destinationDir.toString(),
                         "-aspectpath", javaCompile.classpath.asPath,
                         "-d", javaCompile.destinationDir.toString(),
                         "-classpath", javaCompile.classpath.asPath,
                         "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]

        MessageHandler handler = new MessageHandler(true)
        new Main().run(args, handler)
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    break
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break
            }
        }
    }
}
```

## 2. 编写 AspectJ 切面：
创建一个 Java 类作为您的 AspectJ 切面，用于定义横切逻辑。切面类中定义了一系列切点和通知，以及需要在哪些地方织入这些通知。

```java

import android.util.Log;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class LoggingAspect {
    private static final String TAG = "LoggingAspect";
    ThreadLocal<Long> time = new ThreadLocal<>();
    @Before("execution(* com.snrt.helloworld.music.*.getMusics(..))")
    public void before() {
        // 在方法调用前执行的逻辑，比如打印日志
        long startTime = System.currentTimeMillis();
        Log.e(TAG, "方法开始时间: "+ startTime);
        time.set(startTime);
    }
    @After("execution(* com.snrt.helloworld.music.*.getMusics(..))")
    public void after() {
        // 在方法调用前执行的逻辑，比如打印日志
        long endTime = System.currentTimeMillis();
        Log.e(TAG, "方法结束时间: "+ endTime);
        Log.e(TAG, "after: "+ (endTime - time.get()) +"ms");
    }
}
```

## 3. 构建和运行：
完成上述步骤后，重新构建您的 Android 项目。AspectJ 将会在编译期间织入您的切面逻辑。
运行结束如下：

```agsl
2023-08-04 20:40:47.160 22060-22060 LoggingAspect           com.snrt.helloworld                  E  after: 12ms
2023-08-04 20:44:47.954 24627-24627 LoggingAspect           com.snrt.helloworld                  E  方法开始时间: 1691153087954
2023-08-04 20:44:47.968 24627-24627 LoggingAspect           com.snrt.helloworld                  E  方法结束时间: 1691153087968
2023-08-04 20:44:47.968 24627-24627 LoggingAspect           com.snrt.helloworld                  E  after: 14ms

```

以上就是在 Android 项目中使用 AspectJ 的基本流程。使用 AspectJ 可以更好地将横切关注点从业务逻辑中分离出来，从而提高代码的模块化和可维护性。


