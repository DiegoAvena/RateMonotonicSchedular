import java.util.concurrent.*;
import java.util.*;

class RMS <T extends SchedularContainer> implements Runnable{

  /*

  Contains all of the threads that will be scheduled here. Sorted based
  on the thread priority

  */
  private T[] arrayOfObjectsToCreateAndScheduleThreadsFor; //Contains the actual objects going into the threads, is sorted from least period to greatest before thread creation
  private Thread[] threadsCreated; //Keeps track of all the threads created
  private int[] threadPriorities; //Stores the assigned thread priorities, so that when threads are recreated the same priority can be used, these are in order, so index 0 stores the priority of the thread at index 0 in the array above, etc
  private int unitSize; //should be between 10ms to 100ms

  private int framePeriod; //For this assignment, the program ends when the scheduler goes through this frame period 10x
  private int numberOfFramePeriodsToComplete; //program ends when numberOfFramePeriodsCompleted >= this
  private int numberOfFramePeriodsCompleted; //number of frame periods completed so far
  private int currentFrame; //when this equals the framePeriod, I increment numberOfFramePeriodsCompleted
  private int idleFramePeriod; //how long the RMS sleeps for until it wakes up on its own to being a new frame
  private int currentTaskIndex; //The index of the current thread that was most recently running, so that it can be paused without having to loop over the other threads

  private Semaphore RMSSemaphore; //the semaphore that the RMS waits on before scheduling again

  private Dispatcher dispatcher; //while RMS waits, this dispatcher resumes threads this RMS scheduled by checking if the previous thread running is done
  Thread dispatchThread; //the thread the dispatcher goes on

  public RMS(T[] arrayOfObjectsToCreateAndScheduleThreadsFor, int unitSize) {

    RMSSemaphore = new Semaphore(1);

    this.arrayOfObjectsToCreateAndScheduleThreadsFor = arrayOfObjectsToCreateAndScheduleThreadsFor;
    this.unitSize = unitSize;

    framePeriod = 16;
    numberOfFramePeriodsCompleted = 0;
    numberOfFramePeriodsToComplete = 10;
    currentFrame = 0;
    currentTaskIndex = 0;

    threadsCreated = new Thread[arrayOfObjectsToCreateAndScheduleThreadsFor.length];
    threadPriorities = new int[arrayOfObjectsToCreateAndScheduleThreadsFor.length];

  }

  public Semaphore getRMSSemaphore() {

    return RMSSemaphore;

  }

  public void run() {

    initialize();
    doScheduling();

  }

  public int getCurrentTaskIndex() {

    return currentTaskIndex;

  }

  public void setCurrentTaskIndex(int currentTaskIndex) {

    this.currentTaskIndex = currentTaskIndex;

  }

  public T[] getArrayOfObjectsToCreateAndScheduleThreadsFor() {

    return arrayOfObjectsToCreateAndScheduleThreadsFor;

  }

  public Thread[] getThreadsCreated() {

    return threadsCreated;

  }

  private void initialize() {

    AssignPrioritiesAndCreateThreads();

    //want the idle frame period to be as small as the smallest period for the threads that need to be scheduled:
    idleFramePeriod = arrayOfObjectsToCreateAndScheduleThreadsFor[0].getTaskPeriod();

    //initialize the semaphore of the thread with highest priority (which is at index 0 since I already sorted the array) to 1 so that it can run
    arrayOfObjectsToCreateAndScheduleThreadsFor[0].getSemaphore().release();

    for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

      if ((i + 1) < arrayOfObjectsToCreateAndScheduleThreadsFor.length) {

        arrayOfObjectsToCreateAndScheduleThreadsFor[i].setSemaphoreOfOtherTaskThatMustWaitForMeToFinish(arrayOfObjectsToCreateAndScheduleThreadsFor[i + 1].getSemaphore());

      }

    }

    for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

