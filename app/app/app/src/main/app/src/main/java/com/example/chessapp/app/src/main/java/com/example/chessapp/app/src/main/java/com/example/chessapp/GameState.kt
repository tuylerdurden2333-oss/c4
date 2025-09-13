data class GameState(
    val board: Array<Piece?>,
    val currentPlayer: Piece.Color,
    val selectedPosition: Int? = null,
    val validMoves: List<Int> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!board.contentEquals(other.board)) return false
        if (currentPlayer != other.currentPlayer) return false
        if (selectedPosition != other.selectedPosition) return false
        if (validMoves != other.validMoves) return false
        if (isCheck != other.isCheck) return false
        if (isCheckmate != other.isCheckmate) return false
        if (isStalemate != other.isStalemate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + currentPlayer.hashCode()
        result = 31 * result + (selectedPosition ?: 0)
        result = 31 * result + validMoves.hashCode()
        result = 31 * result + isCheck.hashCode()
        result = 31 * result + isCheckmate.hashCode()
        result = 31 * result + isStalemate.hashCode()
        return result
    }
}
