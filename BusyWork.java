import java.util.concurrent.*;

/*

The task that the RMS will be scheduling.

*/
class BusyWork extends SchedularContainer {

  private int howManyTimesToWork; //specifies how many times this thread should run DoWork()

  BusyWork(int howManyTimesToWork, int taskPeriod) {

    super.taskPeriod = taskPeriod;
    this.howManyTimesToWork = howManyTimesToWork;

  }

  public void run() {

    finishedRunning = false;
    numberOfTimesThreadHasRan++;

    if (exit) {

      return;

    }

    try {

      mySemaphore.acquire();

    }
    catch (Exception e) {


    }

    for (int i = 0; i < howManyTimesToWork; i++) {

      if (exit) {

        break;

      }

      CheckIfThreadShouldPauseAndWait();
      DoWork();

    }

    if (semaphoreOfOtherTaskThatMustWaitForMeToFinish != null) {

      semaphoreOfOtherTaskThatMustWaitForMeToFinish.release();

    }

    finishedRunning = true;

  }

  private void DoWork() {

    int currentColumnInMatrix = 0;
    float matrix[][] = {{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f},
                        {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f}
                      };

    int orderInWhichToTraverseColumnsOfMatrixBy[] = {0, 5, 1, 6, 2, 7, 3, 8, 4, 9};

    float product = 1.0f;

    for (int i = 0; i < 10; i++) {

      if (exit) {

        break;

      }

      CheckIfThreadShouldPauseAndWait();

      currentColumnInMatrix = orderInWhichToTraverseColumnsOfMatrixBy[i];

      for (int row = 0; row < 10; row++) {

        if (exit) {

          break;

        }

        product *= matrix[row][currentColumnInMatrix];

      }

    }

  }

}