      arrayOfObjectsToCreateAndScheduleThreadsFor[i].setFrameTaskMustBeCompletedBy(currentFrame + arrayOfObjectsToCreateAndScheduleThreadsFor[i].getTaskPeriod());

    }

    //At time 0, all threads scheduled:
    for (int i = 0; i < threadsCreated.length; i++) {

      threadsCreated[i].start();
      arrayOfObjectsToCreateAndScheduleThreadsFor[i].setHasBeenScheduled(true);

    }

    launchDispatcher();

    try {

      RMSSemaphore.acquire();

    } catch (Exception e) {

    }

    startTimerUntilNextSchedulingOccurs();

  }

  private void launchDispatcher() {

    dispatcher = new Dispatcher(this);
    Thread dispatchThread = new Thread(dispatcher);
    dispatchThread.start();

  }

  private void startTimerUntilNextSchedulingOccurs() {

    Timer timer = new Timer();
    RMSWaker waker = new RMSWaker(RMSSemaphore);
    timer.schedule(waker, idleFramePeriod * unitSize);

  }

  private void doScheduling() {

    while (true) {

      try {

        RMSSemaphore.acquire();

      } catch (Exception e) {



      }

      dispatcher.stop();
      currentFrame++;

      //check for overruns in current task:
      for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

        if ((arrayOfObjectsToCreateAndScheduleThreadsFor[i].getFrameTaskMustBeCompletedBy() > currentFrame) && (arrayOfObjectsToCreateAndScheduleThreadsFor[i].getFinishedRunning() == false) && (arrayOfObjectsToCreateAndScheduleThreadsFor[i].getThisTaskRecentlyOverranItsDeadline() == false)) {

          //this task had an overrun:
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setThisTaskRecentlyOverranItsDeadline(true); //so that this task skips its next execution period
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].SetNumberOfTimesThreadOverran(arrayOfObjectsToCreateAndScheduleThreadsFor[i].getNumberOfTimesThreadOverran() + 1);
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setHasBeenScheduled(false);

        }
        else if (arrayOfObjectsToCreateAndScheduleThreadsFor[i].getThisTaskRecentlyOverranItsDeadline() && ((currentFrame % arrayOfObjectsToCreateAndScheduleThreadsFor[i].getTaskPeriod()) == 0)) {

          //Insures that while the scheduling of this thread is skipped for its next execution period, it does not skip the one after
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setThisTaskRecentlyOverranItsDeadline(false);
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setHasBeenScheduled(true); //so that dispatcher knows it can now resume this thread for completion

        }

      }

      //pause thread that was running, since thread 1 now needs to run
      if ((arrayOfObjectsToCreateAndScheduleThreadsFor[currentTaskIndex].getFinishedRunning() == false) && (currentTaskIndex != 0)) {

        arrayOfObjectsToCreateAndScheduleThreadsFor[currentTaskIndex].Pause();

      }

      //Check if RMS has completed another frame period (RMS should only run for 10 frame periods, then everything ends)
      if (currentFrame == framePeriod) {

        currentFrame = 0;
        numberOfFramePeriodsCompleted++;
        currentTaskIndex = 0;

        //Check if RMS is done, (rms has completed 10 frame periods)
        if (numberOfFramePeriodsCompleted == numberOfFramePeriodsToComplete) {

          //done
          for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

            arrayOfObjectsToCreateAndScheduleThreadsFor[i].getSemaphore().release();
            arrayOfObjectsToCreateAndScheduleThreadsFor[i].stop();

          }

          RMSSemaphore.release();

          printResults();
          break;

        }

      }

      //Reschedule threads:
      for (int i = 0; i < threadsCreated.length; i++) {

        arrayOfObjectsToCreateAndScheduleThreadsFor[i].getSemaphore().release();

        if (i > 0) {

          //Close the semaphores of the other threads:
          try {

            arrayOfObjectsToCreateAndScheduleThreadsFor[i].getSemaphore().acquire();

          }
          catch (Exception e) {


          }

        }

        if (arrayOfObjectsToCreateAndScheduleThreadsFor[i].getFinishedRunning() && ((currentFrame % arrayOfObjectsToCreateAndScheduleThreadsFor[i].getTaskPeriod()) == 0) && (arrayOfObjectsToCreateAndScheduleThreadsFor[i].getThisTaskRecentlyOverranItsDeadline() == false)) {

          //reschedule this thread
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setFrameTaskMustBeCompletedBy(currentFrame + arrayOfObjectsToCreateAndScheduleThreadsFor[i].getTaskPeriod());
          threadsCreated[i] = new Thread(arrayOfObjectsToCreateAndScheduleThreadsFor[i]);
          threadsCreated[i].setPriority(threadPriorities[i]);
          arrayOfObjectsToCreateAndScheduleThreadsFor[i].setHasBeenScheduled(true);

          try {

            threadsCreated[i].start();

          } catch (Exception e) {

          }

        }

      }

      launchDispatcher();
      startTimerUntilNextSchedulingOccurs();

    }

  }

  private void printResults() {

    for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

      System.out.println("Task "+(i + 1)+" results: ");
      System.out.println("Times ran: "+arrayOfObjectsToCreateAndScheduleThreadsFor[i].getNumberOfTimesThreadHasRan());
      System.out.println("Times it overran: "+arrayOfObjectsToCreateAndScheduleThreadsFor[i].getNumberOfTimesThreadOverran());
      System.out.println();

    }

  }

  private void AssignPrioritiesAndCreateThreads() {

    //start insertion sort to sort tasks from least period to greatest period
    for (int i = 1; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

      T currentItem = arrayOfObjectsToCreateAndScheduleThreadsFor[i];
      int periodOfTaskAtCurrentSpot = arrayOfObjectsToCreateAndScheduleThreadsFor[i].getTaskPeriod();

      /*
      -going to compare this value to the left neighbors,
      and if any of these are bigger, we shift them right to make room for the
      currentValueToInsertIntoCorrectSpot in the array
      */

      int j = i - 1;

      while (j >= 0 && (arrayOfObjectsToCreateAndScheduleThreadsFor[j].getTaskPeriod() > periodOfTaskAtCurrentSpot)) {

        //this left neighbor is bigger, and needs to be shifted right to make some room for currentValueToInsertIntoCorrectSpot
        arrayOfObjectsToCreateAndScheduleThreadsFor[j + 1] = arrayOfObjectsToCreateAndScheduleThreadsFor[j];
        j--;

      }

      /*
      -By this point we are at the correct spot to insert the value at in the array:
      do j + 1 because j will always be 1 less then the actual spot this value should be inserted at due to doing j--
      */
      arrayOfObjectsToCreateAndScheduleThreadsFor[j + 1] = currentItem;

    }

    //by this point, I have an array of objects that are sorted from least task period to greatest task period, so now I can easily assign priorities to the threads I create below:

    int currentPriority = 10; //the max priority goes to the task with the smallest period, which is at index 0 in arrayOfObjectsToCreateAndScheduleThreadsFor since its sorted...

    //Assign the priorites now, and create the threads:
    for (int i = 0; i < arrayOfObjectsToCreateAndScheduleThreadsFor.length; i++) {

      Thread thread = new Thread(arrayOfObjectsToCreateAndScheduleThreadsFor[i]);
      thread.setPriority(currentPriority);
      threadPriorities[i] = currentPriority;

      currentPriority--;
      threadsCreated[i] = thread;

    }

  }

}
