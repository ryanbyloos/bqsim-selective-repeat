package Protocol;

import reso.common.AbstractTimer;
import reso.scheduler.AbstractScheduler;

public class Timer extends AbstractTimer {
    public Timer(AbstractScheduler scheduler, double interval) {
        super(scheduler, interval, true);
    }
    @Override
    protected void run() throws Exception {
        System.out.println("Current time: "+scheduler.getCurrentTime());
    }

}
