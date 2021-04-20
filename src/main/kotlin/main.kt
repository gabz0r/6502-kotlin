
fun main() {
    val mos6502 = Cpu6502(clkFreq = 10000)

    mos6502.reset()

    while(true) {
        mos6502.clock()
    }
}