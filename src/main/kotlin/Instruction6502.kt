data class Instruction6502(
    val Mnemonic: String,
    val Opcode: UByte,
    val AddrMode: AddressMode,
    val Cycles: UByte
)

enum class AddressMode {
    IMPLICIT,
    ACCUMULATOR,
    IMMEDIATE,
    ZEROPAGE,
    ZEROPAGE_X,
    ZEROPAGE_Y,
    RELATIVE,
    ABSOLUTE,
    ABSOLUTE_X,
    ABSOLUTE_Y,
    INDIRECT,
    IDX_INDIRECT,
    IDR_INDEXED
}