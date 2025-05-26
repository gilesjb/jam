#!/usr/bin/env kotlin -Xjvm-default=all -cp build/jam-0.9.jar

interface FibonacciExample : Project {
    fun fib(x : Long) : Long = if (x < 2) x else fib(x - 1) + fib(x - 2)

    fun demo() {
        println("fib(10) = ${fib(10)}")
        println("fib(20) = ${fib(20)}")
    }
}

Project.run(FibonacciExample::class.java, FibonacciExample::demo, args)
