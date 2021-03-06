package kafka.manager

import java.util.concurrent.{LinkedBlockingQueue, TimeUnit, ThreadPoolExecutor}

import akka.pattern._

import scala.concurrent.{Future, ExecutionContext}
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Created by hiral on 5/3/15.
 */

case class LongRunningPoolConfig(threadPoolSize: Int, maxQueueSize: Int)
trait LongRunningPoolActor extends BaseActor {
  
  protected val longRunningExecutor = new ThreadPoolExecutor(
    longRunningPoolConfig.threadPoolSize, longRunningPoolConfig.threadPoolSize,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue[Runnable](longRunningPoolConfig.maxQueueSize))
  protected val longRunningExecutionContext = ExecutionContext.fromExecutor(longRunningExecutor)

  protected def longRunningPoolConfig: LongRunningPoolConfig
  
  protected def longRunningQueueFull(): Unit

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.info("Shutting down long running executor...")
    Try(longRunningExecutor.shutdown())
    super.postStop()
  }

   protected def longRunning[T](fn: => Future[T])(implicit ec: ExecutionContext, ct: ClassTag[T]) : Unit = {
    if(longRunningExecutor.getQueue.remainingCapacity() == 0) {
      longRunningQueueFull()
    } else {
      fn match {
        case _ if ct.runtimeClass == classOf[Unit] =>
          //do nothing with unit
        case f =>
          f pipeTo sender
      }
    }
  }
}
