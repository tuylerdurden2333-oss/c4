import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var squareSize = 0f
    private val lightSquareColor = Color.parseColor("#F0D9B5")
    private val darkSquareColor = Color.parseColor("#B58863")
    private val highlightColor = Color.parseColor("#64FF0000") // Semi-transparent red
    private val moveIndicatorColor = Color.parseColor("#6400FF00") // Semi-transparent green
    
    private var gameState: GameState? = null
    private var pieceBitmaps: MutableMap<String, Bitmap> = mutableMapOf()
    
    var onPieceSelected: ((Int) -> Unit)? = null
    var onMoveRequested: ((Int, Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        squareSize = (minOf(width, height) / 8f)
        loadPieceBitmaps()
    }

    private fun loadPieceBitmaps() {
        val pieces = listOf("wp", "wn", "wb", "wr", "wq", "wk", "bp", "bn", "bb", "br", "bq", "bk")
        pieces.forEach { piece ->
            val resId = resources.getIdentifier(piece, "drawable", context.packageName)
            val bitmap = BitmapFactory.decodeResource(resources, resId)
            pieceBitmaps[piece] = Bitmap.createScaledBitmap(bitmap, squareSize.toInt(), squareSize.toInt(), true)
        }
    }

    fun updateGameState(newState: GameState) {
        gameState = newState
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBoard(canvas)
        drawHighlights(canvas)
        drawPieces(canvas)
    }

    private fun drawBoard(canvas: Canvas) {
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val paint = Paint().apply {
                    color = if ((row + col) % 2 == 0) lightSquareColor else darkSquareColor
                }
                val left = col * squareSize
                val top = row * squareSize
                canvas.drawRect(left, top, left + squareSize, top + squareSize, paint)
            }
        }
    }

    private fun drawHighlights(canvas: Canvas) {
        gameState?.let { state ->
            // Draw selected square highlight
            state.selectedPosition?.let { position ->
                val row = position / 8
                val col = position % 8
                val paint = Paint().apply { color = highlightColor }
                canvas.drawRect(col * squareSize, row * squareSize, 
                               (col + 1) * squareSize, (row + 1) * squareSize, paint)
            }
            
            // Draw valid move indicators
            val paint = Paint().apply { color = moveIndicatorColor }
            state.validMoves.forEach { position ->
                val row = position / 8
                val col = position % 8
                val centerX = col * squareSize + squareSize / 2
                val centerY = row * squareSize + squareSize / 2
                val radius = squareSize / 4
                canvas.drawCircle(centerX, centerY, radius, paint)
            }
        }
    }

    private fun drawPieces(canvas: Canvas) {
        gameState?.board?.forEachIndexed { index, piece ->
            piece?.let {
                val row = index / 8
                val col = index % 8
                val pieceCode = when (it.type) {
                    Piece.Type.PAWN -> "p"
                    Piece.Type.KNIGHT -> "n"
                    Piece.Type.BISHOP -> "b"
                    Piece.Type.ROOK -> "r"
                    Piece.Type.QUEEN -> "q"
                    Piece.Type.KING -> "k"
                }
                val colorPrefix = if (it.color == Piece.Color.WHITE) "w" else "b"
                val bitmap = pieceBitmaps["$colorPrefix$pieceCode"]
                bitmap?.let {
                    canvas.drawBitmap(it, col * squareSize, row * squareSize, null)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val col = (event.x / squareSize).toInt()
            val row = (event.y / squareSize).toInt()
            val position = row * 8 + col
            
            gameState?.let { state ->
                if (state.currentPlayer == state.board[position]?.color) {
                    // Select piece if it's the player's turn and their piece
                    onPieceSelected?.invoke(position)
                } else if (state.selectedPosition != null && state.validMoves.contains(position)) {
                    // Attempt to move to a valid destination
                    onMoveRequested?.invoke(state.selectedPosition!!, position)
                }
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
