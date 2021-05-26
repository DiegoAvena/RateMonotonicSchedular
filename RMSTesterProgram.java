class RMSTesterProgram {

  public static void main(String[] args) {

    //Case 1:
    System.out.println("Case 1: No overruns" + '\n');
    BusyWork taskOne = new BusyWork(100, 1);
    BusyWork taskTwo = new BusyWork(200, 2);
    BusyWork taskThree = new BusyWork(400, 4);
    BusyWork taskFour = new BusyWork(1600, 16);

    BusyWork[] arrayOfTasks = new BusyWork[] {taskOne, taskTwo, taskThree, taskFour};
    RMS<BusyWork> scheduler = new RMS<BusyWork>(arrayOfTasks, 10);

    Thread RMSThread = new Thread(scheduler);

    RMSThread.start();

    try {

      RMSThread.join();

    } catch (Exception e) {


    }

    System.out.println("Case 2: Failed case where doWork function is called as many times as required to lead to an overrun condition t2");


    taskOne = new BusyWork(100, 1);
    taskTwo = new BusyWork(20000, 2);
    taskThree = new BusyWork(400, 4);
    taskFour = new BusyWork(1600, 16);

    arrayOfTasks = new BusyWork[] {taskOne, taskTwo, taskThree, taskFour};
    scheduler = new RMS<BusyWork>(arrayOfTasks, 10);

    RMSThread = new Thread(scheduler);

    RMSThread.start();

    try {

      RMSThread.join();

    } catch (Exception e) {


    }

    System.exit(0);

  }

}
