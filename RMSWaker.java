import java.util.TimerTask;
import java.util.concurrent.*;

/*

Responsible for releasing the semaphore that
the RMS waits on for the RMS to do some scheduling again

*/
class RMSWaker extends TimerTask {

  private Semaphore semaphoreToRelease;

  RMSWaker(Semaphore semaphoreToRelease) {

    this.semaphoreToRelease = semaphoreToRelease;

  }

  public void run() {

    ReleaseSemaphore();

  }

  void ReleaseSemaphore() {

    semaphoreToRelease.release();

  }

}
