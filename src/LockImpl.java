import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * @author wangyoucai
 * @date 2019/10/10
 */
public class LockImpl implements Mlock {

    private Sync sync;

    /**
     * 默认非公平锁
     */
    public LockImpl() {
        sync = new NonFairLock();
    }

    public LockImpl(boolean fair) {
        sync =  fair ? new FairLock() :  new NonFairLock();
    }

    /**
     * 获取锁
     * 如果锁未被别的线程占用，则立刻获取锁，并把当前线程设置为持有锁的线程
     * 如果锁已经被别的线程持有，则进入等待队列进行阻塞，在此之前还有两次尝试获取锁的行为，均失败才会进入队列
     */
    @Override
    public void lock() {
        sync.lock();
    }

    /**
     * 释放锁
     * 唤醒后继节点
     */
    @Override
    public void unlock() {
        sync.unlock();
    }

    abstract static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 获取锁的抽象方法 具体实现由子类完成 比如公平锁与非公平锁
         */
        abstract void lock();

        /**
         * 释放锁的操作 具体实现由子类完成
         * 其实完全可以在父类实现 因为公平锁和非公平锁的实现一样的
         */
        abstract void unlock();

        /**
         * 因为公平锁和非公平锁的释放锁操作都相同所以可放在父类实现即可
         * 而获取锁的操作，对于公平锁和非公平锁的实现不一样 所以放到具体实现类
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            int c = getState() - arg;
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }
    }

    /**
     * 非公平锁
     * 非公平锁通俗的解释就是新来的线程可以往等待队列中进行插队
     * 比如，当前持有锁的线程刚刚释放，还没来的及唤醒后继节点
     */
    static final class NonFairLock extends Sync {
        @Override
        final void lock() {
            // 1.尝试快速获取锁，抢占 非公平
            // 2.否则自旋阻塞
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
            } else {
                acquire(1);
            }
        }

        @Override
        void unlock() {
            release(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            Thread t = Thread.currentThread();
            int state = getState();
            // 0 表示锁空闲
            if (state == 0){
                // 通过原子操作来更新状态 如果成功 设置当前线程为持有锁的线程
                if (compareAndSetState(0,1)){
                    setExclusiveOwnerThread(t);
                    return true;
                }
            }else if (getExclusiveOwnerThread() == t){
                // 如果状态非0 有两种可能 1.别的线程获取锁了 2.当前线程重入
                // 第一重情况返回false 下面处理第二种
                int newState = state + arg;
                setState(newState);
                return true;
            }
            return false;
        }

    }

    /**
     * 公平锁 完全按照FIFO顺序来获取锁
     */
    static final class FairLock extends Sync {

        @Override
        final void lock() {
            acquire(1);
        }

        @Override
        void unlock() {
            release(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            Thread t = Thread.currentThread();
            int state = getState();
            // 0 表示锁空闲
            if (state == 0){
                // 因为是公平锁 所以首先检查是否有比他先到的线程在等待，其他逻辑一样
                if (!hasQueuedPredecessors() && compareAndSetState(0,1)){
                    setExclusiveOwnerThread(t);
                    return true;
                }
            }else if (getExclusiveOwnerThread() == t){
                // 如果状态非0 有两种可能 1.别的线程获取锁了 2.当前线程重入
                // 第一重情况返回false 下面处理第二种
                int newState = state + arg;
                setState(newState);
                return true;
            }
            return false;
        }

    }


}
