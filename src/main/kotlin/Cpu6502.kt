import jdk.jfr.Unsigned
import kotlin.experimental.and
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

data class Cpu6502(
    var clkFreq: Long = 10,

    // states
    var PC: UShort = 0u,
    var SP: UShort = 0u,

    // registers
    var ACC: UByte = 0u,
    var IDX: UByte = 0u,
    var IDY: UByte = 0u,

    // status flags
    var CRY: Boolean = false,
    var ZER: Boolean = false,
    var IRD: Boolean = false,
    var DEC: Boolean = false,
    var BRK: Boolean = false,
    var OVF: Boolean = false,
    var NEG: Boolean = false,

    var MEM: Memory6502 = Memory6502()
) {

    private var cycles = 0
    private val SPOF: UShort
        get() = SP.plus(0x0100u).toUShort()

    fun reset() {

        // get kernal address from iv
        val initializationVector = 0xFFFC

        val ivPointerLow = MEM[initializationVector].toUInt()
        val ivPointerHigh = MEM[initializationVector + 1].toUInt()
        PC = (((ivPointerHigh and 0xFFu)) shl 0x08 or (ivPointerLow and 0xFFu)).toUShort()
        SP = 0xFDu

        ACC = 0x00u
        IDX = 0x00u
        IDY = 0x00u

        CRY = false
        ZER = false
        IRD = false
        DEC = false
        BRK = false
        OVF = false
        NEG = false
    }

    fun clock() {
        val elapsed = measureTimeMillis {
            if(cycles == 0) {

                val opc = MEM[PC]
                PC++

                val instruction = Instructions[opc]

                if(instruction == null) {
                    println("Execution finished or unknown operation!")

                    MEM.dump(Memory6502.ZEROPAGE)
                    MEM.dump(Memory6502.MLANG_P1)
                    MEM.dump(Memory6502.IV)
                    exitProcess(0)
                }

                instruction?.let {
                    cycles = instruction.Cycles.toInt()

                    execute(instruction)
                    __debugInternalCpuState(instruction.Mnemonic)
                }
            }
            cycles--
        }
        Thread.sleep(1000 / clkFreq)
    }

    private fun execute(ins: Instruction6502) {
        when {
            ins.Mnemonic == "LDX" && ins.AddrMode == AddressMode.IMMEDIATE -> {
                IDX = MEM[PC]
                PC++
                ZER = IDX.toInt() == 0
                NEG = (IDX and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "LDX" && ins.AddrMode == AddressMode.ZEROPAGE -> {
                val addr = addressZeroPage()
                IDX = MEM[addr]
                ZER = IDX.toInt() == 0
                NEG = (IDX and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "LDY" && ins.AddrMode == AddressMode.IMMEDIATE -> {
                IDY = MEM[PC]
                PC++
                ZER = IDY.toInt() == 0
                NEG = (IDY and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "LDA" && ins.AddrMode == AddressMode.IMMEDIATE -> {
                ACC = MEM[PC]
                PC++
                ZER = ACC.toInt() == 0
                NEG = (ACC and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "TYA" && ins.AddrMode == AddressMode.IMPLICIT -> {
                ACC = IDY
                ZER = ACC.toInt() == 0
                NEG = (ACC and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "TAY" && ins.AddrMode == AddressMode.IMPLICIT -> {
                IDY = ACC
                ZER = IDY.toInt() == 0
                NEG = (IDY and 0x80u).toInt() == 1 // zweierkomplement an stelle 8 => Negativ
            }
            ins.Mnemonic == "SEI" && ins.AddrMode == AddressMode.IMPLICIT -> {
                IRD = true // interrupts deaktivieren
            }
            ins.Mnemonic == "TXS" && ins.AddrMode == AddressMode.IMPLICIT -> {
                SP = IDX.toUShort()
            }
            ins.Mnemonic == "CLD" && ins.AddrMode == AddressMode.IMPLICIT -> {
                DEC = false
            }
            ins.Mnemonic == "JSR" && ins.AddrMode == AddressMode.ABSOLUTE -> {
                MEM[SPOF] = ((PC.toUInt().plus(0x02u) and 0xFF00u) shr 0x08).toUByte()
                SP--
                MEM[SPOF] = (PC.toUInt().plus(0x02u) and 0x00FFu).toUByte()

                // sprungadresse holen
                PC = addressAbsolute()

            }
            ins.Mnemonic == "RTS" && ins.AddrMode == AddressMode.IMPLICIT -> {
                val retAddrLow = MEM[SPOF].toUInt()
                SP++
                val retAddrHigh = MEM[SPOF].toUInt()

                PC = (((retAddrHigh and 0xFFu)) shl 0x08 or (retAddrLow and 0xFFu)).toUShort()
            }
            ins.Mnemonic == "BNE" && ins.AddrMode == AddressMode.RELATIVE -> {
                if(!ZER) {
                    cycles++ //braucht 1 microop mehr wenn gebrancht wird
                    val branchAddrAbs = addressRelative()
                    if(branchAddrAbs and 0xFF00u != PC and 0xFF00u) {
                        // page muss gewechselt werden -> 1 microop mehr
                        cycles++
                    }

                    //braaaaaanchen
                    PC++
                    PC = branchAddrAbs
                    PC++ //pc pre - fetch auf nächste adresse schieben
                } else {
                    PC++
                }
            }
            ins.Mnemonic == "SBC" && ins.AddrMode == AddressMode.IMMEDIATE -> {
                val subValue = MEM[PC].toUShort() xor 0x00FFu
                PC++

                val carry = (if(CRY) 0x01u else 0x00u).toUByte()
                val tmp = (ACC + subValue + carry).toUShort()
                CRY = (tmp and 0xFF00u).toInt() != 0
                ZER = (tmp and 0x00FFu).toInt() == 0
                OVF = !((tmp xor ACC.toUShort()) and (tmp xor subValue) and 0x0080u).equals(0)
                NEG = (ACC and 0x80u).toInt() == 1

                ACC = (tmp and 0x00FFu).toUByte()
            }
            ins.Mnemonic == "ADC" && ins.AddrMode == AddressMode.ZEROPAGE -> {
                val addr = addressZeroPage()

                val addValue = MEM[addr].toUShort()

                val carry = (if(CRY) 0x01u else 0x00u).toUByte()
                val tmp = ACC.toUShort() + addValue + carry
                CRY = tmp > 0xFFu
                ZER = (tmp and 0x00FFu).equals(0)
                OVF = !(((ACC.toUShort() xor addValue) and (ACC.toUShort() xor tmp.toUShort()) and 0x0080u)).equals(0)

                NEG = (ACC and 0x80u).toInt() == 1

                ACC = (tmp and 0x00FFu).toUByte()
            }
            ins.Mnemonic == "JMP" && ins.AddrMode == AddressMode.INDIRECT -> {
                PC = addressIndirect()
            }
            ins.Mnemonic == "STX" && ins.AddrMode == AddressMode.ABSOLUTE -> {
                val targetAddr = addressAbsolute()
                MEM[targetAddr] = IDX
            }
            ins.Mnemonic == "STX" && ins.AddrMode == AddressMode.ZEROPAGE -> {
                val targetAddr = addressZeroPage()
                MEM[targetAddr] = IDX
            }
            ins.Mnemonic == "CLI" && ins.AddrMode == AddressMode.IMPLICIT -> {
                IRD = false
            }
            ins.Mnemonic == "SEC" && ins.AddrMode == AddressMode.IMPLICIT -> {
                CRY = true
            }
            ins.Mnemonic == "CLC" && ins.AddrMode == AddressMode.IMPLICIT -> {
                CRY = false
            }
            ins.Mnemonic == "STA" && ins.AddrMode == AddressMode.ZEROPAGE -> {
                val addr = addressZeroPage()
                MEM[addr] = ACC
            }
            ins.Mnemonic == "DEY" && ins.AddrMode == AddressMode.IMPLICIT -> {
                IDY--
                ZER = IDY == (0x00u).toUByte()
                NEG = (IDY and 0x80u).toInt() == 1
            }
        }
    }

    private fun addressAbsolute(): UShort {
        val jmpPointerLow = MEM[PC].toUInt()
        PC++
        val jmpPointerHigh = MEM[PC].toUInt()
        PC++

        return ((jmpPointerHigh and 0xFFu) shl 0x08 or (jmpPointerLow and 0xFFu)).toUShort()
    }

    private fun addressZeroPage(): UByte {
        val addrAbs = MEM[PC]
        PC++

        return addrAbs and 0xFFu
    }

    // kann nur bis -128 / +127 adressen springen
    private fun addressRelative(): UShort {
        var addrOffset = MEM[PC].toByte()
        //PC++

        // jump richtig berechnen, dh. pc um 1 adresse inc
        //PC++

        // sprung zurück
        /*if(!(addrOffset and 0x80u).equals(0)) {
            //addrOffset = addrOffset or 0xFFu
            //return PC.minus(addrOffset).toUShort()
        }*/
        return (PC.toShort() + addrOffset).toUShort()//.plus(addrOffset).toUShort()
    }

    private fun addressIndirect(): UShort {
        val jmpPointerLow = MEM[PC].toUInt()
        PC++
        val jmpPointerHigh = MEM[PC].toUInt()
        PC++

        var pointer = ((jmpPointerHigh and 0xFFu) shl 0x08 or (jmpPointerLow and 0xFFu))

        if(jmpPointerLow == 0x00FFu) {
            // 6502 hardware bug
            val low = MEM[(pointer and 0xFF00u).toUShort()].toUInt()
            var high = MEM[pointer.toUShort()].toUShort().toUInt()

            return ((high shl 0x08) or low).toUShort()
        } else {
            val low = MEM[(pointer and 0xFF00u).toUShort()].toUInt()
            var high = MEM[pointer.toUShort().plus(0x01u).toUShort()].toUInt()

            return ((high shl 0x08) or low).toUShort()
        }
    }

    object Instructions {
        private val instructions = listOf(
            Instruction6502("STA", 0x85u, AddressMode.ZEROPAGE, 3u),
            Instruction6502("LDA", 0xA9u, AddressMode.IMMEDIATE, 2u),
            Instruction6502("LDX", 0xA2u, AddressMode.IMMEDIATE, 2u),
            Instruction6502("LDX", 0xA6u, AddressMode.ZEROPAGE, 3u),
            Instruction6502("LDY", 0xA0u, AddressMode.IMMEDIATE, 2u),
            Instruction6502("TYA", 0x98u, AddressMode.IMPLICIT, 2u),
            Instruction6502("TAY", 0xA8u, AddressMode.IMPLICIT, 2u),
            Instruction6502("SEI", 0x78u, AddressMode.IMPLICIT, 2u),
            Instruction6502("TXS", 0x9Au, AddressMode.IMPLICIT, 2u),
            Instruction6502("CLD", 0xD8u, AddressMode.IMPLICIT, 2u),
            Instruction6502("JSR", 0x20u, AddressMode.ABSOLUTE, 6u),
            Instruction6502("BNE", 0xD0u, AddressMode.RELATIVE, 2u),
            Instruction6502("JMP", 0x6Cu, AddressMode.INDIRECT, 5u),
            Instruction6502("STX", 0x8Eu, AddressMode.ABSOLUTE, 4u),
            Instruction6502("STX", 0x86u, AddressMode.ZEROPAGE, 3u),
            Instruction6502("CLI", 0x58u, AddressMode.IMPLICIT, 2u),
            Instruction6502("RTS", 0x60u, AddressMode.IMPLICIT, 6u),
            Instruction6502("SEC", 0x38u, AddressMode.IMPLICIT, 2u),
            Instruction6502("CLC", 0x18u, AddressMode.IMPLICIT, 2u),
            Instruction6502("SBC", 0xE9u, AddressMode.IMMEDIATE, 2u),
            Instruction6502("ADC", 0x65u, AddressMode.ZEROPAGE, 3u),
            Instruction6502("DEY", 0x88u, AddressMode.IMPLICIT, 3u)
        )


        operator fun get(mnemonic: String, addrMode: AddressMode) = instructions.firstOrNull { i ->
            i.Mnemonic == mnemonic && i.AddrMode == addrMode
        }

        operator fun get(opcode: UByte) = instructions.firstOrNull { i ->
            i.Opcode == opcode
        }
    }

    fun __debugInternalCpuState(instruction: String) {
        println("=============== 6502 state ===============")
        println("INS = $instruction")
        println("PC = ${PC.toString(16)}, $PC")
        println("SP = ${SP.toString(16)}, $SP")
        println("ACC = ${ACC.toString(16)}, $ACC")
        println("IDX = ${IDX.toString(16)}, $IDX")
        println("IDY = ${IDY.toString(16)}, $IDY")
        println("CRY = $CRY")
        println("ZER = $ZER")
        println("IRD = $IRD")
        println("DEC = $DEC")
        println("BRK = $BRK")
        println("OVF = $OVF")
        println("NEG = $NEG")
        println("==========================================")

    }
}