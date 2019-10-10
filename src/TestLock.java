import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author wangyoucai
 * @date 2019/10/10
 */
public class TestLock {
    static int j = 0;
    public static void main(String[] args) throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(10);
        for (int i=0;i<11;i++){
            service.execute(new Encre());
        }
        Thread.sleep(500);
        System.out.println(j);
        service.shutdownNow();
    }
    static class Encre implements Runnable{
        Mlock mlock = new LockImpl();
        @Override
        public void run() {

            try {
                mlock.lock();
                for (int i = 0; i < 13; i++) {

                    j++;

                }
            } finally {
                mlock.unlock();
            }

        }
    }
}
