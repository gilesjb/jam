#!/usr/bin/env kotlin -Xjvm-default=all -cp build/jam-0.9.jar

interface FibonacciExample : Project {
    fun fib(x : Long) : Long = if (x < 2) x else fib(x - 1) + fib(x - 2)

    fun fib50() = fib(50)
}

Project.run(FibonacciExample::class.java, FibonacciExample::fib50, args)
