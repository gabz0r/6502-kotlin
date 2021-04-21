import java.io.File

fun main() {
    val mos6502 = Cpu6502(clkFreq = 10000)

    val fib = loadRom("fib.sev")

    mos6502.MEM.load(fib, Memory6502.MLANG_P1.first.toUShort())
    mos6502.MEM.setInitializationVector(Memory6502.MLANG_P1.first.toUShort())

    mos6502.reset()

    while(true) {
        mos6502.clock()
    }
}

fun loadRom(filename: String): UByteArray {
    println(File("fib.asm").absolutePath)
    return File(filename).readBytes().toUByteArray()
}