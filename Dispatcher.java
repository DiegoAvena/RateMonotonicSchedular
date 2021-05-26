import java.util.concurrent.*;

/*

Dispatcher listens for semaphores of threads that become available
when the thread that they are waiting for finishes early (before the end of the
frame period). This allows threads that the RMS has scheduled to run but were paused due to
a higher priority thread needing to run, to RESUME from where
they were paused. Once the RMS wakes up again, this dispatch thread stops, and a new dispatch thread
is launched by the RMS

*/
class Dispatcher implements Runnable {

  RMS rmsToListenTo;

  Semaphore[] semaphoresOfTasks;
  SchedularContainer[] arrayOfTasks;
  Thread[] threadsForTheTasks;
  boolean exit;

  Dispatcher(RMS rmsToListenTo) {

    this.rmsToListenTo = rmsToListenTo;
    arrayOfTasks = rmsToListenTo.getArrayOfObjectsToCreateAndScheduleThreadsFor();
    threadsForTheTasks = rmsToListenTo.getThreadsCreated();
    semaphoresOfTasks = new Semaphore[threadsForTheTasks.length];

    for (int i = 0; i < threadsForTheTasks.length; i++) {

      semaphoresOfTasks[i] = arrayOfTasks[i].getSemaphore();

    }

  }

  public void run () {

    while (exit == false) {

      //wake up threads whose semaphores are available:
      for (int i = 1; i < semaphoresOfTasks.length; i++) {

        if (exit) {

          //stop thread
          break;

        }

        if (arrayOfTasks[i - 1].getFinishedRunning() && arrayOfTasks[i].getHasBeenScheduled()) {

            //resume the task thread, since previous task has finished running, which means the semaphore of this task is available:
            rmsToListenTo.setCurrentTaskIndex(i);
            threadsForTheTasks[i].interrupt();
            break;

        }
        else if ((arrayOfTasks[i].getHasBeenScheduled() == false)) {

          //Allow thread waiting for this thread to finish to run now:
          if (arrayOfTasks[i].getSemaphoreOfOtherTaskThatMustWaitForMeToFinish() != null) {

            arrayOfTasks[i].getSemaphoreOfOtherTaskThatMustWaitForMeToFinish().release();

          }

        }

      }

    }

  }

  public void stop() {

    exit = true;

  }

}
