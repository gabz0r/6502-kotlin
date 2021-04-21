data class Memory6502(
    var content: UByteArray = UByteArray(0x10000) // 1024 * 64
) {
    init {
        initFibonacci()
    }

    fun initFibonacci() {
        // startvektor aus fffc auf 0600
        content[0xFFFC] = 0x00u
        content[0xFFFD] = 0x06u

        content[0x0600] = 0xA2u
        content[0x0601] = 0x01u
        content[0x0602] = 0x86u
        content[0x0603] = 0x00u
        content[0x0604] = 0x38u
        content[0x0605] = 0xA0u
        content[0x0606] = 0x0Du
        content[0x0607] = 0x98u
        content[0x0608] = 0xE9u
        content[0x0609] = 0x03u
        content[0x060A] = 0xA8u
        content[0x060B] = 0x18u
        content[0x060C] = 0xA9u
        content[0x060D] = 0x02u
        content[0x060E] = 0x85u
        content[0x060F] = 0x01u
        content[0x0610] = 0xA6u
        content[0x0611] = 0x01u
        content[0x0612] = 0x65u
        content[0x0613] = 0x00u
        content[0x0614] = 0x85u
        content[0x0615] = 0x01u
        content[0x0616] = 0x86u
        content[0x0617] = 0x00u
        content[0x0618] = 0x88u
        content[0x0619] = 0xD0u
        content[0x061A] = 0xF5u

        // ergebnis aus akkumulator in zp$0003 speichern
        content[0x061B] = 0x85u
        content[0x061C] = 0x02u

        // des lulzes wegen
        content[0x061D] = 0xA2u
        content[0x061E] = 0x13u

        content[0x061F] = 0x86u
        content[0x0620] = 0x00u

        content[0x0621] = 0xA2u
        content[0x0622] = 0x37u

        content[0x0623] = 0x86u
        content[0x0624] = 0x01u
    }

    fun initKernalC64() {
        // initialize NMI at FFFA to FE43
        content[0xFFFA] = 0x43u
        content[0xFFFB] = 0xFEu

        // initialize IV at FFFC to FCE2
        content[0xFFFC] = 0xE2u
        content[0xFFFD] = 0xFCu

        // initialize ISR at FFFE to FE43
        content[0xFFFE] = 0x48u
        content[0xFFFF] = 0xFFu

        // kernal rom rst routine
        content[0xFCE2] = Cpu6502.Instructions["LDX", AddressMode.IMMEDIATE]!!.Opcode
        content[0xFCE3] = 0xFFu
        content[0xFCE4] = Cpu6502.Instructions["SEI", AddressMode.IMPLICIT]!!.Opcode
        content[0xFCE5] = Cpu6502.Instructions["TXS", AddressMode.IMPLICIT]!!.Opcode
        content[0xFCE6] = Cpu6502.Instructions["CLD", AddressMode.IMPLICIT]!!.Opcode
        content[0xFCE7] = Cpu6502.Instructions["JSR", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCE8] = 0x02u
        content[0xFCE9] = 0xFDu
        // check auf card bei FD02, dummy macht jetzt erstmal nur RTS (falls karte vorhanden zero flag = 1)
        content[0xFD02] = Cpu6502.Instructions["RTS", AddressMode.IMPLICIT]!!.Opcode
        //end video init
        content[0xFCEA] = Cpu6502.Instructions["BNE", AddressMode.IMMEDIATE]!!.Opcode
        content[0xFCEB] = 0x03u
        content[0xFCEC] = Cpu6502.Instructions["JMP", AddressMode.INDIRECT]!!.Opcode
        content[0xFCED] = 0x00u
        content[0xFCEE] = 0x80u
        content[0xFCEF] = Cpu6502.Instructions["STX", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCF0] = 0x16u
        content[0xFCF1] = 0xD0u
        content[0xFCF2] = Cpu6502.Instructions["JSR", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCF3] = 0xA3u
        content[0xFCF4] = 0xFDu
        // io controller bei FDA3 initialisieren, dummy macht jetzt erstmal nur RTS (falls karte vorhanden zero flag = 1)
        content[0xFDA3] = Cpu6502.Instructions["RTS", AddressMode.IMPLICIT]!!.Opcode
        //end video init
        content[0xFCF5] = Cpu6502.Instructions["JSR", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCF6] = 0x50u
        content[0xFCF7] = 0xFDu
        // mem bei FD50 initialisieren, dummy macht jetzt erstmal nur RTS
        content[0xFD50] = Cpu6502.Instructions["RTS", AddressMode.IMPLICIT]!!.Opcode
        //end video init
        content[0xFCF8] = Cpu6502.Instructions["JSR", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCF9] = 0x15u
        content[0xFCFA] = 0xFDu
        // io vektoren bei FD15 initialisieren, dummy macht jetzt erstmal nur RTS
        content[0xFD15] = Cpu6502.Instructions["RTS", AddressMode.IMPLICIT]!!.Opcode
        //end video init
        content[0xFCFB] = Cpu6502.Instructions["JSR", AddressMode.ABSOLUTE]!!.Opcode
        content[0xFCFC] = 0x5Bu
        content[0xFCFD] = 0xFFu
        // c64 irq bei FF5B initialisieren, dummy macht jetzt erstmal nur RTS
        content[0xFF5B] = Cpu6502.Instructions["RTS", AddressMode.IMPLICIT]!!.Opcode
        //end video init
        content[0xFCFE] = Cpu6502.Instructions["CLI", AddressMode.IMPLICIT]!!.Opcode
        content[0xFCFF] = Cpu6502.Instructions["JMP", AddressMode.INDIRECT]!!.Opcode
        content[0xFD00] = 0x00u
        content[0xFD01] = 0xA0u
        // bei adresse A000 sitzt der zeiger auf basic im RAM, würde durch indirect JMP dann normalerweise booten
    }

    fun reset() {
        for (i in content.indices)
            content[i] = 0x00u
    }

    fun dump(range: IntRange) {
        println("Memory dump ${range.first.toString(16)} - ${range.last.toString(16)}")
        for(i in range) {
            if(i == 0 || i.rem(16) == 0 || range.last - range.first < 16) {

                var address = i.toString(16)
                repeat((1 .. (4 - address.length)).count()) {
                    address = "0${address}"
                }

                print("\n$address: ")
            }
            var cnt = content[i].toString(16)

            repeat((1 .. (2 - cnt.length)).count()) {
                cnt = "0${cnt}"
            }

            print("$cnt ")
        }

        println()
    }

    companion object MemoryMap {
        val ZEROPAGE = IntRange(0x0000, 0x00FF)
        val ZEROPAGE_STACK = IntRange(0x0100, 0x01FF)
        val OS_BASIC_PTR_P2 = IntRange(0x0200, 0x02FF)
        val OS_BASIC_PTR_P3 = IntRange(0x0300, 0x03FF)
        val SCREEN = IntRange(0x0400, 0x07FF)
        val BASIC_PRG = IntRange(0x0800, 0x9FFF)
        val MLANG_P1 = IntRange(0xA000, 0xBFFF)
        val MLANG_P2 = IntRange(0xC000, 0xCFFF)

        val IV = IntRange(0xFFFA, 0xFFFF)
    }

    fun load(data: UByteArray, address: UShort) {
        //content = data.copyOf(0x10000)
        reset()
        data.forEachIndexed { index, uByte ->
            val targetMemoryAddress = index.toUInt() + address
            content[targetMemoryAddress.toInt()] = uByte
        }
    }

    fun setInitializationVector(iv: UShort) {
        content[0xFFFC] = (iv.toUInt() and 0x00FFu).toUByte()
        content[0xFFFD] = ((iv.toUInt() and 0xFF00u) shr 0x08).toUByte()
    }

    operator fun get(idx: Int) = content[idx]
    operator fun set(idx: Int, value: UByte) {
        content[idx] = value
    }

    operator fun get(idx: UByte) = content[idx.toInt()]
    operator fun set(idx: UByte, value: UByte) {
        content[idx.toInt()] = value
    }

    operator fun get(idx: UShort) = content[idx.toInt()]
    operator fun set(idx: UShort, value: UByte) {
        content[idx.toInt()] = value
    }
}