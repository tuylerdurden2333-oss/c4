import android.content.Context
import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

class ChessEngine(private val context: Context) {
    private var stockfishProcess: Process? = null
    private var stockfishInput: DataOutputStream? = null
    private var stockfishOutput: BufferedReader? = null
    private val board = Board()
    private var playerSide: Piece.Color = Piece.Color.WHITE

    fun setPlayerSide(side: Piece.Color) {
        playerSide = side
        if (side == Piece.Color.BLACK) {
            // If player is black, let AI make first move
            makeAIMove()
        }
    }

    fun initEngine() {
        try {
            // Assuming Stockfish binary is in assets/stockfish/ folder
            val stockfishFile = File(context.filesDir, "stockfish")
            if (!stockfishFile.exists()) {
                context.assets.open("stockfish/stockfish").copyTo(stockfishFile.outputStream())
                stockfishFile.setExecutable(true)
            }

            stockfishProcess = ProcessBuilder(stockfishFile.absolutePath).start()
            stockfishInput = DataOutputStream(stockfishProcess!!.outputStream)
            stockfishOutput = BufferedReader(InputStreamReader(stockfishProcess!!.inputStream))

            // Configure Stockfish for maximum difficulty
            sendCommand("uci")
            sendCommand("setoption name Skill Level value 20")
            sendCommand("setoption name Contempt value 0")
            sendCommand("setoption name Min Split Depth value 0")
            sendCommand("setoption name Threads value ${Runtime.getRuntime().availableProcessors()}")
            sendCommand("setoption name Hash value 2048") // Max hash for mobile devices
            sendCommand("setoption name Ponder value false")
            sendCommand("setoption name MultiPV value 1")
            sendCommand("setoption name UCI_Chess960 value false")
            sendCommand("ucinewgame")
            sendCommand("isready")

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendCommand(command: String) {
        try {
            stockfishInput?.writeBytes("$command\n")
            stockfishInput?.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getValidMoves(fromSquare: Square): List<Square> {
        return board.legalMoves()
            .filter { it.from == fromSquare }
            .map { it.to }
    }

    fun makeMove(from: Square, to: Square): Boolean {
        val move = Move(from, to)
        if (board.isMoveLegal(move, false)) {
            board.doMove(move)
            return true
        }
        return false
    }

    fun makeAIMove(): Move? {
        if (board.isMated || board.isDraw) return null

        sendCommand("position fen ${board.fen()}")
        sendCommand("go movetime 5000") // 5 seconds for maximum strength

        var bestMove: String? = null
        var line: String?
        do {
            line = stockfishOutput?.readLine()
            if (line != null && line.startsWith("bestmove")) {
                bestMove = line.split(" ")[1]
            }
        } while (line != null && bestMove == null)

        bestMove?.let {
            val from = Square.fromValue(it.substring(0, 2).toUpperCase())
            val to = Square.fromValue(it.substring(2, 4).toUpperCase())
            val move = Move(from, to)
            if (board.isMoveLegal(move, false)) {
                board.doMove(move)
                return move
            }
        }
        return null
    }

    fun getBoardState(): GameState {
        // Convert chesslib board to our GameState representation
        // Implementation details would depend on your board representation
        // This is a simplified version
        val pieces = Array<Piece?>(64) { null }
        
        for (square in Square.values()) {
            val piece = board.getPiece(square)
            if (piece != com.github.bhlangonijr.chesslib.Piece.NONE) {
                val type = when (piece.pieceType) {
                    com.github.bhlangonijr.chesslib.PieceType.PAWN -> Piece.Type.PAWN
                    com.github.bhlangonijr.chesslib.PieceType.KNIGHT -> Piece.Type.KNIGHT
                    com.github.bhlangonijr.chesslib.PieceType.BISHOP -> Piece.Type.BISHOP
                    com.github.bhlangonijr.chesslib.PieceType.ROOK -> Piece.Type.ROOK
                    com.github.bhlangonijr.chesslib.PieceType.QUEEN -> Piece.Type.QUEEN
                    com.github.bhlangonijr.chesslib.PieceType.KING -> Piece.Type.KING
                    else -> null
                }
                val color = if (piece.pieceSide == com.github.bhlangonijr.chesslib.Side.WHITE) 
                    Piece.Color.WHITE else Piece.Color.BLACK
                
                if (type != null) {
                    pieces[square.ordinal] = Piece(type, color, square.ordinal)
                }
            }
        }
        
        return GameState(
            board = pieces,
            currentPlayer = if (board.sideToMove == com.github.bhlangonijr.chesslib.Side.WHITE) 
                Piece.Color.WHITE else Piece.Color.BLACK,
            isCheck = board.isKingAttacked,
            isCheckmate = board.isMated,
            isStalemate = board.isStalemate
        )
    }

    fun cleanup() {
        try {
            sendCommand("quit")
            stockfishInput?.close()
            stockfishOutput?.close()
            stockfishProcess?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
