package ecb.cluster.tpcdi

import akka.actor.{ActorSystem, Address}
import akka.cluster.Cluster
import akka.cluster.metrics.{ClusterMetricsExtension, Metric, NodeMetrics, SigarMetricsCollector}
import org.hyperic.sigar.SigarProxy

class MetricsCollector(address: Address, decayFactor: Double, sigar: SigarProxy)
  extends SigarMetricsCollector(address, decayFactor, sigar) {

  //import StandardMetrics._
  import org.hyperic.sigar.NetStat

  /**
   * This constructor is used when creating an instance from configured FQCN
   */
  //def this(system: ActorSystem) = this(Cluster(system).selfAddress, ClusterMetricsExtension(system).settings)

  private val decayFactorOption = Some(decayFactor)

  override def metrics(): Set[Metric] = {
    // Must obtain cpuPerc in one shot. See https://github.com/akka/akka/issues/16121
    val netStat = sigar.getNetStat
    super.metrics union Set(tcpInboundTotal(netStat)).flatten
  }

  def tcpInboundTotal(netStat: NetStat): Option[Metric] = Metric.create(
    name = "TcpInboundTotal",
    value = netStat.getTcpInboundTotal.asInstanceOf[Number],
    decayFactor = decayFactorOption)
}

object Net {

  /*
  def smooth: Int = v match {
    case Some(value) => value 
    case None        => 0 
  }
  */

  def unapply(nodeMetrics: NodeMetrics): Option[(Address, Long, Long)] = {

      for {
        tcpInbound <- nodeMetrics.metric("TcpInboundTotal")
      } yield (nodeMetrics.address, nodeMetrics.timestamp,
        tcpInbound.smoothValue.longValue)

    //(nodeMetrics.address, nodeMetrics.timestamp, nodeMetrics.metric("TcpInboundTotal").get)
  }
}
