LDX #$01
STX $00

SEC; clean carry
LDY #$07 ;target fibonacci
TYA
SBC #$03; handles the algorithm iteration counting
TAY

CLC; clean carry
LDA #$02
STA $01

loop: LDX $01
      ADC $00
      STA $01
      STX $00
      DEY
      BNE loop