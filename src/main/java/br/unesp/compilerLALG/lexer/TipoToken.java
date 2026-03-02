package br.unesp.compilerLALG.lexer;

public enum TipoToken {
    OPSOMA,  // +
    OPSUB,   // -
    OPMUL,   // *
    OPDIV,   // /
    AP,      // (
    FP,      // )
    NUM,     // Números naturais ou reais (ex: 2, 3.2, 12.01)
    EOF      // Marcador de fim de cadeia (End Of File)
}
