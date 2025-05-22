**Part1 Q's:**
  Answer:
    Output:
      `Calling run()
      Running in: main
      Calling start()
      Running in: Thread-2
`    The start() method does not block the calling thread; it returns immediately after starting the new thread.
    Calling run() does not create a new thread; it simply executes the run() method in the context of the calling thread.
**Part2 Q's:**
  Answer:
    Output:
      `Main thread ends.
      Daemon thread running...`
    The daemon thread is likely being interrupted before it can complete its loop of 20 iterations. The JVM does not wait for daemon threads to finish; it simply terminates them when all user threads have completed.
    The JVM will wait for the thread to end it tasks 
    Also This must be done before the thread is started; otherwise, an IllegalThreadStateException will be thrown.
    Watching configuration files for changes,background tasks that should not block the application from exiting. However, because they can be terminated at any time, they should be used with caution, especially if they are performing critical tasks.
**Part3 Q's:**
  Answer:
    Output:
      `Thread is running using a ...!`
    The () -> { ... } syntax in your code is called a lambda expression.
    If you create a class that extends Thread, you would need to override the run() method in that class. This approach is more verbose and requires more boilerplate code.