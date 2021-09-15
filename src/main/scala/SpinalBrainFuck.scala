import spinal.core._
import spinal.lib._

object sbfState extends SpinalEnum { val FETCH, EXECUTE = newElement() }

class SpinalBrainFuck (
  prgMemSize : Int = 1024,
  tapeLen : Int = 1024,
  cellWidth : Int = 8,
  loopStackSize : Int = 32
) extends Component {
  val io = new Bundle {
    val run = in(Bool)
    val done = out(Bool)
    val inData = slave(Stream(Bits(8 bits)))
    val outData = master(Stream(Bits(8 bits)))
    val program = slave(Flow(new Bundle{
      val addr = UInt(log2Up(prgMemSize) bits)
      val data = Bits(8 bits)
    }))
  }
  io.inData.ready := False
  io.outData.valid := False
  io.done := False

  val stall = False
  val phase = Reg(sbfState()) init sbfState.FETCH
  when (io.run & phase === sbfState.FETCH) { phase := sbfState.EXECUTE }
  when (!stall & phase === sbfState.EXECUTE) { phase := sbfState.FETCH }

  val prgMem = Mem((0 until prgMemSize).map(_ => B(0, 8 bits)))
  prgMem.write(io.program.payload.addr, io.program.payload.data, io.program.valid)
  val prgMemPtr = Reg(UInt(log2Up(prgMemSize) bits)) init 0
  val instruction = prgMem.readSync(prgMemPtr, phase === sbfState.FETCH)
  when (!stall & phase === sbfState.EXECUTE) { prgMemPtr := prgMemPtr + 1 }
  when (!io.run) { prgMemPtr := 0 }

  val dataMem = Mem((0 until tapeLen).map(_ => U(0, cellWidth bits)))
  val dataMemPtr = CounterUpDown(tapeLen)
  val data = dataMem.readSync(dataMemPtr, phase === sbfState.FETCH)
  val dataMemWrite = False
  val dataMemWriteValue = U(0, cellWidth bits)
  dataMem.write(dataMemPtr, dataMemWriteValue, dataMemWrite)

  io.outData.payload := data.resized.asBits

  val loopStack = Mem((0 until loopStackSize).map(_ => U(0, log2Up(prgMemSize) bits)))
  val loopStackPtr = CounterUpDown(loopStackSize)
  val loopStackPush = False
  val loopStackPop = False
  val loopStackVal = loopStack.readSync(loopStackPtr, phase === sbfState.FETCH)
  loopStack.write(loopStackPtr.value + 1, prgMemPtr + 1, loopStackPush)
  when (loopStackPush) { loopStackPtr.increment() }
  when (loopStackPop) { loopStackPtr.decrement() }

  val skipMode = Reg(Bool) init False
  val skipModeCounter = CounterUpDown(loopStackSize)

  when (!skipMode & phase === sbfState.EXECUTE) {
    switch(instruction) {
      is(B('>', 8 bits)) { dataMemPtr.increment() }
      is(B('<', 8 bits)) { dataMemPtr.decrement() }
      is(B('+', 8 bits)) {
        dataMemWrite := True
        dataMemWriteValue := data + 1
      }
      is(B('-', 8 bits)) {
        dataMemWrite := True
        dataMemWriteValue := data - 1
      }
      is(B('.', 8 bits)) {
        io.outData.valid := True
        stall := !io.outData.ready
      }
      is(B(',', 8 bits)) {
        io.inData.ready := True
        stall := !io.inData.valid
        dataMemWrite := io.inData.valid
        dataMemWriteValue := io.inData.payload.resized.asUInt
      }
      is(B('[', 8 bits)) {
        when(data === 0) { skipMode := True } otherwise { loopStackPush := True }
      }
      is(B(']', 8 bits)) {
        when(data =/= 0) { prgMemPtr := loopStackVal } otherwise { loopStackPop := True }
      }
      is(B(0, 8 bits)) {
        stall := True
        io.done := True
      }
    } // end instruction switch
  }

  when (skipMode & phase === sbfState.EXECUTE) {
    switch (instruction) {
      is (B('[', 8 bits)) { skipModeCounter.increment() }
      is (B(']', 8 bits)) {
        when(skipModeCounter =/= 0) { skipModeCounter.decrement() } otherwise { skipMode := False }
      }
    }
  } // end skip mode switch
}

