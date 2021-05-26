import java.util.concurrent.*;

/*

Tasks that are to be scheduled by the RMS
must extend from this class, since this class
contains all of the data needed for the task to be
handled by the RMS

*/
public abstract class SchedularContainer implements Runnable {

  protected int numberOfTimesThreadHasRan; //keeps track of how many times this task has ran
  private int numberOfTimesThreadOverran; //the counter that tracks how many times this task overran its deadline
  protected boolean finishedRunning; //signals that this task finished, used to help detect an overrun
  protected boolean thisTaskRecentlyOverranItsDeadline; //set to true when this task overruns, set to false after 1 execution period has passed

  protected boolean exit; //if true, the thread this task is on will quit
  protected boolean threadShouldPauseAndWait; //If true, puts the thread this task is on to sleep until it is awakened again, the thread will not exit with this

  protected Semaphore mySemaphore; //the semaphore of this task

  /*

  -the semaphore of a task waiting for this task to finish, (task 3 waits for task 2 to finish, and task 4 waits for task 3 to finish,
  when this task finishes, it must release the semaphore of the task waiting on it)

  */
  protected Semaphore semaphoreOfOtherTaskThatMustWaitForMeToFinish;

  //Used by the RMS:
  protected int taskPeriod; //the period for this task
  protected int frameTaskMustBeCompletedBy; //the deadline of this task
  protected boolean hasBeenScheduled; //true if RMS scheduled this task, false for 1 execution period in cases where task misses its deadline

  public abstract void run();

  public SchedularContainer() {

    mySemaphore = new Semaphore(0);
    numberOfTimesThreadHasRan = 0; //keeps track of how many times this task has ran
    numberOfTimesThreadOverran = 0;
    finishedRunning = false; //signals that this task finished

    //Used by the RMS:
    taskPeriod = 0;

  }

  public boolean getHasBeenScheduled() {

    return hasBeenScheduled;

  }

  public void setHasBeenScheduled(boolean hasBeenScheduled) {

    this.hasBeenScheduled = hasBeenScheduled;

  }

  public void setSemaphoreOfOtherTaskThatMustWaitForMeToFinish(Semaphore semaphoreOfOtherTaskThatMustWaitForMeToFinish) {

    this.semaphoreOfOtherTaskThatMustWaitForMeToFinish = semaphoreOfOtherTaskThatMustWaitForMeToFinish;

  }

  public Semaphore getSemaphoreOfOtherTaskThatMustWaitForMeToFinish() {

    return semaphoreOfOtherTaskThatMustWaitForMeToFinish;

  }

  public void setMySemaphore(Semaphore mySemaphore) {

    this.mySemaphore = mySemaphore;

  }

  public void SetSemaphoreFree() {

    mySemaphore.release();

  }

  public Semaphore getSemaphore() {

    return mySemaphore;

  }

  public void setFrameTaskMustBeCompletedBy(int frameTaskMustBeCompletedBy) {

    this.frameTaskMustBeCompletedBy = frameTaskMustBeCompletedBy;

  }

  public int getFrameTaskMustBeCompletedBy() {

    return frameTaskMustBeCompletedBy;

  }

  public void SetNumberOfTimesThreadOverran(int numberOfTimesThreadOverran) {

    this.numberOfTimesThreadOverran = numberOfTimesThreadOverran;

  }

  public int getNumberOfTimesThreadOverran() {

    return numberOfTimesThreadOverran;

  }

  public int getTaskPeriod() {

    return taskPeriod;

  }

  public int getNumberOfTimesThreadHasRan() {

    return numberOfTimesThreadHasRan;

  }

  public boolean getFinishedRunning() {

    return this.finishedRunning;

  }

  public void setThisTaskRecentlyOverranItsDeadline(boolean thisTaskRecentlyOverranItsDeadline) {

    this.thisTaskRecentlyOverranItsDeadline = thisTaskRecentlyOverranItsDeadline;

  }

  public boolean getThisTaskRecentlyOverranItsDeadline() {

    return thisTaskRecentlyOverranItsDeadline;

  }

  public void stop() {

    exit = true;

  }

  //Pauses the thread
  protected void CheckIfThreadShouldPauseAndWait() {

    if (threadShouldPauseAndWait) {

      try {

        Thread.sleep(Long.MAX_VALUE);

      } catch (InterruptedException e) {

        threadShouldPauseAndWait = false;

      }

    }

  }

  public void Pause() {

    threadShouldPauseAndWait = true;

  }

}
