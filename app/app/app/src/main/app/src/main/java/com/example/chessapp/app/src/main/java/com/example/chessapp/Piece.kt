data class Piece(
    val type: Type,
    val color: Color,
    val position: Int // 0-63 board index
) {
    enum class Type { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }
    enum class Color { WHITE, BLACK }
}
