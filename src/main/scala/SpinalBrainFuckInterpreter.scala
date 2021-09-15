import spinal.core._
import spinal.core.sim._
import spinal.sim._

import scala.collection.mutable.StringBuilder
import scala.io.Source

object SpinalBrainFuckInterpreter  {
  def run (program : String, inputs : String = "", timeout : Int = 1000000) : String = {
    val simConfig = SimConfig
      .withWave
      .allOptimisation
      .compile(new SpinalBrainFuck(math.pow(2, log2Up(program.length + 1)).toInt))

    val outData = new StringBuilder
    simConfig.doSim { dut =>
      dut.io.run #= false
      dut.io.program.valid #= false
      dut.io.inData.valid #= false
      dut.io.outData.ready #= false

      dut.clockDomain.forkStimulus(10)
      dut.clockDomain.waitSampling(50)

      dut.io.program.valid #= true
      for ((char, idx) <- (program ++ "\0").map(_.toInt).zipWithIndex) {
        dut.io.program.addr #= idx
        dut.io.program.data #= char
        dut.clockDomain.waitSampling()
      }
      dut.io.program.valid #= false

      dut.clockDomain.waitSampling(10)

      dut.io.run #= true

      var timer = 0
      val timeoutThread = fork {
        while (timer < timeout && !dut.io.done.toBoolean) {
          timer += 1
          dut.clockDomain.waitSampling()
        }
      }

      val inputThread = fork {
        for (inVal <- inputs.toArray) {
          dut.io.inData.valid #= true
          dut.io.inData.payload #= inVal
          dut.clockDomain.waitRisingEdgeWhere(dut.io.inData.ready.toBoolean)
          dut.io.inData.valid #= false
        }
      }

      val outputThread = fork {
        while (true) {
          dut.io.outData.ready #= true
          dut.clockDomain.waitRisingEdgeWhere(dut.io.outData.valid.toBoolean)
          outData ++= dut.io.outData.payload.toInt.toChar.toString
          dut.io.outData.ready #= false
        }
      }

      timeoutThread.join()
      simSuccess()
    }
    outData.toString
  }

  def main(args: Array[String]): Unit = {
    val programFile = io.Source.fromFile(args.head)
    val program = try programFile.mkString finally programFile.close()
    val input = args.tail mkString " "

    println(run(program, input))
  }
}

