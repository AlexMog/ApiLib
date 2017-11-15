package alexmog.apilib.services;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import alexmog.apilib.ApiServer;

public abstract class BasicWaitAndExecuteService implements Service {

	protected Deque<Runnable> mActionMaps = new LinkedList<Runnable>();
	protected Lock mLock = new ReentrantLock();
	protected Condition mCondVar = mLock.newCondition();
	protected boolean mRunning = true;
	
	protected void addAction(Runnable r) {
		mLock.lock();
		try {
			mActionMaps.push(r);
			mCondVar.signalAll();
		} finally {
			mLock.unlock();
		}
	}
	
	@Override
	public void stop() {
		mLock.lock();
		try {
			mRunning = false;
			mCondVar.signalAll();
		} finally {
			mLock.unlock();
		}
	}
	
	@Override
	public void run() {
		while (mRunning) {
			mLock.lock();
			try {
				if (mActionMaps.isEmpty()) {
					mCondVar.await();
				}
				while (mRunning && !mActionMaps.isEmpty()) {
					mActionMaps.pop().run();
				}
			} catch (InterruptedException e) {
				ApiServer.LOGGER.log(Level.SEVERE, "Service", e);
			} finally {
				mLock.unlock();
			}
		}
	}
	
}
